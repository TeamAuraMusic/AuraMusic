package com.auramusic.app.voice

sealed class VoiceUiState {
    data object Idle : VoiceUiState()
    data class Listening(val amplitude: Float = 0f) : VoiceUiState()
    data object Processing : VoiceUiState()
    data class PartialResult(val text: String) : VoiceUiState()
    data object CommandRecognized : VoiceUiState()
    data class Error(val message: String) : VoiceUiState()
}
