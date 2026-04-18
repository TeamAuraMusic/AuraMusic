package com.auramusic.app.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.app.constants.EnableVoiceCommandsKey
import com.auramusic.app.constants.EnableVoiceWakeWordKey
import com.auramusic.app.constants.VoiceWakeWordKey
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.utils.dataStore
import com.auramusic.app.voice.wakeword.VoskWakeWordDetector
import com.auramusic.app.voice.wakeword.WakeWordService
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
        private val wakeWordDetector: VoskWakeWordDetector,
        private val voiceFeedbackManager: VoiceFeedbackManager,
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
    private var isSessionActive = false // guards against rapid manual clicks

    init {
        observePreferences()
        collectRecognitionEvents()
        // Initialize voice feedback manager
        voiceFeedbackManager.initialize()
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
        // Start wake word detection service when app is visible
        if (voiceEnabled && wakeWordEnabled && hasMicPermission) {
            WakeWordService.start(context)
        }
    }

    fun onAppBackground() {
        isAppInForeground = false
        stopEverything()
        _uiState.update { VoiceUiState() }
        // Optionally stop wake word service to save battery when app is background
        // Siri/Gemini keep it always-on, but that requires system-level integration
        // For now, stop when app backgrounded
        WakeWordService.stop(context)
    }

    fun onMicPermissionChanged(granted: Boolean) {
        hasMicPermission = granted
        // Do NOT auto-start mic on permission grant
    }

    fun startManualSession() {
        // Prevent spamming the mic button rapidly (guard with isSessionActive)
        if (isSessionActive) return
        stopEverything()
        consecutiveErrors = 0
        isSessionActive = true
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
        // Restart VOSK wake word service after overlay dismissed
        if (voiceEnabled && wakeWordEnabled && hasMicPermission && isAppInForeground) {
            restartWakeWordService()
        }
    }

    private fun restartWakeWordService() {
        try {
            WakeWordService.start(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopEverything() {
        feedbackJob?.cancel()
        feedbackJob = null
        restartJob?.cancel()
        restartJob = null
        voiceCommandManager.stopListening()
        isSessionActive = false
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
                    // Do NOT auto-start wake word on app foreground
                    // Wake word only starts when user manually activates mic or says wake word during manual session
                    if (!enabled || !wakeWord) {
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

                    VoiceRecognitionEvent.WakeWordDetected -> {
                        // Wake word detected by VOSK; detector already stopped itself in triggerWakeWord()
                        android.util.Log.d("VoiceCommandViewModel", "WakeWordDetected received, showing overlay")
                        stopEverything()
                        consecutiveErrors = 0
                        _uiState.update {
                            VoiceUiState(
                                isVisible = true,
                                mode = VoiceMode.COMMAND,
                                phase = VoicePhase.LISTENING,
                            )
                        }
                        // Short delay to allow mic handoff from VOSK AudioRecord to SpeechRecognizer
                        viewModelScope.launch {
                            delay(150)
                            voiceCommandManager.startListening(RecognitionMode.COMMAND)
                        }
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
            // Greet user and wait for command
            voiceFeedbackManager.speak("Hello! How can I help you today?")
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
            voiceFeedbackManager.speak(errorMsg)
            scheduleOverlayDismiss()
        } else {
            val errorMsg = event.message
            _uiState.update {
                it.copy(phase = VoicePhase.ERROR, errorMessage = errorMsg)
            }
            voiceFeedbackManager.speak(errorMsg)
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
                    val feedback = "I didn't understand that"
                    _uiState.update {
                        it.copy(
                            phase = VoicePhase.FEEDBACK,
                            feedbackText = feedback,
                        )
                    }
                    voiceFeedbackManager.speak(feedback)
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
                    // Speak feedback result
                    voiceFeedbackManager.speak(feedback)
                    scheduleOverlayDismiss()
                }
            }
        }
    }

    /**
     * After showing feedback/error, dismiss the overlay and go back to idle.
     * Then restart VOSK wake word detection.
     */
    private fun scheduleOverlayDismiss() {
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(1500)
            stopEverything()
            _uiState.update { VoiceUiState() }
            // Restart VOSK wake word service after command completes (only if enabled and in foreground)
            if (voiceEnabled && wakeWordEnabled && hasMicPermission && isAppInForeground) {
                restartWakeWordService()
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
        voiceFeedbackManager.shutdown()
    }
}
