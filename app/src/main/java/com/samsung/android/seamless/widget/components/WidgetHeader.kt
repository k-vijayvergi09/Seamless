package com.samsung.android.seamless.widget.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.samsung.android.seamless.R

@Composable
fun WidgetHeader() {
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
                color = WidgetColors.Accent.toGlanceProvider(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
