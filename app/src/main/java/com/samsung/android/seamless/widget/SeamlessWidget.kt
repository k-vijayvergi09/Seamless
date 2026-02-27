package com.samsung.android.seamless.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.samsung.android.seamless.widget.components.StateContent
import com.samsung.android.seamless.widget.components.WidgetContainer
import com.samsung.android.seamless.widget.components.WidgetHeader

/** Keys for Glance widget state stored via PreferencesGlanceStateDefinition */
object WidgetStateKeys {
    val STATE = stringPreferencesKey("recognition_state")
    val TRANSCRIPT = stringPreferencesKey("transcript_text")
    val ERROR = stringPreferencesKey("error_message")
}

class SeamlessWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val state = parseState(prefs[WidgetStateKeys.STATE])
            val transcript = prefs[WidgetStateKeys.TRANSCRIPT] ?: ""
            val error = prefs[WidgetStateKeys.ERROR] ?: ""

            GlanceTheme {
                WidgetContainer(context) {
                    WidgetHeader()
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    StateContent(state = state, transcript = transcript, error = error)
                }
            }
        }
    }

    private fun parseState(stateStr: String?): RecognitionState =
        try {
            RecognitionState.valueOf(stateStr ?: RecognitionState.IDLE.name)
        } catch (e: Exception) {
            RecognitionState.IDLE
        }
}
