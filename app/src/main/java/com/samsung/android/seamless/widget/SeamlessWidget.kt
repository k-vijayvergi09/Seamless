package com.samsung.android.seamless.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.samsung.android.seamless.R

private val CyanAccent   = Color(0xFF00D4AA)
private val GreenActive  = Color(0xFF4CAF50)
private val RedError     = Color(0xFFFF5252)
private val MutedGray    = Color(0xFF8899AA)

class SeamlessWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stateManager = WidgetStateManager(context)
        val state        = stateManager.recognitionState
        val transcript   = stateManager.transcriptText
        val error        = stateManager.errorMessage

        provideContent {
            GlanceTheme {
                WidgetContent(state = state, transcript = transcript, error = error, context = context)
            }
        }
    }

    @Composable
    private fun WidgetContent(
        state: RecognitionState,
        transcript: String,
        error: String,
        context: Context
    ) {
        val toggleIntent = Intent(context, ToggleRecognitionActivity::class.java)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(ImageProvider(R.drawable.widget_background))
                .padding(16.dp)
                .clickable(actionStartActivity(toggleIntent)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header: mic icon + app name
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_mic),
                        contentDescription = "Microphone",
                        modifier = GlanceModifier.size(20.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "Seamless",
                        style = TextStyle(
                            color = ColorProvider(day = CyanAccent, night = CyanAccent),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(12.dp))

                // State-specific middle content
                when (state) {
                    RecognitionState.IDLE -> {
                        if (transcript.isNotBlank()) {
                            // Show last transcript after stopping
                            Text(
                                text = transcript,
                                style = TextStyle(
                                    color = ColorProvider(day = CyanAccent, night = CyanAccent),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = GlanceModifier.fillMaxWidth()
                            )
                        } else {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_waves),
                                contentDescription = "Sound waves",
                                modifier = GlanceModifier.fillMaxWidth().height(24.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(12.dp))
                        Text(
                            text = "Tap to start listening",
                            style = TextStyle(
                                color = ColorProvider(day = MutedGray, night = MutedGray),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    RecognitionState.LISTENING -> {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_waves),
                            contentDescription = "Sound waves",
                            modifier = GlanceModifier.fillMaxWidth().height(24.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = GlanceModifier.height(12.dp))
                        Text(
                            text = "Listening\u2026 Tap to stop",
                            style = TextStyle(
                                color = ColorProvider(day = GreenActive, night = GreenActive),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    RecognitionState.SPEECH_ACTIVE -> {
                        Text(
                            text = transcript.ifBlank { "\u2026" },
                            style = TextStyle(
                                color = ColorProvider(day = CyanAccent, night = CyanAccent),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = GlanceModifier.fillMaxWidth()
                        )
                        Spacer(modifier = GlanceModifier.height(12.dp))
                        Text(
                            text = "Tap to stop",
                            style = TextStyle(
                                color = ColorProvider(day = GreenActive, night = GreenActive),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    RecognitionState.ERROR -> {
                        Text(
                            text = error.ifBlank { "Something went wrong" },
                            style = TextStyle(
                                color = ColorProvider(day = RedError, night = RedError),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = GlanceModifier.fillMaxWidth()
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = "Tap to retry",
                            style = TextStyle(
                                color = ColorProvider(day = MutedGray, night = MutedGray),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }
    }
}
