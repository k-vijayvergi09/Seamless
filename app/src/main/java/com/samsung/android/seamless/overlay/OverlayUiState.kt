package com.samsung.android.seamless.overlay

import com.samsung.android.seamless.widget.RecognitionState

data class OverlayUiState(
    val overlayVisible: Boolean = false,
    val expanded: Boolean = false,
    val recognitionState: RecognitionState = RecognitionState.IDLE,
    val committedTranscript: String = "",
    val partialTranscript: String = "",
    val errorMessage: String = "",
    val isRecording: Boolean = false
)
