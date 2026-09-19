package com.palash.voicebridge.ml.translation

/**
 * Tokenizer and script processor for IndicTrans2 (hin_Deva -> sat_Olck).
 * Handles:
 * 1. Indic normalization (Devanagari danda, nukta, zero-width joiners)
 * 2. Language tag injection (__hin_Deva__, __sat_Olck__)
 * 3. Subword segmentation and Ol Chiki script detokenization
 */
class IndicTrans2Tokenizer {

    companion object {
        const val HINDI_TAG = "__hin_Deva__"
        const val SANTALI_OL_CHIKI_TAG = "__sat_Olck__"
        const val UNK_TOKEN = "<unk>"
        const val PAD_TOKEN = "<pad>"
        const val BOS_TOKEN = "<s>"
        const val EOS_TOKEN = "</s>"

        // Ol Chiki Unicode Range: U+1C50 to U+1C7F
        // Mapping reference for numerals and fundamental phonetic sounds
        val DEVA_TO_OL_CHIKI_DIGITS = mapOf(
            '०' to '᱐', '१' to '᱑', '२' to '᱒', '३' to '᱓', '४' to '᱔',
            '५' to '᱕', '६' to '᱖', '७' to '᱗', '८' to '᱘', '९' to '᱙',
            '0' to '᱐', '1' to '᱑', '2' to '᱒', '3' to '᱓', '4' to '᱔',
            '5' to '᱕', '6' to '᱖', '7' to '᱗', '8' to '᱘', '9' to '᱙'
        )
    }

    /**
     * Normalizes Hindi Devanagari text prior to tokenization.
     */
    fun normalizeHindi(text: String): String {
        return text.trim()
            .replace("\u200D", "") // remove ZWJ
            .replace("\u200C", "") // remove ZWNJ
            .replace("।", " । ")  // isolate Devanagari danda
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Prepares input text with IndicTrans2 language prefixes.
     */
    fun preprocess(
        text: String,
        srcLang: String = "hin_Deva",
        tgtLang: String = "sat_Olck"
    ): String {
        val normalized = normalizeHindi(text)
        val srcTag = "__${srcLang}__"
        val tgtTag = "__${tgtLang}__"
        return "$srcTag $tgtTag $normalized"
    }

    /**
     * Postprocesses translated tokens into clean Ol Chiki output.
     */
    fun postprocess(rawOutput: String): String {
        return rawOutput
            .replace(SANTALI_OL_CHIKI_TAG, "")
            .replace(HINDI_TAG, "")
            .replace(BOS_TOKEN, "")
            .replace(EOS_TOKEN, "")
            .replace(PAD_TOKEN, "")
            .replace(UNK_TOKEN, "")
            .replace("@@ ", "") // BPE subword stitch
            .replace("  ", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Transliterates standard numerals into Ol Chiki digits (᱐-᱙).
     */
    fun convertNumeralsToOlChiki(text: String): String {
        val sb = StringBuilder()
        for (char in text) {
            sb.append(DEVA_TO_OL_CHIKI_DIGITS[char] ?: char)
        }
        return sb.toString()
    }
}
