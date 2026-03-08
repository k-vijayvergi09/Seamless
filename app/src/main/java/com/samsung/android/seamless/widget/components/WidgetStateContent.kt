package com.samsung.android.seamless.widget.components

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionStartActivity
import com.samsung.android.seamless.R
import com.samsung.android.seamless.widget.RecognitionState
import com.samsung.android.seamless.widget.ToggleRecognitionActivity

@Composable
fun StateContent(
    context: Context,
    state: RecognitionState
) {
    when (state) {
        RecognitionState.IDLE -> LauncherContent(context, active = false)
        RecognitionState.LISTENING -> LauncherContent(context, active = true)
        RecognitionState.SPEECH_ACTIVE -> LauncherContent(context, active = true)
        RecognitionState.ERROR -> LauncherContent(context, active = false)
    }
}

@Composable
private fun LauncherContent(
    context: Context,
    active: Boolean
) {
    val toggleIntent = Intent(context, ToggleRecognitionActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }
    IconActionButton(
        iconRes = R.drawable.ic_widget_mic,
        action = actionStartActivity(toggleIntent),
        active = active
    )
}
