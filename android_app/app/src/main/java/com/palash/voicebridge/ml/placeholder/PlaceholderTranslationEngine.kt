package com.palash.voicebridge.ml.placeholder

import com.palash.voicebridge.domain.engine.TranslationEngine
import com.palash.voicebridge.domain.model.TranslationResult
import kotlinx.coroutines.delay

/**
 * Placeholder translation engine for Phase 1.
 * Returns sample translations with simulated latency.
 * Will be replaced by IndicTrans2TranslationEngine in Phase 2.
 */
class PlaceholderTranslationEngine : TranslationEngine {

    // Sample translations for demo purposes
    private val sampleTranslations = mapOf(
        "नमस्ते" to "ᱡᱚᱦᱟᱨ",
        "धन्यवाद" to "ᱥᱟᱨᱦᱟᱣ",
        "हाँ" to "ᱦᱮᱸ",
        "नहीं" to "ᱵᱟᱝ",
        "अच्छा" to "ᱵᱩᱜᱤᱱ",
        "पानी" to "ᱫᱟᱹᱜ",
        "खाना" to "ᱡᱚᱢ",
        "स्कूल" to "ᱤᱥᱠᱩᱞ",
        "शिक्षक" to "ᱜᱩᱨᱩ",
        "बच्चे" to "ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚ"
    )

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        val startTime = System.currentTimeMillis()

        // Simulate processing time
        delay(500)

        val translation = sampleTranslations[text.trim()]
            ?: "[ᱯᱞᱮᱥᱦᱚᱞᱰᱟᱨ] $text"

        val latency = System.currentTimeMillis() - startTime

        return TranslationResult(
            sourceText = text,
            translatedText = translation,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            isVerified = false,
            latencyMs = latency,
            engineUsed = "placeholder"
        )
    }

    override fun isAvailable(): Boolean = true

    override fun engineName(): String = "Placeholder (Phase 1)"

    override fun supportedPairs(): List<Pair<String, String>> = listOf(
        "hin_Deva" to "sat_Olck"
    )

    override fun release() {
        // No resources to release
    }
}
