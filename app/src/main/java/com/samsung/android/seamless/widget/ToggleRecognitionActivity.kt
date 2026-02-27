package com.samsung.android.seamless.widget

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.samsung.android.seamless.MainActivity
import com.samsung.android.seamless.service.SpeechRecognitionService

/**
 * Transparent trampoline Activity that handles widget taps.
 * Required because foreground services cannot be started from a BroadcastReceiver
 * on Android 12+, but CAN be started from an Activity.
 */
class ToggleRecognitionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate — handling toggle")

        try {
            handleToggle()
        } catch (e: Exception) {
            Log.e(TAG, "Toggle failed", e)
        }

        finish()
    }

    private fun handleToggle() {
        Log.i(TAG, "handleToggle: isRunning=${SpeechRecognitionService.isRunning}")

        if (SpeechRecognitionService.isRunning) {
            Log.i(TAG, "Service running → stopping")
            SpeechRecognitionService.stopRecognition(this)
        } else {
            val hasPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            Log.i(TAG, "hasAudioPermission=$hasPermission")

            if (hasPermission) {
                val stateManager = WidgetStateManager(this)
                stateManager.transcriptText = ""
                stateManager.errorMessage = ""
                Log.i(TAG, "Starting recognition service")
                SpeechRecognitionService.startRecognition(this)
            } else {
                Log.w(TAG, "No audio permission — launching MainActivity")
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_REQUEST_AUDIO_PERMISSION, true)
                }
                startActivity(intent)
            }
        }
    }

    companion object {
        private const val TAG = "ToggleRecognitionAct"
    }
}
