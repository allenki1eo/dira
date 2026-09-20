package com.dira.app.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OverlaySessionState(
    val active: Boolean = false,
    val loading: Boolean = false,
    val instruction: String = "",
    val error: String? = null,
    val listening: Boolean = false,
)

/**
 * Bridge between the capture/overlay service and the Activity UI.
 * Session lives in the foreground service so it survives Home / recents.
 */
object CoachBus {
    private val _state = MutableStateFlow(OverlaySessionState())
    val state: StateFlow<OverlaySessionState> = _state.asStateFlow()

    fun publish(update: OverlaySessionState) {
        _state.value = update
    }

    fun clear() {
        _state.value = OverlaySessionState()
    }
}
