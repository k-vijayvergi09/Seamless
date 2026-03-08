package com.samsung.android.seamless.overlay.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.samsung.android.seamless.R

class CollapsedBubbleView(context: Context) : FrameLayout(context) {

    private val iconView = ImageView(context)
    private val statusDot = FrameLayout(context)

    init {
        val bubbleSize = dp(64)
        layoutParams = LayoutParams(bubbleSize, bubbleSize)

        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = intArrayOf(
                Color.parseColor("#CC0B1630"),
                Color.parseColor("#E6101D3D")
            )
            gradientType = GradientDrawable.RADIAL_GRADIENT
            setStroke(dp(1), Color.parseColor("#3300D4AA"))
        }
        elevation = dp(12).toFloat()
        clipToOutline = true

        addView(
            iconView,
            LayoutParams(dp(28), dp(28), Gravity.CENTER)
        )
        iconView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_widget_mic))

        addView(
            statusDot,
            LayoutParams(dp(10), dp(10), Gravity.END or Gravity.TOP).apply {
                topMargin = dp(10)
                marginEnd = dp(10)
            }
        )
        statusDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#FF5A5A"))
        }

        setRecording(false)
    }

    fun setRecording(isRecording: Boolean) {
        iconView.setColorFilter(
            Color.parseColor(
                if (isRecording) "#FF00E0B8" else "#FFD7E4F2"
            )
        )
        statusDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(
                Color.parseColor(
                    if (isRecording) "#FF00D4AA" else "#667A8A9A"
                )
            )
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
