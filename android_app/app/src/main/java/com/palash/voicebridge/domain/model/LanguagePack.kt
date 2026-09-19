package com.palash.voicebridge.domain.model

/**
 * Represents a language supported by the system.
 * Designed to be extensible for Ho, Mundari, and other languages.
 */
data class LanguagePack(
    val code: String,        // e.g., "hin_Deva", "sat_Olck"
    val name: String,        // e.g., "Hindi", "Santali"
    val nativeName: String,  // e.g., "हिंदी", "ᱥᱟᱱᱛᱟᱲᱤ"
    val script: String,      // e.g., "Devanagari", "Ol Chiki"
    val bcp47: String        // e.g., "hi", "sat"
) {
    companion object {
        val HINDI = LanguagePack(
            code = "hin_Deva",
            name = "Hindi",
            nativeName = "हिंदी",
            script = "Devanagari",
            bcp47 = "hi"
        )
        val MUNDARI = LanguagePack(
            code = "unr_Deva",
            name = "Mundari",
            nativeName = "मुंडारी",
            script = "Devanagari",
            bcp47 = "unr"
        )
        val SANTALI = LanguagePack(
            code = "sat_Olck",
            name = "Santali",
            nativeName = "ᱥᱟᱱᱛᱟᱲᱤ",
            script = "Ol Chiki",
            bcp47 = "sat"
        )
    }
}
