package com.samsung.android.seamless.widget

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.samsung.android.seamless.MainActivity
import com.samsung.android.seamless.service.SpeechRecognitionService

/**
 * Glance [ActionCallback] invoked when the user taps the widget.
 *
 * Uses [SpeechRecognitionService.isRunning] (an in-memory flag) rather than SharedPreferences
 * to decide start vs stop, so the toggle is always consistent regardless of coroutine timing.
 */
class ToggleRecognitionAction : ActionCallback {

    override suspend fun onAction(
        context: android.content.Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            Log.d(TAG, "onAction ▶ isRunning=${SpeechRecognitionService.isRunning}")

            if (SpeechRecognitionService.isRunning) {
                Log.d(TAG, "Service running → stopping")
                SpeechRecognitionService.stopRecognition(context)
            } else {
                val hasPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "hasAudioPermission=$hasPermission")

                if (hasPermission) {
                    val stateManager = WidgetStateManager(context)
                    stateManager.transcriptText = ""
                    stateManager.errorMessage = ""
                    Log.d(TAG, "Calling startRecognition")
                    SpeechRecognitionService.startRecognition(context)
                } else {
                    Log.w(TAG, "No audio permission — launching MainActivity for request")
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_REQUEST_AUDIO_PERMISSION, true)
                    }
                    context.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            // Glance silently swallows ActionCallback exceptions — catch here so it appears in Logcat.
            Log.e(TAG, "onAction CRASHED", e)
        }
    }

    companion object {
        private const val TAG = "ToggleAction"
    }
}
