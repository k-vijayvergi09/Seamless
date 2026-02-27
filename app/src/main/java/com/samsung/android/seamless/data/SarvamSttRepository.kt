package com.samsung.android.seamless.data

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresPermission
import com.samsung.android.seamless.BuildConfig
import com.samsung.android.seamless.domain.SttRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SarvamSttRepository : SttRepository {

    companion object {
        private const val TAG = "SarvamSttRepository"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_SIZE = 4096 // 128ms of audio at 16kHz/16-bit/mono

        private const val WS_URL = "wss://api.sarvam.ai/speech-to-text/ws" +
                "?language-code=en-IN" +
                "&mode=translit" +
                "&sample_rate=$SAMPLE_RATE" +
                "&vad_signals=true" +
                "&high_vad_sensitivity=true" +
                "&input_audio_codec=pcm_s16le"
    }

    private var audioRecord: AudioRecord? = null
    private var webSocket: WebSocket? = null
    private var captureJob: Job? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Required for persistent WebSocket
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    // extraBufferCapacity: buffers up to 64 messages so tryEmit() never drops an API response
    // even if the collector coroutine hasn't started yet or is briefly busy.
    private val _transcripts = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val transcripts = _transcripts.asSharedFlow()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startStreaming() {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize * 2, CHUNK_SIZE * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize")
            audioRecord?.release()
            audioRecord = null
            _transcripts.tryEmit("""{"type":"error","data":{"error":"Microphone unavailable"}}""")
            return
        }

        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Api-Subscription-Key", BuildConfig.SARVAM_API_KEY)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected")
                startAudioCapture()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.i(TAG, "Received: $text")
                _transcripts.tryEmit(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed: $code $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                val errorJson = JSONObject().apply {
                    put("type", "error")
                    put("data", JSONObject().apply {
                        put("error", t.localizedMessage ?: "Connection failed")
                    })
                }
                _transcripts.tryEmit(errorJson.toString())
            }
        })
    }

    private fun startAudioCapture() {
        audioRecord?.startRecording()

        captureJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(CHUNK_SIZE)
            Log.i(TAG, "Audio capture started")

            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val bytesRead = audioRecord?.read(buffer, 0, CHUNK_SIZE) ?: -1
                if (bytesRead > 0) {
                    sendAudioChunk(buffer.copyOf(bytesRead))
                } else if (bytesRead < 0) {
                    Log.e(TAG, "AudioRecord.read error: $bytesRead")
                    break
                }
            }
            Log.i(TAG, "Audio capture loop ended")
        }
    }

    private fun sendAudioChunk(pcmData: ByteArray) {
        val base64Audio = Base64.encodeToString(pcmData, Base64.NO_WRAP)
        val json = JSONObject().apply {
            put("audio", JSONObject().apply {
                put("data", base64Audio)
                put("encoding", "audio/wav")
                put("sample_rate", SAMPLE_RATE)
            })
        }
        webSocket?.send(json.toString())
    }

    private fun flush() {
        val json = JSONObject().apply { put("type", "flush") }
        webSocket?.send(json.toString())
    }

    override fun stopStreaming() {
        captureJob?.cancel()
        captureJob = null

        flush()

        audioRecord?.let { record ->
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
            record.release()
        }
        audioRecord = null

        webSocket?.close(1000, "Client disconnect")
        webSocket = null

        Log.i(TAG, "Streaming stopped")
    }
}
