package com.palash.voicebridge.domain.model

/**
 * Represents a bilingual flashcard.
 */
data class Flashcard(
    val id: String,
    val category: String,        // e.g., "numbers", "animals", "colors", "shapes", "objects"
    val categoryHindi: String,   // e.g., "संख्या", "जानवर"
    val hindi: String,           // Hindi text
    val santali: String,         // Santali text (Ol Chiki)
    val english: String,         // English (for reference)
    val emoji: String,           // Visual representation
    val audioFile: String? = null
)
