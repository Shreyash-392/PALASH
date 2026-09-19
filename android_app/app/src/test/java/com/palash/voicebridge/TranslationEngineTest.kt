package com.palash.voicebridge

import com.palash.voicebridge.ml.translation.IndicTrans2Tokenizer
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying IndicTrans2 tokenizer, Devanagari normalization,
 * and Ol Chiki numeral conversion without network connectivity.
 */
class TranslationEngineTest {

    private val tokenizer = IndicTrans2Tokenizer()

    @Test
    fun testHindiNormalization() {
        val input = "बच्चों,   आज हम गिनती सीखेंगे।  "
        val normalized = tokenizer.normalizeHindi(input)
        assertEquals("बच्चों, आज हम गिनती सीखेंगे ।", normalized)
    }

    @Test
    fun testIndicTrans2LanguageTags() {
        val input = "दो और तीन को जोड़ो।"
        val preprocessed = tokenizer.preprocess(input, "hin_Deva", "sat_Olck")
        assertTrue(preprocessed.startsWith("__hin_Deva__ __sat_Olck__"))
    }

    @Test
    fun testOlChikiNumeralConversion() {
        val input = "1 2 3 4 5 6 7 8 9 0"
        val converted = tokenizer.convertNumeralsToOlChiki(input)
        assertEquals("᱑ ᱒ ᱓ ᱔ ᱕ ᱖ ᱗ ᱘ ᱙ ᱐", converted)
    }

    @Test
    fun testOlChikiDevanagariDigits() {
        val input = "१२३४५"
        val converted = tokenizer.convertNumeralsToOlChiki(input)
        assertEquals("᱑᱒᱓᱔᱕", converted)
    }

    @Test
    fun testPostProcessing() {
        val raw = "__sat_Olck__ ᱜᱤᱫᱽᱨᱟᱹᱢᱚᱱ, ᱛᱤᱱᱟᱹᱜ ᱟᱞᱮ ᱞᱮᱠᱷᱟ ᱥᱮᱪᱮᱫ ᱟᱹᱞᱮ᱾ </s>"
        val cleaned = tokenizer.postprocess(raw)
        assertEquals("ᱜᱤᱫᱽᱨᱟᱹᱢᱚᱱ, ᱛᱤᱱᱟᱹᱜ ᱟᱞᱮ ᱞᱮᱠᱷᱟ ᱥᱮᱪᱮᱫ ᱟᱹᱞᱮ᱾", cleaned)
    }
}
