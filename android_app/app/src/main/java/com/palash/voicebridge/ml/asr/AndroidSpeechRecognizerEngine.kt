package com.palash.voicebridge.ml.asr

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

/**
 * Truly Offline Hindi Speech Recognition using Vosk API.
 */
class AndroidSpeechRecognizerEngine(private val context: Context) : RecognitionListener {
    sealed class SpeechState {
        object Idle : SpeechState()
        object Listening : SpeechState()
        object Processing : SpeechState()
        data class Result(val text: String, val confidence: Float) : SpeechState()
        data class Error(val message: String) : SpeechState()
    }

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    var isAvailable = false
        private set

    init {
        // Unpack the Vosk model from assets asynchronously
        StorageService.unpack(context, "model-hi", "model",
            { m ->
                this.model = m
                isAvailable = true
            },
            { e ->
                _state.value = SpeechState.Error("Failed to load offline model: ${e.message}")
            }
        )
    }

    fun startListening(preferOffline: Boolean = true) {
        if (model == null) {
            _state.value = SpeechState.Error("ऑफ़लाइन मॉडल लोड हो रहा है, कृपया प्रतीक्षा करें...")
            return
        }
        
        _state.value = SpeechState.Listening
        _amplitude.value = 0.3f // Vosk doesn't natively expose RMS easily, fake ambient amplitude
        
        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
        } catch (e: IOException) {
            _state.value = SpeechState.Error("माइक्रोफ़ोन त्रुटि: ${e.message}")
        }
    }

    fun stopListening() {
        _state.value = SpeechState.Processing
        speechService?.stop()
        speechService = null
        _amplitude.value = 0f
    }

    fun cancel() {
        speechService?.cancel()
        speechService = null
        _state.value = SpeechState.Idle
        _amplitude.value = 0f
    }

    fun resetState() {
        _state.value = SpeechState.Idle
        _amplitude.value = 0f
    }

    fun release() {
        speechService?.stop()
        speechService?.shutdown()
        model?.close()
    }

    override fun onPartialResult(hypothesis: String?) {
        // Flash animation or partial results can be handled here if needed
        _amplitude.value = (Math.random() * 0.5 + 0.3).toFloat() // Simulate amplitude changes during speech
    }

    override fun onResult(hypothesis: String?) {
        parseResult(hypothesis)
    }

    override fun onFinalResult(hypothesis: String?) {
        parseResult(hypothesis)
    }

    override fun onError(exception: Exception?) {
        _state.value = SpeechState.Error(exception?.message ?: "Unknown Error")
        _amplitude.value = 0f
    }

    override fun onTimeout() {
        _state.value = SpeechState.Error("Timeout")
        _amplitude.value = 0f
    }
    
    private fun parseResult(jsonStr: String?) {
        if (jsonStr.isNullOrEmpty()) return
        try {
            val jsonObject = JSONObject(jsonStr)
            val text = jsonObject.optString("text", "")
            
            // Only update state if it actually heard something valid
            if (text.isNotEmpty()) {
                _state.value = SpeechState.Result(text, 1.0f)
                // Auto-stop the continuous listening after a full sentence is detected
                // This mimics the original Google SpeechRecognizer behavior
                mainHandler.post { stopListening() }
            }
            // We safely ignore empty text results since Vosk emits them frequently when stopping
        } catch (e: Exception) {
            // Ignore parse errors
        }
    }
}
