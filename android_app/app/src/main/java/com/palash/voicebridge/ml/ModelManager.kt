package com.palash.voicebridge.ml

import com.palash.voicebridge.domain.engine.AsrEngine
import com.palash.voicebridge.domain.engine.TranslationEngine
import com.palash.voicebridge.domain.engine.TtsEngine

/**
 * Manages ML model lifecycle.
 * Tracks model availability and provides status for the UI.
 */
class ModelManager(
    val translationEngine: TranslationEngine,
    val asrEngine: AsrEngine,
    val ttsEngine: TtsEngine
) {
    data class ModelStatus(
        val name: String,
        val engineName: String,
        val isAvailable: Boolean,
        val type: ModelType
    )

    enum class ModelType {
        TRANSLATION, ASR, TTS
    }

    fun getModelStatuses(): List<ModelStatus> = listOf(
        ModelStatus(
            name = "Translation (Hindi → Santali)",
            engineName = translationEngine.engineName(),
            isAvailable = translationEngine.isAvailable(),
            type = ModelType.TRANSLATION
        ),
        ModelStatus(
            name = "Speech Recognition (Hindi)",
            engineName = asrEngine.engineName(),
            isAvailable = asrEngine.isAvailable(),
            type = ModelType.ASR
        ),
        ModelStatus(
            name = "Text-to-Speech (Santali)",
            engineName = ttsEngine.engineName(),
            isAvailable = ttsEngine.isAvailable(),
            type = ModelType.TTS
        )
    )

    fun isFullyOperational(): Boolean =
        translationEngine.isAvailable() && asrEngine.isAvailable()

    fun release() {
        translationEngine.release()
        asrEngine.release()
        ttsEngine.release()
    }
}
