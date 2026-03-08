package com.samsung.android.seamless.widget

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.samsung.android.seamless.MainActivity
import com.samsung.android.seamless.overlay.OverlayPermissionActivity
import com.samsung.android.seamless.service.SpeechRecognitionService

/**
 * Transparent trampoline Activity that handles widget start/stop taps.
 */
class ToggleRecognitionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: handling toggle")

        try {
            handleToggle()
        } catch (e: Exception) {
            Log.e(TAG, "Toggle failed", e)
        }

        finish()
    }

    private fun handleToggle() {
        if (intent.getBooleanExtra(EXTRA_FORCE_STOP, false)) {
            Log.i(TAG, "Force-stop intent received")
            SpeechRecognitionService.stopRecognition(this)
            Toast.makeText(this, "Stopped listening", Toast.LENGTH_SHORT).show()
            return
        }

        val hasPermission =
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        Log.i(TAG, "hasAudioPermission=$hasPermission")

        if (hasPermission) {
            Log.i(TAG, "Opening overlay launcher")
            startActivity(
                Intent(this, OverlayPermissionActivity::class.java).apply {
                    putExtra(OverlayPermissionActivity.EXTRA_START_EXPANDED, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } else {
            Log.w(TAG, "No audio permission; redirecting to MainActivity for permission prompt")
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_REQUEST_AUDIO_PERMISSION, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            Toast.makeText(this, "Please grant microphone permission", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "ToggleRecognitionAct"
        const val EXTRA_FORCE_STOP = "extra_force_stop"
    }
}
