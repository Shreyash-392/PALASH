package com.palash.voicebridge.domain.engine

import com.palash.voicebridge.domain.model.TranslationResult

/**
 * Abstraction for translation engines.
 * Designed so IndicTrans2 can be swapped in during Phase 2,
 * and additional language pairs (Ho, Mundari) can be added later.
 */
interface TranslationEngine {
    /**
     * Translate text from source language to target language.
     * @param text The text to translate
     * @param sourceLanguage Source language code (e.g., "hin_Deva")
     * @param targetLanguage Target language code (e.g., "sat_Olck")
     */
    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult

    /** Check if the engine is loaded and ready */
    fun isAvailable(): Boolean

    /** Get the name of this engine for diagnostics */
    fun engineName(): String

    /** Get supported language pairs */
    fun supportedPairs(): List<Pair<String, String>>

    /** Release resources */
    fun release()
}
