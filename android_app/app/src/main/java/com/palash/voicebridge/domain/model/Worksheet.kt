package com.palash.voicebridge.domain.model

/**
 * Represents a worksheet template for generating bilingual worksheets.
 */
data class WorksheetTemplate(
    val id: String,
    val classLevel: Int,
    val subject: String,
    val subjectHindi: String,
    val topic: String,
    val topicHindi: String,
    val questionType: QuestionType,
    val instructionHindi: String,
    val instructionSantali: String,
    val questions: List<WorksheetQuestion>
)

data class WorksheetQuestion(
    val id: String,
    val questionHindi: String,
    val questionSantali: String,
    val options: List<String>? = null,       // For MCQ type
    val optionsSantali: List<String>? = null, // Santali options for MCQ
    val answer: String? = null,
    val blankCount: Int = 1                  // For fill-in-the-blank
)

enum class QuestionType {
    COUNTING,
    MATCHING,
    ADDITION,
    FILL_IN_THE_BLANK,
    MULTIPLE_CHOICE,
    CIRCLE_ANSWER,
    COMPARISON
}
