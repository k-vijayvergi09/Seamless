package com.samsung.android.seamless.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.samsung.android.seamless.MainActivity
import com.samsung.android.seamless.R
import com.samsung.android.seamless.data.SarvamSttRepository
import com.samsung.android.seamless.overlay.OverlayStateStore
import com.samsung.android.seamless.widget.CopyTranscriptActivity
import com.samsung.android.seamless.widget.RecognitionState
import com.samsung.android.seamless.widget.ToggleRecognitionActivity
import com.samsung.android.seamless.widget.WidgetStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

class SpeechRecognitionService : Service() {

    companion object {
        private const val TAG = "SpeechRecognitionSvc"
        private const val CHANNEL_ID = "seamless_speech_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.samsung.android.seamless.ACTION_START"

        /** True while the service is actively recording. Checked by ToggleRecognitionAction. */
        @Volatile
        var isRunning: Boolean = false
            private set

        fun startRecognition(context: Context) {
            context.startForegroundService(
                Intent(context, SpeechRecognitionService::class.java).apply {
                    action = ACTION_START
                }
            )
        }

        /**
         * Stops the service via stopService() so onDestroy() handles all cleanup reliably,
         * avoiding the race where serviceScope.cancel() kills the IDLE-state coroutine first.
         */
        fun stopRecognition(context: Context) {
            context.stopService(Intent(context, SpeechRecognitionService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var stateManager: WidgetStateManager
    private val repository = SarvamSttRepository()
    private val transcriptAccumulator = TranscriptAccumulator()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        stateManager = WidgetStateManager(applicationContext)
        createNotificationChannel()
        publishOverlayState(
            recognitionState = stateManager.recognitionState,
            transcript = stateManager.transcriptText,
            error = stateManager.errorMessage
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_START -> startListening()
            else -> {
                Log.w(TAG, "Unknown action — stopping self")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * All cleanup lives here so it runs whether the service is stopped by the user tapping
     * the widget, by an API error, or by the system.
     */
    override fun onDestroy() {
        Log.i(TAG, "onDestroy called")
        isRunning = false
        repository.stopStreaming()

        // Update widget state using Glance's internal state management.
        // runBlocking ensures completion before service is destroyed.
        kotlinx.coroutines.runBlocking {
            stateManager.setIdleStateAndRefresh()
        }
        publishOverlayState(
            recognitionState = RecognitionState.IDLE,
            transcript = stateManager.transcriptText,
            error = ""
        )

        serviceScope.cancel()
        super.onDestroy()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun startListening() {
        if (isRunning) {
            Log.w(TAG, "startListening called while already running — ignoring")
            return
        }

        Log.i(TAG, "startListening — calling startForeground")
        isRunning = true
        transcriptAccumulator.reset(stateManager.transcriptText)
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        repository.startStreaming()

        serviceScope.launch {
            stateManager.updateStateAndRefreshWidget(state = RecognitionState.LISTENING)
        }
        publishOverlayState(
            recognitionState = RecognitionState.LISTENING,
            transcript = transcriptAccumulator.combined(),
            error = ""
        )

        serviceScope.launch {
            repository.transcripts.collect { raw ->
                Log.i(TAG, "transcript received: $raw")
                handleMessage(raw)
            }
        }
    }

    private fun handleMessage(raw: String) {
        try {
            val json = JSONObject(raw)
            val data = json.optJSONObject("data")

            when (json.optString("type")) {
                "data" -> {
                    val transcript = data?.optString("transcript", "") ?: ""
                    if (transcript.isNotBlank()) {
                        val updated = transcriptAccumulator.onData(transcript)
                        serviceScope.launch {
                            stateManager.updateStateAndRefreshWidget(
                                state = RecognitionState.SPEECH_ACTIVE,
                                transcript = updated
                            )
                        }
                        publishOverlayState(
                            recognitionState = RecognitionState.SPEECH_ACTIVE,
                            transcript = updated,
                            error = ""
                        )
                    }
                }
                "events" -> {
                    // Only handle END_SPEECH to return to LISTENING state.
                    // START_SPEECH is redundant since "data" messages already set SPEECH_ACTIVE.
                    val signal = data?.optString("signal_type", "") ?: ""
                    if (signal == "END_SPEECH") {
                        val transcript = transcriptAccumulator.onEndSpeech()
                        serviceScope.launch {
                            stateManager.updateStateAndRefreshWidget(
                                state = RecognitionState.LISTENING,
                                transcript = transcript
                            )
                        }
                        publishOverlayState(
                            recognitionState = RecognitionState.LISTENING,
                            transcript = transcript,
                            error = ""
                        )
                    }
                }
                "error" -> {
                    val error = data?.optString("message")
                        ?: data?.optString("error")
                        ?: "Unknown error"
                    Log.e(TAG, "API error: $error")
                    val userFriendlyError = mapErrorToUserMessage(error)
                    serviceScope.launch {
                        stateManager.updateStateAndRefreshWidget(
                            state = RecognitionState.ERROR,
                            error = userFriendlyError
                        )
                    }
                    publishOverlayState(
                        recognitionState = RecognitionState.ERROR,
                        transcript = transcriptAccumulator.combined(),
                        error = userFriendlyError
                    )
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $raw", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Speech Recognition",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active while Seamless is listening"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, ToggleRecognitionActivity::class.java).apply {
                putExtra(ToggleRecognitionActivity.EXTRA_FORCE_STOP, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val copyIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, CopyTranscriptActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Seamless")
            .setContentText("Listening... speak to transcribe")
            .setSmallIcon(R.drawable.ic_widget_mic)
            .setContentIntent(openAppIntent)
            .addAction(0, "Stop", stopIntent)
            .addAction(0, "Copy", copyIntent)
            .setOngoing(true)
            .build()
    }

    private fun mapErrorToUserMessage(error: String): String {
        val lower = error.lowercase()
        return when {
            "microphone" in lower || "audio" in lower -> "Microphone issue. Check permissions and retry."
            "timeout" in lower || "connection" in lower || "network" in lower ||
                    "host" in lower || "socket" in lower -> "Network issue. Check connection and retry."
            else -> "Something went wrong. Please retry."
        }
    }

    private fun publishOverlayState(
        recognitionState: RecognitionState,
        transcript: String,
        error: String
    ) {
        OverlayStateStore.setRecognitionState(
            recognitionState = recognitionState,
            committedTranscript = transcript,
            partialTranscript = "",
            errorMessage = error
        )
    }
}
