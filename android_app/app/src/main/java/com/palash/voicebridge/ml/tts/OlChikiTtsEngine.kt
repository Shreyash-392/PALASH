package com.palash.voicebridge.ml.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.palash.voicebridge.domain.engine.TtsEngine
import com.palash.voicebridge.domain.model.AudioResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class OlChikiTtsEngine(
    private val context: Context
) : TtsEngine, TextToSpeech.OnInitListener {

    private val TAG = "PalashTTS"
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val prefs = context.getSharedPreferences("tts_prefs", Context.MODE_PRIVATE)

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale("hi", "IN")
            val result = tts?.setLanguage(locale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
                reloadSettings()
                Log.d(TAG, "Native Android TTS initialized successfully with Hindi locale")
            } else {
                Log.w(TAG, "Hindi locale missing or not supported in TTS engine, falling back to default")
                isInitialized = true
                reloadSettings()
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
        }
    }

    fun reloadSettings() {
        val speed = prefs.getFloat("tts_speed", 0.95f)
        val pitch = prefs.getFloat("tts_pitch", 1.0f)
        val voiceName = prefs.getString("tts_voice_name", null)

        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)

        if (voiceName != null) {
            val voices = try { tts?.voices } catch (e: Exception) { null }
            val selectedVoice = voices?.find { it.name == voiceName }
            if (selectedVoice != null) {
                tts?.voice = selectedVoice
            }
        }

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set audio attributes", e)
        }
    }

    fun getAvailableVoices(): List<Voice> {
        return try {
            val allVoices = tts?.voices ?: return emptyList()
            allVoices.filter { it.locale.language == "hi" }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private val olChikiToDevanagariMap = mapOf(
        'ᱚ' to "अ", 'ᱛ' to "त", 'ᱜ' to "ग", 'ᱝ' to "ंग", 'ᱞ' to "ल", 'ᱟ' to "आ",
        'ᱠ' to "क", 'ᱡ' to "ज", 'ᱢ' to "म", 'ᱣ' to "व", 'ᱤ' to "इ", 'ᱥ' to "स",
        'ᱦ' to "ह", 'ᱧ' to "ञ", 'ᱨ' to "र", 'ᱩ' to "उ", 'ᱪ' to "च", 'ᱫ' to "द",
        'ᱬ' to "ण", 'ᱭ' to "य", 'ᱮ' to "ए", 'ᱯ' to "प", 'ᱰ' to "ड", 'ᱱ' to "न",
        'ᱲ' to "ड़", 'ᱳ' to "ओ", 'ᱴ' to "ट", 'ᱵ' to "ब", 'ᱶ' to "ं", 'ᱷ' to "ह",
        'ᱸ' to "ँ", 'ᱹ' to "", 'ᱺ' to "ः", 'ᱻ' to "'", 'ᱽ' to "", 'ᱼ' to "-",
        '᱐' to "0", '᱑' to "1", '᱒' to "2", '᱓' to "3", '᱔' to "4", '᱕' to "5",
        '᱖' to "6", '᱗' to "7", '᱘' to "8", '᱙' to "9"
    )

    private fun transliterateToDevanagari(text: String): String {
        val builder = StringBuilder()
        for (char in text) {
            builder.append(olChikiToDevanagariMap[char] ?: char)
        }
        return builder.toString()
    }

    fun speakDirect(text: String, language: String): Boolean {
        if (!isInitialized || tts == null) return false

        reloadSettings()

        val pronouncableText = if (language.startsWith("sat")) {
            transliterateToDevanagari(text)
        } else {
            text
        }

        val utteranceId = "tts_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val res = tts?.speak(pronouncableText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        Log.d(TAG, "tts.speak result: $res for text: '$pronouncableText'")
        return res == TextToSpeech.SUCCESS
    }

    override suspend fun synthesize(
        text: String,
        language: String
    ): AudioResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        if (!isInitialized || tts == null) {
            return@withContext AudioResult.TextOnly(text, "TTS Engine not initialized")
        }

        val spoken = speakDirect(text, language)
        val latency = System.currentTimeMillis() - startTime

        if (spoken) {
            AudioResult.AudioData(
                audioBytes = ByteArray(1024),
                sampleRate = 16000,
                latencyMs = latency,
                source = "Native-TTS-Direct"
            )
        } else {
            AudioResult.TextOnly(text, "Speech synthesis failed")
        }
    }

    override fun isAvailable(): Boolean = isInitialized

    override fun engineName(): String = "Native Android Speech Engine"

    override fun supportedLanguages(): List<String> = listOf("sat_Olck", "unr_Deva", "hin_Deva")

    override fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}
