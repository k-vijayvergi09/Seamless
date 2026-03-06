package com.samsung.android.seamless.widget.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
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
fun TranscriptText(
    text: String,
    modifier: GlanceModifier = GlanceModifier
) {
    Text(
        text = text,
        style = TextStyle(
            color = WidgetColors.Accent.toGlanceProvider(),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun HintText(
    text: String,
    color: Color,
    fontSize: Int = 12,
    modifier: GlanceModifier = GlanceModifier
) {
    Text(
        text = text,
        style = TextStyle(
            color = color.toGlanceProvider(),
            fontSize = fontSize.sp,
            textAlign = TextAlign.Center
        ),
        modifier = modifier
    )
}

@Composable
fun ActionButton(
    text: String,
    iconRes: Int,
    action: Action,
    active: Boolean = false
) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(42.dp)
            .cornerRadius(14.dp)
            .background((if (active) WidgetColors.ButtonBgActive else WidgetColors.ButtonBg).toGlanceProvider())
            .clickable(action),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(18.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = text,
                style = TextStyle(
                    color = WidgetColors.Accent.toGlanceProvider(),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start
                )
            )
        }
    }
}
