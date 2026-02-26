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
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.samsung.android.seamless.MainActivity
import com.samsung.android.seamless.R
import com.samsung.android.seamless.data.SarvamSttRepository
import com.samsung.android.seamless.widget.RecognitionState
import com.samsung.android.seamless.widget.SeamlessWidget
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
        Log.d(TAG, "onCreate")
        stateManager = WidgetStateManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")
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
        isRunning = false
        repository.stopStreaming()

        // Update SharedPreferences synchronously so the next widget tap reads IDLE immediately.
        stateManager.recognitionState = RecognitionState.IDLE

        // Refresh the widget in a NEW scope — serviceScope is cancelled right after, so any
        // coroutine inside it would never run.
        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            try {
                val manager = GlanceAppWidgetManager(applicationContext)
                for (id in manager.getGlanceIds(SeamlessWidget::class.java)) {
                    SeamlessWidget().update(applicationContext, id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Widget refresh failed on destroy", e)
            }
        }

        serviceScope.cancel()
        super.onDestroy()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun startListening() {
        Log.d(TAG, "startListening — calling startForeground")
        isRunning = true
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        Log.d(TAG, "startForeground done — starting stream")
        repository.startStreaming()
        Log.d(TAG, "startStreaming called — launching coroutines")

        serviceScope.launch {
            stateManager.updateStateAndRefreshWidget(RecognitionState.LISTENING)
        }

        serviceScope.launch {
            repository.transcripts.collect { raw ->
                Log.d(TAG, "transcript received: $raw")
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
                                transcript = updated
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
                        stateManager.updateStateAndRefreshWidget(nextState)
                    }
                }
                "error" -> {
                    val error = data?.optString("error", "Unknown error") ?: "Unknown error"
                    Log.e(TAG, "API error: $error")
                    serviceScope.launch {
                        stateManager.updateStateAndRefreshWidget(
                            state = RecognitionState.ERROR,
                            error = error
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
