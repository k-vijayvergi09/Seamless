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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val BASE_RECONNECT_DELAY_MS = 1_000L
        private const val MAX_RECONNECT_DELAY_MS = 15_000L

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
    private var reconnectJob: Job? = null
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var shouldReconnect = false
    private var reconnectAttempt = 0

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
        if (webSocket != null) {
            Log.w(TAG, "startStreaming called with an existing websocket; ignoring")
            return
        }
        shouldReconnect = true
        reconnectAttempt = 0
        reconnectJob?.cancel()
        connectWebSocket()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun connectWebSocket() {
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
            cleanupAudioRecord()
            emitError("Microphone unavailable")
            return
        }

        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Api-Subscription-Key", BuildConfig.SARVAM_API_KEY)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected")
                reconnectAttempt = 0
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
                this@SarvamSttRepository.webSocket = null
                if (shouldReconnect) {
                    scheduleReconnect("Socket closed: $code $reason")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                this@SarvamSttRepository.webSocket = null
                cleanupAudioRecord()
                if (shouldReconnect && reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
                    scheduleReconnect(t.localizedMessage ?: "Connection failed")
                    return
                }
                emitError(t.localizedMessage ?: "Connection failed")
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
                    emitError("Audio capture failed ($bytesRead)")
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
        shouldReconnect = false
        reconnectAttempt = 0
        reconnectJob?.cancel()
        reconnectJob = null
        captureJob?.cancel()
        captureJob = null

        flush()
        cleanupAudioRecord()

        webSocket?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null

        Log.i(TAG, "Streaming stopped")
    }

    private fun scheduleReconnect(reason: String) {
        if (!shouldReconnect) return
        if (reconnectJob?.isActive == true) return

        reconnectAttempt += 1
        val delayMs = minOf(
            MAX_RECONNECT_DELAY_MS,
            BASE_RECONNECT_DELAY_MS * (1L shl (reconnectAttempt - 1))
        )
        Log.w(TAG, "Scheduling reconnect attempt=$reconnectAttempt in ${delayMs}ms; reason=$reason")

        reconnectJob = repositoryScope.launch {
            delay(delayMs)
            if (!shouldReconnect) return@launch
            connectWebSocket()
        }
    }

    private fun cleanupAudioRecord() {
        audioRecord?.let { record ->
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
            record.release()
        }
        audioRecord = null
    }

    private fun emitError(message: String) {
        val errorJson = JSONObject().apply {
            put("type", "error")
            put("data", JSONObject().apply {
                put("error", message)
            })
        }
        _transcripts.tryEmit(errorJson.toString())
    }
}
