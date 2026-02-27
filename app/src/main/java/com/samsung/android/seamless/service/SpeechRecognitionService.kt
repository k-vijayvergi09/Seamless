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
import com.samsung.android.seamless.widget.RecognitionState
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

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        stateManager = WidgetStateManager(applicationContext)
        createNotificationChannel()
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
            stateManager.setIdleStateAndRefresh(this@SpeechRecognitionService)
        }

        serviceScope.cancel()
        super.onDestroy()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun startListening() {
        Log.i(TAG, "startListening — calling startForeground")
        isRunning = true
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        Log.i(TAG, "startForeground done — starting stream")
        repository.startStreaming()
        Log.i(TAG, "startStreaming called — launching coroutines")

        serviceScope.launch {
            stateManager.updateStateAndRefreshWidget(
                state = RecognitionState.LISTENING,
                callerContext = this@SpeechRecognitionService
            )
        }

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
                        val updated = buildTranscript(stateManager.transcriptText, transcript)
                        serviceScope.launch {
                            stateManager.updateStateAndRefreshWidget(
                                state = RecognitionState.SPEECH_ACTIVE,
                                transcript = updated,
                                callerContext = this@SpeechRecognitionService
                            )
                        }
                    }
                }
                "events" -> {
                    val signal = data?.optString("signal_type", "") ?: ""
                    val nextState = when (signal) {
                        "START_SPEECH" -> RecognitionState.SPEECH_ACTIVE
                        "END_SPEECH" -> RecognitionState.LISTENING
                        else -> return
                    }
                    serviceScope.launch {
                        stateManager.updateStateAndRefreshWidget(
                            state = nextState,
                            callerContext = this@SpeechRecognitionService
                        )
                    }
                }
                "error" -> {
                    // API returns error in "message" field, fallback to "error" field
                    val error = data?.optString("message")
                        ?: data?.optString("error")
                        ?: "Unknown error"
                    Log.e(TAG, "API error: $error")
                    serviceScope.launch {
                        stateManager.updateStateAndRefreshWidget(
                            state = RecognitionState.ERROR,
                            error = error,
                            callerContext = this@SpeechRecognitionService
                        )
                    }
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $raw", e)
        }
    }

    private fun buildTranscript(existing: String, newChunk: String): String =
        if (existing.isBlank()) newChunk else "$existing $newChunk"

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
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Seamless")
            .setContentText("Listening for speech…")
            .setSmallIcon(R.drawable.ic_widget_mic)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()
    }
}
