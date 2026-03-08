package com.samsung.android.seamless.overlay.ui

import android.content.Context
import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.samsung.android.seamless.R
import com.samsung.android.seamless.overlay.OverlayUiState
import com.samsung.android.seamless.widget.RecognitionState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class ExpandedOverlayView(
    context: Context,
    private val onCollapse: () -> Unit,
    private val onDismiss: () -> Unit,
    private val onToggleRecognition: () -> Unit,
    private val onCopy: () -> Unit,
    private val onClear: () -> Unit
) : FrameLayout(context) {

    private var uiState by mutableStateOf(OverlayUiState())
    private val overlayOwners = OverlayComposeOwners()

    init {
        overlayOwners.performRestore()
        setViewTreeLifecycleOwner(overlayOwners)
        setViewTreeViewModelStoreOwner(overlayOwners)
        setViewTreeSavedStateRegistryOwner(overlayOwners)

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                OverlayExpandedPanel(
                    state = uiState,
                    onCollapse = onCollapse,
                    onDismiss = onDismiss,
                    onToggleRecognition = onToggleRecognition,
                    onCopy = onCopy,
                    onClear = onClear
                )
            }
        }
        addView(
            composeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun bind(state: OverlayUiState) {
        uiState = state
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        overlayOwners.handleAttach()
    }

    override fun onDetachedFromWindow() {
        overlayOwners.handleDetach()
        super.onDetachedFromWindow()
    }
}

@Composable
private fun OverlayExpandedPanel(
    state: OverlayUiState,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit,
    onToggleRecognition: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 18.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF5101D3D),
                            Color(0xF20A1530)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeaderRow(
                    onCollapse = onCollapse,
                    onDismiss = onDismiss
                )

                ListeningAura(isActive = state.isRecording)

                AnimatedContent(
                    targetState = stateLabel(state),
                    label = "state_label"
                ) { label ->
                    Text(
                        text = label,
                        color = Color(0xFFD7E4F2),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                AnimatedContent(
                    targetState = transcriptText(state),
                    transitionSpec = {
                        androidx.compose.animation.fadeIn(tween(180)) +
                                slideInVertically(initialOffsetY = { it / 6 }) togetherWith
                                androidx.compose.animation.fadeOut(tween(120)) +
                                slideOutVertically(targetOffsetY = { -it / 8 })
                    },
                    label = "transcript_content"
                ) { transcript ->
                    Text(
                        text = transcript,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        color = if (state.recognitionState == RecognitionState.ERROR) {
                            Color(0xFFD7E4F2)
                        } else {
                            Color(0xFF00D4AA)
                        },
                        fontSize = 20.sp,
                        lineHeight = 25.sp,
                        textAlign = TextAlign.Center
                    )
                }

                UtilityRow(
                    modifier = Modifier.padding(top = 16.dp),
                    onCopy = onCopy,
                    onClear = onClear
                )

                PrimaryActionButton(
                    modifier = Modifier.padding(top = 18.dp),
                    active = state.isRecording,
                    text = primaryButtonText(state),
                    onClick = onToggleRecognition
                )
            }
        }
    }    
}

@Composable
private fun HeaderRow(
    onCollapse: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_widget_mic),
                contentDescription = null,
                tint = Color(0xFF00D4AA),
                modifier = Modifier.size(18.dp)
            )
            Text(
            text = "Seamless",
            modifier = Modifier.padding(start = 8.dp),
            color = Color(0xFF00D4AA),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        HeaderAction(text = "Minimize", onClick = onCollapse)
        HeaderAction(text = "Dismiss", onClick = onDismiss)
    }
}

@Composable
private fun HeaderAction(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .padding(start = 10.dp)
            .clickable(onClick = onClick),
        color = Color(0xFF9EB2C7),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ListeningAura(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "listening_aura")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.18f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.18f else 0f,
        targetValue = if (isActive) 0.34f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .padding(top = 18.dp, bottom = 12.dp)
            .size(74.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .background(
                    color = Color(0xFF00D4AA),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x9900D4AA),
                            Color(0x22172434)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_widget_mic),
                contentDescription = null,
                tint = Color(0xFF00E0B8),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun UtilityRow(
    modifier: Modifier = Modifier,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UtilityAction(text = "Copy", color = Color(0xFF9EB2C7), onClick = onCopy)
        UtilityAction(text = "Clear", color = Color(0xFFFF7A7A), onClick = onClear)
    }
}

@Composable
private fun UtilityAction(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun PrimaryActionButton(
    modifier: Modifier = Modifier,
    active: Boolean,
    text: String,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (active) Color(0x334CAF50) else Color(0x33222A33)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_widget_mic),
                contentDescription = null,
                tint = Color(0xFF00D4AA),
                modifier = Modifier.size(18.dp)
            )
            AnimatedContent(
                targetState = text,
                modifier = Modifier.padding(start = 10.dp),
                label = "primary_button_text"
            ) { label ->
                Text(
                    text = label,
                    color = Color(0xFF00D4AA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun stateLabel(state: OverlayUiState): String =
    when (state.recognitionState) {
        RecognitionState.IDLE -> "Assistant ready"
        RecognitionState.LISTENING -> "Listening"
        RecognitionState.SPEECH_ACTIVE -> "Transcribing"
        RecognitionState.ERROR -> state.errorMessage.ifBlank { "Something went wrong" }
    }

private fun transcriptText(state: OverlayUiState): String =
    when {
        state.committedTranscript.isNotBlank() -> state.committedTranscript
        state.partialTranscript.isNotBlank() -> state.partialTranscript
        state.recognitionState == RecognitionState.LISTENING -> "Speak now"
        state.recognitionState == RecognitionState.ERROR -> "Retry when you are ready"
        else -> "Tap start to begin"
    }

private fun primaryButtonText(state: OverlayUiState): String =
    when {
        state.isRecording -> "Stop listening"
        state.recognitionState == RecognitionState.ERROR -> "Retry listening"
        else -> "Start listening"
    }

private class OverlayComposeOwners :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val ownerViewModelStore = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = ownerViewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    fun performRestore() {
        savedStateController.performAttach()
        savedStateController.performRestore(Bundle())
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun handleAttach() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun handleDetach() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
}
