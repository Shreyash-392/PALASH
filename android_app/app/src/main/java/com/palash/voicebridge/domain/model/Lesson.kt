package com.palash.voicebridge.domain.model

/**
 * Represents a lesson in the lesson library.
 */
data class Lesson(
    val id: String,
    val classLevel: Int,         // e.g., 1, 2, 3
    val subject: String,         // e.g., "Mathematics", "Language"
    val subjectHindi: String,    // e.g., "गणित", "भाषा"
    val topic: String,           // e.g., "Addition"
    val topicHindi: String,      // e.g., "जोड़"
    val title: String,           // English title
    val titleHindi: String,      // Hindi title
    val titleSantali: String,    // Santali title (Ol Chiki)
    val learningOutcome: String,
    val learningOutcomeHindi: String,
    val instructionHindi: String,
    val instructionSantali: String,
    val activity: String,
    val activityHindi: String,
    val assessmentHindi: String,
    val assessmentSantali: String,
    val teacherPromptHindi: String,
    val teacherPromptSantali: String,
    val audioFile: String? = null  // optional audio filename
)
