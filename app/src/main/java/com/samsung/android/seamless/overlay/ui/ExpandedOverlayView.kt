package com.samsung.android.seamless.overlay.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.samsung.android.seamless.R
import com.samsung.android.seamless.overlay.OverlayUiState
import com.samsung.android.seamless.widget.RecognitionState

class ExpandedOverlayView(
    context: Context,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit,
    onToggleRecognition: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit
) : FrameLayout(context) {

    private val stateLabel = TextView(context)
    private val transcriptText = TextView(context)
    private val primaryButton = TextView(context)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        background = GradientDrawable().apply {
            cornerRadius = dp(28).toFloat()
            colors = intArrayOf(
                Color.parseColor("#F5101D3D"),
                Color.parseColor("#F20A1530")
            )
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            setStroke(dp(1), Color.parseColor("#3300D4AA"))
        }
        elevation = dp(18).toFloat()
        setPadding(dp(20), dp(18), dp(20), dp(18))

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        container.addView(buildHeaderRow(onCollapse, onDismiss))
        container.addView(spacer(18))

        stateLabel.apply {
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFD7E4F2"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        container.addView(
            stateLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        transcriptText.apply {
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FF00D4AA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create(typeface, Typeface.NORMAL)
            maxLines = 4
            ellipsize = TextUtils.TruncateAt.END
        }
        container.addView(spacer(14))
        container.addView(
            transcriptText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val utilityRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        utilityRow.addView(utilityAction("Copy", Color.parseColor("#FF9EB2C7"), onCopy))
        utilityRow.addView(spacer(12, horizontal = true))
        utilityRow.addView(utilityAction("Clear", Color.parseColor("#FFFF7A7A"), onClear))
        container.addView(spacer(16))
        container.addView(
            utilityRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        primaryButton.apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setTextColor(Color.parseColor("#FF00D4AA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.parseColor("#33222A33"))
            }
            setCompoundDrawablePadding(dp(10))
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(context, R.drawable.ic_widget_mic),
                null,
                null,
                null
            )
            setOnClickListener { onToggleRecognition() }
        }
        container.addView(spacer(18))
        container.addView(
            primaryButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    fun bind(state: OverlayUiState) {
        stateLabel.text = when (state.recognitionState) {
            RecognitionState.IDLE -> "Assistant ready"
            RecognitionState.LISTENING -> "Listening"
            RecognitionState.SPEECH_ACTIVE -> "Transcribing"
            RecognitionState.ERROR -> state.errorMessage.ifBlank { "Something went wrong" }
        }

        transcriptText.text = when {
            state.committedTranscript.isNotBlank() -> state.committedTranscript
            state.partialTranscript.isNotBlank() -> state.partialTranscript
            state.recognitionState == RecognitionState.LISTENING -> "Speak now"
            state.recognitionState == RecognitionState.ERROR -> "Retry when you are ready"
            else -> "Tap start to begin"
        }

        primaryButton.text = when {
            state.isRecording -> "Stop listening"
            state.recognitionState == RecognitionState.ERROR -> "Retry listening"
            else -> "Start listening"
        }

        val buttonBackground = primaryButton.background as GradientDrawable
        buttonBackground.setColor(
            Color.parseColor(
                if (state.isRecording) "#334CAF50" else "#33222A33"
            )
        )
    }

    private fun buildHeaderRow(
        onCollapse: () -> Unit,
        onDismiss: () -> Unit
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val branding = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        branding.addView(ImageView(context).apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_widget_mic))
            setColorFilter(Color.parseColor("#FF00D4AA"))
        }, LinearLayout.LayoutParams(dp(18), dp(18)))
        branding.addView(spacer(8, horizontal = true))
        branding.addView(TextView(context).apply {
            text = "Seamless"
            setTextColor(Color.parseColor("#FF00D4AA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.create(typeface, Typeface.BOLD)
        })

        row.addView(
            branding,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        row.addView(topAction("Minimize", onCollapse))
        row.addView(spacer(8, horizontal = true))
        row.addView(topAction("Dismiss", onDismiss))
        return row
    }

    private fun utilityAction(
        label: String,
        color: Int,
        onClick: () -> Unit
    ): TextView = TextView(context).apply {
        text = label
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setPadding(dp(6), dp(2), dp(6), dp(2))
        setOnClickListener { onClick() }
    }

    private fun topAction(
        label: String,
        onClick: () -> Unit
    ): TextView = TextView(context).apply {
        text = label
        setTextColor(Color.parseColor("#FF9EB2C7"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setPadding(dp(6), dp(4), dp(6), dp(4))
        setOnClickListener { onClick() }
    }

    private fun spacer(size: Int, horizontal: Boolean = false) = TextView(context).apply {
        layoutParams = if (horizontal) {
            LinearLayout.LayoutParams(dp(size), 1)
        } else {
            LinearLayout.LayoutParams(1, dp(size))
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
