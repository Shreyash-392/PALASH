package com.palash.voicebridge.ui.voice

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palash.voicebridge.PalashApp
import com.palash.voicebridge.ml.asr.AndroidSpeechRecognizerEngine
import com.palash.voicebridge.ml.asr.AndroidSpeechRecognizerEngine.SpeechState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.palash.voicebridge.domain.model.LanguagePack

data class VoiceUiState(
    val selectedLanguage: LanguagePack = LanguagePack.SANTALI,
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val audioAmplitude: Float = 0f,
    val hindiTranscript: String = "",
    val santaliTranslation: String = "",
    val isVerified: Boolean = false,
    val engineUsed: String = "",
    val customInputText: String = "",
    val error: String = "",
    val speechRecognizerAvailable: Boolean = true,
    val isPlayingAudio: Boolean = false
)

class VoiceTranslationViewModel(
    private val app: PalashApp
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    val speechEngine = AndroidSpeechRecognizerEngine(app)

    init {
        _uiState.value = _uiState.value.copy(
            speechRecognizerAvailable = speechEngine.isAvailable
        )

        viewModelScope.launch {
            speechEngine.state.collect { speechState ->
                when (speechState) {
                    is SpeechState.Idle -> {
                        _uiState.value = _uiState.value.copy(
                            isListening = false,
                            isProcessing = false
                        )
                    }
                    is SpeechState.Listening -> {
                        _uiState.value = _uiState.value.copy(
                            isListening = true,
                            isProcessing = false,
                            error = ""
                        )
                    }
                    is SpeechState.Processing -> {
                        _uiState.value = _uiState.value.copy(
                            isListening = false,
                            isProcessing = true
                        )
                    }
                    is SpeechState.Result -> {
                        _uiState.value = _uiState.value.copy(
                            isListening = false,
                            isProcessing = false,
                            hindiTranscript = speechState.text,
                            error = ""
                        )
                        translateText(speechState.text)
                    }
                    is SpeechState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isListening = false,
                            isProcessing = false,
                            error = speechState.message
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            speechEngine.amplitude.collect { amp ->
                _uiState.value = _uiState.value.copy(audioAmplitude = amp)
            }
        }
    }

    fun selectLanguage(language: LanguagePack) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        if (_uiState.value.hindiTranscript.isNotEmpty()) {
            translateText(_uiState.value.hindiTranscript)
        }
    }

    fun updateCustomInputText(text: String) {
        _uiState.value = _uiState.value.copy(customInputText = text)
    }

    fun translateCustomText(text: String? = null) {
        val query = (text ?: _uiState.value.customInputText).trim()
        if (query.isEmpty()) return

        _uiState.value = _uiState.value.copy(
            hindiTranscript = query,
            error = ""
        )
        translateText(query)
    }

    private fun translateText(hindiText: String) {
        _uiState.value = _uiState.value.copy(isProcessing = true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentLang = _uiState.value.selectedLanguage
                val result = app.translationRepository.translate(
                    text = hindiText,
                    sourceLanguage = "hin_Deva",
                    targetLanguage = currentLang.code
                )

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        santaliTranslation = result.translatedText,
                        isVerified = result.isVerified,
                        engineUsed = result.engineUsed,
                        isProcessing = false
                    )

                    // Auto-play speech for BOTH Santali and Mundari!
                    playSantaliAudio()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = "अनुवाद में त्रुटि: " + e.message
                    )
                }
            }
        }
    }

    fun playSantaliAudio() {
        val translationText = _uiState.value.santaliTranslation
        if (translationText.isEmpty()) return

        val currentLang = _uiState.value.selectedLanguage

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isPlayingAudio = true)
            }

            val audioResult = app.audioRepository.getAudio(
                text = translationText,
                language = currentLang.code
            )

            if (audioResult is com.palash.voicebridge.domain.model.AudioResult.AudioData) {
                app.audioRepository.playAudio(audioResult)
                val textLength = translationText.length
                val playbackDurationMs = (textLength * 120L).coerceIn(1200L, 5000L)
                kotlinx.coroutines.delay(playbackDurationMs)
            } else {
                kotlinx.coroutines.delay(1000)
            }

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isPlayingAudio = false)
            }
        }
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            speechEngine.stopListening()
        } else {
            speechEngine.resetState()
            _uiState.value = _uiState.value.copy(error = "")
            speechEngine.startListening()
        }
    }

    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            hindiTranscript = "",
            santaliTranslation = "",
            error = "",
            customInputText = ""
        )
        speechEngine.resetState()
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine.release()
    }

    class Factory(private val app: PalashApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VoiceTranslationViewModel(app) as T
        }
    }
}
