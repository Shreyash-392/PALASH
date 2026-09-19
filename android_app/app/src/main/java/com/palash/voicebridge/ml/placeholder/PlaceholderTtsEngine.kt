package com.palash.voicebridge.ml.placeholder

import com.palash.voicebridge.domain.engine.TtsEngine
import com.palash.voicebridge.domain.model.AudioResult

/**
 * Placeholder TTS engine for Phase 1.
 * Returns text-only results (no actual synthesis).
 * Will be replaced by a real Santali TTS engine in Phase 3.
 */
class PlaceholderTtsEngine : TtsEngine {

    override suspend fun synthesize(
        text: String,
        language: String
    ): AudioResult {
        // In Phase 1, we return text-only since no TTS model is loaded
        return AudioResult.TextOnly(
            text = text,
            reason = "Placeholder TTS — real Santali TTS will be integrated in Phase 3"
        )
    }

    override fun isAvailable(): Boolean = false // Placeholder is never "really" available

    override fun engineName(): String = "Placeholder (Phase 1)"

    override fun supportedLanguages(): List<String> = listOf("sat_Olck")

    override fun release() {
        // No resources to release
    }
}
