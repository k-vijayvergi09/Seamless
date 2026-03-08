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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import com.samsung.android.seamless.R

@Composable
fun TranscriptText(
    text: String,
    modifier: GlanceModifier = GlanceModifier
) {
    Text(
        text = text,
        style = TextStyle(
            color = WidgetColors.Accent.toGlanceProvider(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    )
}

@Composable
fun PrimaryStatusText(
    text: String,
    modifier: GlanceModifier = GlanceModifier
) {
    Text(
        text = text,
        style = TextStyle(
            color = WidgetColors.TextPrimary.toGlanceProvider(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
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
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    )
}

@Composable
fun UtilityActionRow(
    primaryText: String,
    primaryAction: Action,
    secondaryText: String? = null,
    secondaryAction: Action? = null
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UtilityActionText(
            text = primaryText,
            action = primaryAction,
            color = WidgetColors.Muted
        )
        if (secondaryText != null && secondaryAction != null) {
            Spacer(modifier = GlanceModifier.width(12.dp))
            UtilityActionText(
                text = secondaryText,
                action = secondaryAction,
                color = WidgetColors.SubtleDanger
            )
        }
    }
}

@Composable
private fun UtilityActionText(
    text: String,
    action: Action,
    color: Color
) {
    Text(
        text = text,
        style = TextStyle(
            color = color.toGlanceProvider(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        modifier = GlanceModifier
            .cornerRadius(8.dp)
            .clickable(action)
            .padding(horizontal = 6.dp, vertical = 2.dp)
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
            .height(36.dp)
            .cornerRadius(12.dp)
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
                modifier = GlanceModifier.size(16.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = text,
                style = TextStyle(
                    color = WidgetColors.Accent.toGlanceProvider(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start
                )
            )
        }
    }
}
