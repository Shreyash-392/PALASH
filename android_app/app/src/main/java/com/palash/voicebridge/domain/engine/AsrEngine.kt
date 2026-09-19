package com.palash.voicebridge.domain.engine

import com.palash.voicebridge.domain.model.AsrResult

/**
 * Abstraction for Automatic Speech Recognition engines.
 * Designed for AI4Bharat IndicConformer integration in Phase 3.
 */
interface AsrEngine {
    /**
     * Transcribe audio to text.
     * @param audioData Raw audio bytes (PCM 16-bit, mono)
     * @param sampleRate Sample rate in Hz (typically 16000)
     * @param language Language code (e.g., "hin_Deva")
     */
    suspend fun transcribe(
        audioData: ByteArray,
        sampleRate: Int = 16000,
        language: String = "hin_Deva"
    ): AsrResult

    /** Check if the ASR engine is loaded and ready */
    fun isAvailable(): Boolean

    /** Get the name of this engine for diagnostics */
    fun engineName(): String

    /** Get supported languages */
    fun supportedLanguages(): List<String>

    /** Release resources */
    fun release()
}
