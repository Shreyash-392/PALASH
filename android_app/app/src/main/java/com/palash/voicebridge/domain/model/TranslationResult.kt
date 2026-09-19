package com.palash.voicebridge.domain.model

/**
 * Result from the translation engine.
 */
data class TranslationResult(
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val isVerified: Boolean,          // True if from verified phrase DB
    val latencyMs: Long,              // Actual measured latency
    val engineUsed: String            // "verified_db", "indictrans2", "placeholder"
)

/**
 * Result from the ASR engine.
 */
data class AsrResult(
    val transcript: String,
    val language: String,
    val confidence: Float,
    val latencyMs: Long,
    val engineUsed: String
)

/**
 * Result from the TTS engine.
 */
sealed class AudioResult {
    data class AudioData(
        val audioBytes: ByteArray,
        val sampleRate: Int,
        val latencyMs: Long,
        val source: String            // "cached", "tts", "placeholder"
    ) : AudioResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AudioData) return false
            return audioBytes.contentEquals(other.audioBytes) &&
                    sampleRate == other.sampleRate
        }
        override fun hashCode(): Int {
            var result = audioBytes.contentHashCode()
            result = 31 * result + sampleRate
            return result
        }
    }

    data class TextOnly(
        val text: String,
        val reason: String
    ) : AudioResult()
}

/**
 * A vocabulary entry for bilingual word learning.
 */
data class VocabularyEntry(
    val id: String,
    val category: String,
    val hindi: String,
    val santali: String,
    val english: String,
    val emoji: String? = null,
    val audioFile: String? = null,
    val verified: Boolean = false
)
