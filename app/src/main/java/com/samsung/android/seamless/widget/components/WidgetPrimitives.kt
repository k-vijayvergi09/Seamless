package com.samsung.android.seamless.widget.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.samsung.android.seamless.R

@Composable
fun SoundWavesImage() {
    Image(
        provider = ImageProvider(R.drawable.ic_widget_waves),
        contentDescription = "Sound waves",
        modifier = GlanceModifier.fillMaxWidth().height(24.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun TranscriptText(text: String) {
    Text(
        text = text,
        style = TextStyle(
            color = WidgetColors.Accent.toGlanceProvider(),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        ),
        modifier = GlanceModifier.fillMaxWidth()
    )
}

@Composable
fun HintText(
    text: String,
    color: Color,
    fontSize: Int = 12
) {
    Text(
        text = text,
        style = TextStyle(
            color = color.toGlanceProvider(),
            fontSize = fontSize.sp,
            textAlign = TextAlign.Center
        )
    )
}
