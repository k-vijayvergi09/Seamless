package com.samsung.android.seamless.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SeamlessOverlayService : Service() {

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
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.samsung.android.seamless.ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.samsung.android.seamless.ACTION_HIDE_OVERLAY"
    }
}
