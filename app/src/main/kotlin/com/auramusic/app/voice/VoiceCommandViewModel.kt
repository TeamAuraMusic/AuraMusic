package com.auramusic.app.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.app.constants.EnableVoiceCommandsKey
import com.auramusic.app.constants.EnableVoiceWakeWordKey
import com.auramusic.app.constants.VoiceWakeWordKey
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    private val voiceCommandManager: VoiceCommandManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var playerConnectionProvider: (() -> PlayerConnection?)? = null
    private var onSearchCallback: ((String) -> Unit)? = null
    private var onNavigateCallback: ((String) -> Unit)? = null

    private var isAppInForeground = false
    private var hasMicPermission = false
    private var voiceEnabled = false
    private var wakeWordEnabled = false
    private var customWakeWord = "aura"

    private var restartJob: Job? = null
    private var feedbackJob: Job? = null
    private var consecutiveErrors = 0
    private val maxConsecutiveErrors = 5

    init {
        observePreferences()
        collectRecognitionEvents()
    }

    fun bindHandlers(
        playerConnectionProvider: () -> PlayerConnection?,
        onSearch: (String) -> Unit,
        onNavigate: (String) -> Unit,
    ) {
        this.playerConnectionProvider = playerConnectionProvider
        this.onSearchCallback = onSearch
        this.onNavigateCallback = onNavigate
    }

    fun onAppForeground() {
        isAppInForeground = true
        consecutiveErrors = 0
        maybeStartWakeWordListening()
    }

    fun onAppBackground() {
        isAppInForeground = false
        stopEverything()
        _uiState.update { VoiceUiState() }
    }

    fun onMicPermissionChanged(granted: Boolean) {
        hasMicPermission = granted
        if (granted) maybeStartWakeWordListening()
    }

    fun startManualSession() {
        stopEverything()
        consecutiveErrors = 0
        _uiState.update {
            VoiceUiState(
                isVisible = true,
                mode = VoiceMode.MANUAL,
                phase = VoicePhase.LISTENING,
            )
        }
        voiceCommandManager.startListening(RecognitionMode.COMMAND)
    }

    fun dismissOverlay() {
        stopEverything()
        _uiState.update { VoiceUiState() }
        consecutiveErrors = 0
        maybeStartWakeWordListening()
    }

    private fun stopEverything() {
        feedbackJob?.cancel()
        feedbackJob = null
        restartJob?.cancel()
        restartJob = null
        voiceCommandManager.stopListening()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            context.dataStore.data
                .map { prefs ->
                    Triple(
                        prefs[EnableVoiceCommandsKey] ?: false,
                        prefs[EnableVoiceWakeWordKey] ?: false,
                        prefs[VoiceWakeWordKey] ?: "aura"
                    )
                }
                .distinctUntilChanged()
                .collect { (enabled, wakeWord, customWake) ->
                    voiceEnabled = enabled
                    wakeWordEnabled = wakeWord
                    customWakeWord = customWake
                    if (enabled && wakeWord && isAppInForeground && hasMicPermission) {
                        maybeStartWakeWordListening()
                    } else if (!enabled || !wakeWord) {
                        if (_uiState.value.mode == VoiceMode.WAKE_WORD) {
                            voiceCommandManager.stopListening()
                            restartJob?.cancel()
                        }
                    }
                }
        }
    }

    private fun collectRecognitionEvents() {
        viewModelScope.launch {
            voiceCommandManager.events.collect { event ->
                when (event) {
                    is VoiceRecognitionEvent.Ready -> {
                        consecutiveErrors = 0
                        if (_uiState.value.isVisible) {
                            _uiState.update { it.copy(phase = VoicePhase.LISTENING) }
                        }
                    }

                    is VoiceRecognitionEvent.Rms -> {
                        if (_uiState.value.isVisible) {
                            _uiState.update { it.copy(amplitude = event.value) }
                        }
                    }

                    is VoiceRecognitionEvent.PartialText -> {
                        val currentMode = _uiState.value.mode
                        if (currentMode == VoiceMode.WAKE_WORD) {
                            val match = VoiceCommandParser.extractWakeWord(event.text, customWakeWord)
                            if (match.detected) {
                                handleWakeWordDetected(match.remainingText)
                            }
                        } else if (_uiState.value.isVisible) {
                            _uiState.update {
                                it.copy(
                                    recognizedText = event.text,
                                    phase = VoicePhase.LISTENING
                                )
                            }
                        }
                    }

                    is VoiceRecognitionEvent.FinalText -> {
                        handleFinalText(event.text)
                    }

                    is VoiceRecognitionEvent.EndOfSpeech -> {
                        if (_uiState.value.isVisible) {
                            _uiState.update { it.copy(phase = VoicePhase.PROCESSING) }
                        }
                    }

                    is VoiceRecognitionEvent.Error -> {
                        handleError(event)
                    }
                }
            }
        }
    }

    private fun handleWakeWordDetected(remainingText: String) {
        voiceCommandManager.stopListening()
        restartJob?.cancel()
        if (remainingText.isNotEmpty()) {
            _uiState.update {
                VoiceUiState(
                    isVisible = true,
                    mode = VoiceMode.COMMAND,
                    phase = VoicePhase.PROCESSING,
                    recognizedText = remainingText,
                )
            }
            processCommand(remainingText)
        } else {
            _uiState.update {
                VoiceUiState(
                    isVisible = true,
                    mode = VoiceMode.COMMAND,
                    phase = VoicePhase.LISTENING,
                )
            }
            voiceCommandManager.startListening(RecognitionMode.COMMAND)
        }
    }

    private fun handleFinalText(text: String) {
        val currentMode = _uiState.value.mode

        when (currentMode) {
            VoiceMode.WAKE_WORD -> {
                if (text.isNotEmpty()) {
                    val match = VoiceCommandParser.extractWakeWord(text, customWakeWord)
                    if (match.detected) {
                        handleWakeWordDetected(match.remainingText)
                    } else {
                        scheduleWakeWordRestart()
                    }
                } else {
                    scheduleWakeWordRestart()
                }
            }

            VoiceMode.COMMAND, VoiceMode.MANUAL -> {
                if (text.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            recognizedText = text,
                            phase = VoicePhase.PROCESSING
                        )
                    }
                    processCommand(text)
                } else {
                    _uiState.update {
                        it.copy(
                            phase = VoicePhase.ERROR,
                            errorMessage = "I didn't catch that"
                        )
                    }
                    scheduleOverlayDismiss()
                }
            }
        }
    }

    private fun handleError(event: VoiceRecognitionEvent.Error) {
        val currentMode = _uiState.value.mode

        if (currentMode == VoiceMode.WAKE_WORD) {
            if (event.recoverable) {
                consecutiveErrors++
                if (consecutiveErrors < maxConsecutiveErrors) {
                    scheduleWakeWordRestart()
                }
                // If too many errors, stop trying — user can re-trigger via button
            }
            return
        }

        // Command/Manual mode errors
        if (event.recoverable && _uiState.value.isVisible) {
            val errorMsg = if (event.code == android.speech.SpeechRecognizer.ERROR_NO_MATCH ||
                event.code == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            ) {
                "I didn't catch that"
            } else {
                event.message
            }
            _uiState.update {
                it.copy(phase = VoicePhase.ERROR, errorMessage = errorMsg)
            }
            scheduleOverlayDismiss()
        } else {
            _uiState.update {
                it.copy(phase = VoicePhase.ERROR, errorMessage = event.message)
            }
            scheduleOverlayDismiss()
        }
    }

    private fun processCommand(text: String) {
        viewModelScope.launch {
            val command = VoiceCommandParser.parseCommand(text, customWakeWord)
            when (command) {
                is VoiceCommand.WakeWordDetected -> {
                    _uiState.update {
                        it.copy(
                            mode = VoiceMode.COMMAND,
                            phase = VoicePhase.LISTENING,
                            recognizedText = "",
                        )
                    }
                    voiceCommandManager.startListening(RecognitionMode.COMMAND)
                }

                is VoiceCommand.Unknown -> {
                    _uiState.update {
                        it.copy(
                            phase = VoicePhase.FEEDBACK,
                            feedbackText = "I didn't understand: \"$text\"",
                        )
                    }
                    scheduleOverlayDismiss()
                }

                else -> {
                    val feedback = VoiceCommandActionExecutor.execute(
                        command = command,
                        playerConnection = playerConnectionProvider?.invoke(),
                        onSearch = { query -> onSearchCallback?.invoke(query) },
                        onNavigate = { route -> onNavigateCallback?.invoke(route) },
                    )
                    _uiState.update {
                        it.copy(
                            phase = VoicePhase.FEEDBACK,
                            feedbackText = feedback,
                        )
                    }
                    scheduleOverlayDismiss()
                }
            }
        }
    }

    /**
     * After showing feedback/error, dismiss the overlay and go back to idle.
     * Then auto-restart wake word listening (Siri/Gemini style).
     */
    private fun scheduleOverlayDismiss() {
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(1500)
            _uiState.update { VoiceUiState() }
            // Restart wake word if enabled and no excessive errors
            if (wakeWordEnabled && voiceEnabled && isAppInForeground && hasMicPermission
                && consecutiveErrors < maxConsecutiveErrors
            ) {
                delay(500)
                maybeStartWakeWordListening()
            }
        }
    }

    /**
     * Restart wake word listening with backoff to avoid rapid loops.
     */
    private fun scheduleWakeWordRestart() {
        if (!isAppInForeground || !voiceEnabled || !wakeWordEnabled || !hasMicPermission) return
        if (consecutiveErrors >= maxConsecutiveErrors) return

        restartJob?.cancel()
        val delayMs = when {
            consecutiveErrors == 0 -> 200L
            consecutiveErrors < 3 -> 500L
            else -> 1000L
        }
        restartJob = viewModelScope.launch {
            delay(delayMs)
            if (isAppInForeground && voiceEnabled && wakeWordEnabled && hasMicPermission
                && _uiState.value.mode == VoiceMode.WAKE_WORD && !_uiState.value.isVisible
            ) {
                voiceCommandManager.startListening(RecognitionMode.WAKE_WORD)
            }
        }
    }

    private fun maybeStartWakeWordListening() {
        if (!voiceEnabled || !wakeWordEnabled || !isAppInForeground || !hasMicPermission) return
        if (_uiState.value.isVisible) return
        if (_uiState.value.mode == VoiceMode.COMMAND || _uiState.value.mode == VoiceMode.MANUAL) return

        _uiState.update {
            VoiceUiState(mode = VoiceMode.WAKE_WORD, phase = VoicePhase.IDLE)
        }
        consecutiveErrors = 0
        voiceCommandManager.startListening(RecognitionMode.WAKE_WORD)
    }

    override fun onCleared() {
        super.onCleared()
        stopEverything()
        voiceCommandManager.destroy()
    }
}
