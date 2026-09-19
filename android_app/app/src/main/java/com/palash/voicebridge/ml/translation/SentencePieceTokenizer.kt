package com.palash.voicebridge.ml.translation

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class SentencePieceTokenizer(vocabFile: File) {

    private val tokenToId: Map<String, Int>
    private val idToToken: Map<Int, String>

    val hiTokenId: Long = 128036L
    val unrTokenId: Long = 128104L
    val bosTokenId: Long = 0L
    val padTokenId: Long = 1L
    val eosTokenId: Long = 2L

    init {
        val gson = Gson()
        val type = object : TypeToken<Map<String, Int>>() {}.type
        val rawMap: Map<String, Int> = InputStreamReader(vocabFile.inputStream(), StandardCharsets.UTF_8).use { reader ->
            gson.fromJson(reader, type)
        }
        tokenToId = rawMap
        idToToken = rawMap.entries.associate { (k, v) -> v to k }
    }

    val vocabSize: Int
        get() = tokenToId.size

    fun encode(text: String, srcLang: String = "hi", addSpecialTokens: Boolean = true): List<Long> {
        val resultIds = mutableListOf<Long>()
        if (addSpecialTokens) {
            val srcLangId = if (srcLang == "hi") hiTokenId else unrTokenId
            resultIds.add(srcLangId)
        }

        val punctRegex = Regex("([।?!,.!:])")
        val spaceRegex = Regex("\\s+")

        // Clean & normalize input text, separating punctuation cleanly
        val cleanedText = text.trim()
            .replace(punctRegex, " $1 ")
            .replace(spaceRegex, " ")

        val words = cleanedText.split(" ")

        for (word in words) {
            if (word.isEmpty()) continue
            val isPunctuation = word.matches(Regex("[।?!,.!:]"))
            val spWord = if (isPunctuation) word else "\u2581$word"
            val subwordIds = bpeTokenizeWord(spWord)
            resultIds.addAll(subwordIds)
        }

        if (addSpecialTokens) {
            resultIds.add(eosTokenId)
        }

        return resultIds
    }

    private fun bpeTokenizeWord(word: String): List<Long> {
        val ids = mutableListOf<Long>()
        var start = 0
        val len = word.length

        while (start < len) {
            var end = len
            var matched = false

            while (end > start) {
                val sub = word.substring(start, end)
                val id = tokenToId[sub]
                if (id != null) {
                    ids.add(id.toLong())
                    start = end
                    matched = true
                    break
                }
                end--
            }

            if (!matched) {
                // Single character fallback
                val singleChar = word.substring(start, start + 1)
                val charId = tokenToId[singleChar] ?: tokenToId["<unk>"] ?: 3
                ids.add(charId.toLong())
                start++
            }
        }
        return ids
    }

    fun decode(ids: List<Long>, skipSpecialTokens: Boolean = true): String {
        val sb = StringBuilder()
        for (id in ids) {
            val intId = id.toInt()
            if (skipSpecialTokens && (intId == bosTokenId.toInt() || intId == padTokenId.toInt() ||
                        intId == eosTokenId.toInt() || intId == hiTokenId.toInt() || intId == unrTokenId.toInt())) {
                continue
            }
            val token = idToToken[intId] ?: continue
            if (skipSpecialTokens && (token == "__hi__" || token == "__unr__" || token == "<s>" || token == "</s>" || token == "<pad>")) {
                continue
            }
            if (token.startsWith("\u2581")) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(token.substring(1))
            } else {
                sb.append(token)
            }
        }
        return sb.toString().trim()
    }
}
