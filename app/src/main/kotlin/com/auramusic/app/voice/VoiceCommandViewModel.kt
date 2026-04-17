package com.auramusic.app.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    private val voiceCommandManager: VoiceCommandManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private var onCommandCallback: ((VoiceCommand) -> Unit)? = null

    fun startListening(onCommand: (VoiceCommand) -> Unit) {
        onCommandCallback = onCommand
        viewModelScope.launch {
            voiceCommandManager.voiceState.collect { state ->
                _uiState.value = when (state) {
                    is VoiceState.Idle -> VoiceUiState.Idle
                    is VoiceState.Listening -> VoiceUiState.Listening(state.amplitude)
                    is VoiceState.Processing -> VoiceUiState.Processing
                    is VoiceState.PartialResult -> {
                        _recognizedText.value = state.text
                        VoiceUiState.PartialResult(state.text)
                    }
                    is VoiceState.CommandRecognized -> VoiceUiState.CommandRecognized
                    is VoiceState.Error -> VoiceUiState.Error(state.message)
                }
            }
        }

        voiceCommandManager.startListening { command ->
            _uiState.value = VoiceUiState.CommandRecognized
            onCommandCallback?.invoke(command)
        }
    }

    fun stopListening() {
        voiceCommandManager.stopListening()
        _uiState.value = VoiceUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        voiceCommandManager.destroy()
    }
}
