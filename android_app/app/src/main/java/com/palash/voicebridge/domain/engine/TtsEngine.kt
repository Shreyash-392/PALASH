package com.palash.voicebridge.domain.engine

import com.palash.voicebridge.domain.model.AudioResult

/**
 * Abstraction for Text-to-Speech engines.
 * Fallback architecture:
 *   1. Cached verified audio → play directly
 *   2. Local neural TTS (e.g., Indic Parler-TTS) → synthesize
 *   3. Text-only fallback → display text, no audio
 *
 * The app must never crash if neural TTS is unavailable.
 */
interface TtsEngine {
    /**
     * Synthesize speech from text.
     * @param text The text to speak
     * @param language Language code (e.g., "sat_Olck")
     */
    suspend fun synthesize(
        text: String,
        language: String
    ): AudioResult

    /** Check if the TTS engine is loaded and ready */
    fun isAvailable(): Boolean

    /** Get the name of this engine for diagnostics */
    fun engineName(): String

    /** Get supported languages */
    fun supportedLanguages(): List<String>

    /** Release resources */
    fun release()
}
