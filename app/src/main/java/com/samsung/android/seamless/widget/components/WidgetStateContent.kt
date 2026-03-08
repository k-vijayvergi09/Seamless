package com.samsung.android.seamless.widget.components

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.samsung.android.seamless.R
import com.samsung.android.seamless.widget.ClearTranscriptActivity
import com.samsung.android.seamless.widget.CopyTranscriptActivity
import com.samsung.android.seamless.widget.RecognitionState
import com.samsung.android.seamless.widget.ToggleRecognitionActivity

@Composable
fun StateContent(
    context: Context,
    state: RecognitionState,
    transcript: String,
    error: String
) {
    when (state) {
        RecognitionState.IDLE -> IdleContent(context, transcript)
        RecognitionState.LISTENING -> ListeningContent(context)
        RecognitionState.SPEECH_ACTIVE -> SpeechActiveContent(context, transcript)
        RecognitionState.ERROR -> ErrorContent(context, error)
    }
}

@Composable
private fun IdleContent(context: Context, transcript: String) {
    if (transcript.isNotBlank()) {
        TranscriptText(
            text = transcript,
            modifier = GlanceModifier
        )
        Spacer(modifier = GlanceModifier.height(10.dp))
        UtilityActionRow(
            primaryText = "Copy",
            primaryAction = actionStartActivity(transcriptIntent(context)),
            secondaryText = "Clear",
            secondaryAction = actionStartActivity(clearTranscriptIntent(context))
        )
        Spacer(modifier = GlanceModifier.height(12.dp))
        ToggleRecognitionButton(context, text = "Start listening", active = false)
    } else {
        PrimaryStatusText(text = "Ready")
        Spacer(modifier = GlanceModifier.height(6.dp))
        HintText(text = "Tap to start voice capture", color = WidgetColors.Muted, fontSize = 11)
        Spacer(modifier = GlanceModifier.height(14.dp))
        ToggleRecognitionButton(context, text = "Start listening", active = false)
    }
}

@Composable
private fun ListeningContent(context: Context) {
    PrimaryStatusText(text = "Listening")
    Spacer(modifier = GlanceModifier.height(6.dp))
    HintText(text = "Speak now", color = WidgetColors.Muted, fontSize = 11)
    Spacer(modifier = GlanceModifier.height(14.dp))
    ToggleRecognitionButton(context, text = "Stop listening", active = true)
}

@Composable
private fun SpeechActiveContent(context: Context, transcript: String) {
    TranscriptText(
        text = transcript.ifBlank { "Listening..." }
    )
    if (transcript.isNotBlank()) {
        Spacer(modifier = GlanceModifier.height(10.dp))
        UtilityActionRow(
            primaryText = "Copy",
            primaryAction = actionStartActivity(transcriptIntent(context)),
            secondaryText = "Clear",
            secondaryAction = actionStartActivity(clearTranscriptIntent(context))
        )
    } else {
        Spacer(modifier = GlanceModifier.height(6.dp))
        HintText(text = "Transcribing live speech", color = WidgetColors.Muted, fontSize = 11)
    }
    Spacer(modifier = GlanceModifier.height(14.dp))
    ToggleRecognitionButton(context, text = "Stop listening", active = true)
}

@Composable
private fun ErrorContent(context: Context, error: String) {
    Text(
        text = error.ifBlank { "Something went wrong. Please retry." },
        style = TextStyle(
            color = WidgetColors.Error.toGlanceProvider(),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        ),
        modifier = GlanceModifier.fillMaxWidth()
    )
    Spacer(modifier = GlanceModifier.height(8.dp))
    HintText(text = "Check connection and try again", color = WidgetColors.Muted, fontSize = 11)
    Spacer(modifier = GlanceModifier.height(14.dp))
    ToggleRecognitionButton(context, text = "Retry listening", active = false)
}

@Composable
private fun ToggleRecognitionButton(
    context: Context,
    text: String,
    active: Boolean
) {
    val toggleIntent = Intent(context, ToggleRecognitionActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }
    ActionButton(
        text = text,
        iconRes = R.drawable.ic_widget_mic,
        action = actionStartActivity(toggleIntent),
        active = active
    )
}

private fun transcriptIntent(context: Context): Intent =
    Intent(context, CopyTranscriptActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }

private fun clearTranscriptIntent(context: Context): Intent =
    Intent(context, ClearTranscriptActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }
