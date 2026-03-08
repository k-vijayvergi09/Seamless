package com.samsung.android.seamless.widget.components

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

object WidgetColors {
    val Accent = Color(0xFF00D4AA)
    val Active = Color(0xFF4CAF50)
    val Error = Color(0xFFFF5252)
    val Muted = Color(0xFF8899AA)
    val TextPrimary = Color(0xFFD7E4F2)
    val SubtleDanger = Color(0xFFFF7A7A)
    val ButtonBg = Color(0x33222A33)
    val ButtonBgActive = Color(0x334CAF50)
}

fun Color.toGlanceProvider() = ColorProvider(day = this, night = this)
