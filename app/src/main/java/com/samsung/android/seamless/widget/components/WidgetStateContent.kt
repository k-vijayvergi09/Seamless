package com.samsung.android.seamless.widget.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.samsung.android.seamless.widget.RecognitionState

@Composable
fun StateContent(
    state: RecognitionState,
    transcript: String,
    error: String
) {
    when (state) {
        RecognitionState.IDLE -> IdleContent(transcript)
        RecognitionState.LISTENING -> ListeningContent()
        RecognitionState.SPEECH_ACTIVE -> SpeechActiveContent(transcript)
        RecognitionState.ERROR -> ErrorContent(error)
    }
}

@Composable
private fun IdleContent(transcript: String) {
    if (transcript.isNotBlank()) {
        TranscriptText(transcript)
    } else {
        SoundWavesImage()
    }
    Spacer(modifier = GlanceModifier.height(12.dp))
    HintText(text = "Tap to start listening", color = WidgetColors.Muted)
}

@Composable
private fun ListeningContent() {
    SoundWavesImage()
    Spacer(modifier = GlanceModifier.height(12.dp))
    HintText(text = "Listening… Tap to stop", color = WidgetColors.Active)
}

@Composable
private fun SpeechActiveContent(transcript: String) {
    TranscriptText(transcript.ifBlank { "…" })
    Spacer(modifier = GlanceModifier.height(12.dp))
    HintText(text = "Tap to stop", color = WidgetColors.Active)
}

@Composable
private fun ErrorContent(error: String) {
    Text(
        text = error.ifBlank { "Something went wrong" },
        style = TextStyle(
            color = WidgetColors.Error.toGlanceProvider(),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        ),
        modifier = GlanceModifier.fillMaxWidth()
    )
    Spacer(modifier = GlanceModifier.height(8.dp))
    HintText(text = "Tap to retry", color = WidgetColors.Muted, fontSize = 11)
}
