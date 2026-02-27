package com.samsung.android.seamless.widget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager

enum class RecognitionState {
    IDLE,           // Not recording, ready to start
    LISTENING,      // Connected and recording, waiting for speech
    SPEECH_ACTIVE,  // Voice detected, actively transcribing
    ERROR           // Something went wrong
}

/**
 * SharedPreferences-backed state bridge between [SpeechRecognitionService]
 * and [SeamlessWidget]. Both sides read/write through this class.
 *
 * After every state change, call [updateStateAndRefreshWidget] to push
 * a fresh render to all active widget instances.
 */
class WidgetStateManager(private val context: Context) {

    companion object {
        private const val TAG = "WidgetStateManager"
        private const val PREFS_NAME = "seamless_widget_prefs"
        private const val KEY_STATE = "recognition_state"
        private const val KEY_TRANSCRIPT = "transcript_text"
        private const val KEY_ERROR = "error_message"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var recognitionState: RecognitionState
        get() = try {
            RecognitionState.valueOf(prefs.getString(KEY_STATE, RecognitionState.IDLE.name)!!)
        } catch (e: Exception) {
            RecognitionState.IDLE
        }
        set(value) = prefs.edit().putString(KEY_STATE, value.name).apply()

    var transcriptText: String
        get() = prefs.getString(KEY_TRANSCRIPT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TRANSCRIPT, value).apply()

    var errorMessage: String
        get() = prefs.getString(KEY_ERROR, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ERROR, value).apply()

    /**
     * Updates state (and optionally transcript/error) then triggers a re-render
     * of all SeamlessWidget instances on the home screen.
     *
     * Uses commit() instead of apply() to ensure writes complete before
     * the widget reads the updated values.
     */
    suspend fun updateStateAndRefreshWidget(
        state: RecognitionState,
        transcript: String? = null,
        error: String? = null
    ) {
        // Use synchronous commit() to avoid race condition where widget
        // reads stale data before async apply() completes.
        prefs.edit().apply {
            putString(KEY_STATE, state.name)
            transcript?.let { putString(KEY_TRANSCRIPT, it) }
            error?.let { putString(KEY_ERROR, it) }
        }.commit()

        try {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(SeamlessWidget::class.java)
            Log.i(TAG, "Updating ${ids.size} widget(s) → state=$state")
            for (id in ids) {
                SeamlessWidget().update(context, id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Glance update failed for state=$state", e)
        }
    }

    fun reset() {
        prefs.edit()
            .putString(KEY_STATE, RecognitionState.IDLE.name)
            .putString(KEY_TRANSCRIPT, "")
            .putString(KEY_ERROR, "")
            .apply()
    }

    /**
     * Synchronously sets the state to IDLE using commit() instead of apply().
     * Use this in Service.onDestroy() where we need the write to complete
     * before the widget refresh reads from SharedPreferences.
     */
    fun setIdleStateSync() {
        prefs.edit()
            .putString(KEY_STATE, RecognitionState.IDLE.name)
            .commit()  // Synchronous write
    }
}
