package com.palash.voicebridge.ml.placeholder

import com.palash.voicebridge.domain.engine.AsrEngine
import com.palash.voicebridge.domain.model.AsrResult
import kotlinx.coroutines.delay

/**
 * Placeholder ASR engine for Phase 1.
 * Returns canned Hindi transcripts with simulated latency.
 * Will be replaced by IndicConformerAsrEngine in Phase 3.
 */
class PlaceholderAsrEngine : AsrEngine {

    // Sample classroom utterances for demo
    private val sampleTranscripts = listOf(
        "बच्चों, आज हम गिनती सीखेंगे।",
        "दो और तीन को जोड़ो।",
        "यह कौन सा आकार है?",
        "कितने सेब हैं?",
        "सब मिलकर बोलो।",
        "अपनी किताब खोलो।",
        "ध्यान से सुनो।",
        "बहुत अच्छा!",
        "नमस्ते बच्चों!",
        "यह सही जवाब है।"
    )

    private var currentIndex = 0

    override suspend fun transcribe(
        audioData: ByteArray,
        sampleRate: Int,
        language: String
    ): AsrResult {
        val startTime = System.currentTimeMillis()

        // Simulate ASR processing time
        delay(800)

        val transcript = sampleTranscripts[currentIndex % sampleTranscripts.size]
        currentIndex++

        val latency = System.currentTimeMillis() - startTime

        return AsrResult(
            transcript = transcript,
            language = language,
            confidence = 0.95f,
            latencyMs = latency,
            engineUsed = "placeholder"
        )
    }

    override fun isAvailable(): Boolean = true

    override fun engineName(): String = "Placeholder (Phase 1)"

    override fun supportedLanguages(): List<String> = listOf("hin_Deva")

    override fun release() {
        // No resources to release
    }
}
