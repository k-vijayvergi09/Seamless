package com.samsung.android.seamless.overlay

import com.samsung.android.seamless.widget.RecognitionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OverlayStateStore {
    private val _state = MutableStateFlow(OverlayUiState())
    val state: StateFlow<OverlayUiState> = _state.asStateFlow()

    fun update(transform: (OverlayUiState) -> OverlayUiState) {
        _state.value = transform(_state.value)
    }

    fun setOverlayVisible(visible: Boolean) {
        update { it.copy(overlayVisible = visible) }
    }

    fun setExpanded(expanded: Boolean) {
        update { it.copy(expanded = expanded) }
    }

    fun setRecognitionState(
        recognitionState: RecognitionState,
        committedTranscript: String = _state.value.committedTranscript,
        partialTranscript: String = _state.value.partialTranscript,
        errorMessage: String = _state.value.errorMessage
    ) {
        update {
            it.copy(
                recognitionState = recognitionState,
                committedTranscript = committedTranscript,
                partialTranscript = partialTranscript,
                errorMessage = errorMessage,
                isRecording = recognitionState == RecognitionState.LISTENING ||
                        recognitionState == RecognitionState.SPEECH_ACTIVE
            )
        }
    }

    fun clearTranscript() {
        update {
            it.copy(
                committedTranscript = "",
                partialTranscript = "",
                errorMessage = ""
            )
        }
    }
}
