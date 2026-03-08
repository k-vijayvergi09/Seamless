package com.samsung.android.seamless.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.samsung.android.seamless.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SeamlessOverlayService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var overlayManager: OverlayManager

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        createNotificationChannel()
        overlayManager = OverlayManager(this)
        serviceScope.launch {
            OverlayStateStore.state.collect { state ->
                Log.i(TAG, "render overlayVisible=${state.overlayVisible} expanded=${state.expanded} recording=${state.isRecording}")
                overlayManager.render(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
                OverlayStateStore.setOverlayVisible(true)
                OverlayStateStore.setExpanded(
                    intent.getBooleanExtra(EXTRA_START_EXPANDED, false)
                )
            }

            ACTION_HIDE_OVERLAY -> {
                OverlayStateStore.setOverlayVisible(false)
                OverlayStateStore.setExpanded(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        OverlayStateStore.setOverlayVisible(false)
        OverlayStateStore.setExpanded(false)
        overlayManager.hide()
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "SeamlessOverlaySvc"
        private const val CHANNEL_ID = "seamless_overlay_channel"
        private const val NOTIFICATION_ID = 1002
        const val ACTION_SHOW_OVERLAY = "com.samsung.android.seamless.ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.samsung.android.seamless.ACTION_HIDE_OVERLAY"
        const val EXTRA_START_EXPANDED = "extra_start_expanded"
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Seamless Overlay",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Shown while the Seamless floating assistant is active"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Seamless assistant")
            .setContentText("Floating assistant is available")
            .setSmallIcon(R.drawable.ic_widget_mic)
            .setOngoing(true)
            .build()
}
