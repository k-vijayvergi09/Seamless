package com.samsung.android.seamless.overlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

class OverlayPermissionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        continueOrRequestPermission()
    }

    override fun onResume() {
        super.onResume()
        if (intent.getBooleanExtra(EXTRA_WAITING_FOR_PERMISSION_RESULT, false)) {
            showOverlayIfPermittedAndFinish()
        }
    }

    private fun continueOrRequestPermission() {
        Log.i(TAG, "continueOrRequestPermission canDrawOverlays=${canDrawOverlays()}")
        if (canDrawOverlays()) {
            showOverlayIfPermittedAndFinish()
            return
        }

        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
        intent.putExtra(EXTRA_WAITING_FOR_PERMISSION_RESULT, true)
    }

    private fun showOverlayIfPermittedAndFinish() {
        Log.i(TAG, "showOverlayIfPermittedAndFinish canDrawOverlays=${canDrawOverlays()}")
        if (canDrawOverlays()) {
            Log.i(TAG, "Starting overlay foreground service")
            ContextCompat.startForegroundService(
                this,
                Intent(this, SeamlessOverlayService::class.java).apply {
                    action = SeamlessOverlayService.ACTION_SHOW_OVERLAY
                    putExtra(
                        SeamlessOverlayService.EXTRA_START_EXPANDED,
                        intent.getBooleanExtra(EXTRA_START_EXPANDED, false)
                    )
                }
            )
        }
        finish()
    }

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    companion object {
        private const val TAG = "OverlayPermissionAct"
        private const val EXTRA_WAITING_FOR_PERMISSION_RESULT = "waiting_for_overlay_permission_result"
        const val EXTRA_START_EXPANDED = "extra_start_expanded"
    }
}
