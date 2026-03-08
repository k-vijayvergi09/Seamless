package com.samsung.android.seamless.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder
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
        overlayManager = OverlayManager(this)
        serviceScope.launch {
            OverlayStateStore.state.collect { state ->
                overlayManager.render(state)
                if (!state.overlayVisible) {
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                OverlayStateStore.setOverlayVisible(true)
                OverlayStateStore.setExpanded(false)
            }

            ACTION_HIDE_OVERLAY -> {
                OverlayStateStore.setOverlayVisible(false)
                OverlayStateStore.setExpanded(false)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        OverlayStateStore.setOverlayVisible(false)
        OverlayStateStore.setExpanded(false)
        overlayManager.hide()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.samsung.android.seamless.ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.samsung.android.seamless.ACTION_HIDE_OVERLAY"
    }
}
