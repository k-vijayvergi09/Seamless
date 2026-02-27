package com.samsung.android.seamless.widget.components

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

object WidgetColors {
    val Accent = Color(0xFF00D4AA)
    val Active = Color(0xFF4CAF50)
    val Error = Color(0xFFFF5252)
    val Muted = Color(0xFF8899AA)
}

fun Color.toGlanceProvider() = ColorProvider(day = this, night = this)
