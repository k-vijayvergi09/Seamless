package com.samsung.android.seamless.widget.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.layout.padding

@Composable
fun IconActionButton(
    iconRes: Int,
    action: Action,
    active: Boolean = false
) {
    Box(
        modifier = GlanceModifier
            .size(64.dp)
            .cornerRadius(20.dp)
            .background((if (active) WidgetColors.ButtonBgActive else WidgetColors.ButtonBg).toGlanceProvider())
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier
                .size(26.dp)
                .padding(1.dp)
        )
    }
}
