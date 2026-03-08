package com.samsung.android.seamless.overlay

sealed interface OverlayAction {
    data object ShowCollapsed : OverlayAction
    data object Expand : OverlayAction
    data object Collapse : OverlayAction
    data object Dismiss : OverlayAction
    data object StartListening : OverlayAction
    data object StopListening : OverlayAction
    data object CopyTranscript : OverlayAction
    data object ClearTranscript : OverlayAction
}
