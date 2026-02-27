package com.samsung.android.seamless.widget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

enum class RecognitionState {
    IDLE,           // Not recording, ready to start
    LISTENING,      // Connected and recording, waiting for speech
    SPEECH_ACTIVE,  // Voice detected, actively transcribing
    ERROR           // Something went wrong
}

/**
 * State manager that bridges [SpeechRecognitionService] and [SeamlessWidget].
 *
 * Uses Glance's internal state via [updateAppWidgetState] for reliable widget updates
 * from background contexts (Services, WorkManager). Also maintains SharedPreferences
 * for quick state reads by the Service.
 */
class WidgetStateManager(private val context: Context) {

    companion object {
        private const val TAG = "WidgetStateManager"
        private const val PREFS_NAME = "seamless_widget_prefs"
        private const val KEY_STATE = "recognition_state"
        private const val KEY_TRANSCRIPT = "transcript_text"
        private const val KEY_ERROR = "error_message"
    }

    // SharedPreferences for quick service-side reads (not used by widget directly)
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
     * Updates widget state using Glance's internal state management, then triggers update.
     * This approach works reliably from Service/background contexts.
     */
    suspend fun updateStateAndRefreshWidget(
        state: RecognitionState,
        transcript: String? = null,
        error: String? = null
    ) {
        // Update SharedPreferences for service-side reads
        prefs.edit()
            .putString(KEY_STATE, state.name)
            .apply {
                transcript?.let { putString(KEY_TRANSCRIPT, it) }
                error?.let { putString(KEY_ERROR, it) }
            }
            .commit()

        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(SeamlessWidget::class.java)
            Log.i(TAG, "Updating ${glanceIds.size} widget(s) → state=$state")

            for (glanceId in glanceIds) {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WidgetStateKeys.STATE] = state.name
                        transcript?.let { this[WidgetStateKeys.TRANSCRIPT] = it }
                        error?.let { this[WidgetStateKeys.ERROR] = it }
                    }
                }
                SeamlessWidget().update(context, glanceId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Glance update failed for state=$state", e)
        }
    }

    /**
     * Sets state to IDLE for all widgets. Used in Service.onDestroy().
     */
    suspend fun setIdleStateAndRefresh() {
        prefs.edit()
            .putString(KEY_STATE, RecognitionState.IDLE.name)
            .commit()

        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(SeamlessWidget::class.java)
            Log.i(TAG, "Setting ${glanceIds.size} widget(s) to IDLE on destroy")

            for (glanceId in glanceIds) {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WidgetStateKeys.STATE] = RecognitionState.IDLE.name
                    }
                }
                SeamlessWidget().update(context, glanceId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set IDLE state on destroy", e)
        }
    }

    fun reset() {
        prefs.edit()
            .putString(KEY_STATE, RecognitionState.IDLE.name)
            .putString(KEY_TRANSCRIPT, "")
            .putString(KEY_ERROR, "")
            .apply()
    }
}
