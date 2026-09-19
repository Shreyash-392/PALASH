package com.palash.voicebridge.ml.translation

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import com.palash.voicebridge.domain.engine.TranslationEngine
import com.palash.voicebridge.domain.model.TranslationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.concurrent.atomic.AtomicBoolean

class MundariTranslationEngine(
    private val modelDir: File
) : TranslationEngine {

    private val TAG = "MundariNMT"
    private var ortEnv: OrtEnvironment? = null
    private var encoderSession: OrtSession? = null
    private var decoderSession: OrtSession? = null
    private var decoderWithPastSession: OrtSession? = null
    private var tokenizer: SentencePieceTokenizer? = null

    private val isLoaded = AtomicBoolean(false)
    var loadError: String = ""
        private set

    init {
        loadModel(modelDir)
    }

    private fun loadModel(dir: File) {
        try {
            if (!dir.exists()) {
                loadError = "Model dir does not exist: " + dir.absolutePath
                Log.e(TAG, loadError)
                return
            }

            val vocabFile = File(dir, "vocab.json")
            val encoderFile = File(dir, "encoder_model.onnx")
            val decoderFile = File(dir, "decoder_model.onnx")
            val decoderWithPastFile = File(dir, "decoder_with_past_model.onnx")

            val missing = mutableListOf<String>()
            if (!vocabFile.exists()) missing.add("vocab.json")
            if (!encoderFile.exists()) missing.add("encoder_model.onnx")
            if (!decoderFile.exists()) missing.add("decoder_model.onnx")
            if (!decoderWithPastFile.exists()) missing.add("decoder_with_past_model.onnx")

            if (missing.isNotEmpty()) {
                loadError = "Missing files in " + dir.absolutePath + ": " + missing.joinToString()
                Log.e(TAG, loadError)
                return
            }

            tokenizer = SentencePieceTokenizer(vocabFile)

            ortEnv = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
            }

            encoderSession = ortEnv!!.createSession(encoderFile.absolutePath, sessionOptions)
            decoderSession = ortEnv!!.createSession(decoderFile.absolutePath, sessionOptions)
            decoderWithPastSession = ortEnv!!.createSession(decoderWithPastFile.absolutePath, sessionOptions)

            isLoaded.set(true)
            loadError = ""
            Log.d(TAG, "Mundari INT8 ONNX models loaded successfully from " + dir.absolutePath)
        } catch (e: Exception) {
            loadError = "Failed to load model: " + e.message
            Log.e(TAG, loadError, e)
            isLoaded.set(false)
        }
    }

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        if (!isLoaded.get()) {
            val latency = System.currentTimeMillis() - startTime
            val errMsg = if (loadError.isNotEmpty()) loadError else "Model not loaded from " + modelDir.absolutePath
            return@withContext TranslationResult(
                sourceText = text,
                translatedText = "[Error] " + errMsg,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                isVerified = false,
                latencyMs = latency,
                engineUsed = engineName()
            )
        }

        val tok = tokenizer
        if (tok == null) {
            return@withContext TranslationResult(
                sourceText = text,
                translatedText = "[Error] Tokenizer not initialized",
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                isVerified = false,
                latencyMs = 0L,
                engineUsed = engineName()
            )
        }

        val staticEncoderTensors = mutableListOf<OnnxTensor>()

        try {
            val env = ortEnv!!
            val inputIds = tok.encode(text, addSpecialTokens = true)
            val seqLen = inputIds.size.toLong()

            val inputIdsArray = LongArray(inputIds.size) { inputIds[it] }
            val attnMaskArray = LongArray(inputIds.size) { 1L }

            val inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIdsArray), longArrayOf(1, seqLen))
            val attnMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attnMaskArray), longArrayOf(1, seqLen))

            // --- 1. ENCODER PASS ---
            val encoderInputs = mapOf("input_ids" to inputIdsTensor, "attention_mask" to attnMaskTensor)
            val encoderResults = encoderSession!!.run(encoderInputs)
            val lastHiddenStateTensor = encoderResults.get("last_hidden_state").get() as OnnxTensor
            val hiddenShape = lastHiddenStateTensor.info.shape
            val hiddenArray = extractAndClose(lastHiddenStateTensor)
            encoderResults.close()
            inputIdsTensor.close()

            val encHiddenTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(hiddenArray), hiddenShape)

            // --- 2. DECODER INITIAL STEP (Step 0) ---
            val decStartArray = longArrayOf(tok.eosTokenId, tok.unrTokenId)
            val decInputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(decStartArray), longArrayOf(1, 2))

            val decInputs = mapOf(
                "input_ids" to decInputIdsTensor,
                "encoder_attention_mask" to attnMaskTensor,
                "encoder_hidden_states" to encHiddenTensor
            )

            val decResults = decoderSession!!.run(decInputs)
            val logitsTensor = decResults.get("logits").get() as OnnxTensor
            val vocabSize = logitsTensor.info.shape[2].toInt()

            val offsetStep1 = 1 * vocabSize
            var nextTokenId = argmaxFromTensorAndClose(logitsTensor, offsetStep1, vocabSize)

            val generatedIds = mutableListOf<Long>()
            generatedIds.add(tok.eosTokenId)
            generatedIds.add(tok.unrTokenId)
            generatedIds.add(nextTokenId.toLong())

            val layerPastList = mutableListOf<LayerKeyValue>()
            for (i in 0 until 12) {
                val decKeyTensor = decResults.get("present." + i + ".decoder.key").get() as OnnxTensor
                val decValTensor = decResults.get("present." + i + ".decoder.value").get() as OnnxTensor
                val encKeyTensor = decResults.get("present." + i + ".encoder.key").get() as OnnxTensor
                val encValTensor = decResults.get("present." + i + ".encoder.value").get() as OnnxTensor

                val decKeyArr = extractAndClose(decKeyTensor)
                val decValArr = extractAndClose(decValTensor)
                val encKeyArr = extractAndClose(encKeyTensor)
                val encValArr = extractAndClose(encValTensor)

                layerPastList.add(LayerKeyValue(decKeyArr, decValArr, 2L))

                // Create static encoder tensors ONCE for past_key_values across all autoregressive steps
                val encK = OnnxTensor.createTensor(env, FloatBuffer.wrap(encKeyArr), longArrayOf(1, 16, seqLen, 64))
                val encV = OnnxTensor.createTensor(env, FloatBuffer.wrap(encValArr), longArrayOf(1, 16, seqLen, 64))
                staticEncoderTensors.add(encK)
                staticEncoderTensors.add(encV)
            }

            decResults.close()
            decInputIdsTensor.close()
            encHiddenTensor.close()

            // --- 3. AUTOREGRESSIVE GENERATION WITH PAST ---
            val eosId = tok.eosTokenId
            val maxTokens = 25 // Optimal length for classroom and spoken sentences
            var repeatCount = 0
            var lastTokenId = -1L

            while (nextTokenId.toLong() != eosId && nextTokenId.toLong() != 0L && generatedIds.size < maxTokens) {
                // Repetition penalty / loop breaker
                if (nextTokenId.toLong() == lastTokenId) {
                    repeatCount++
                    if (repeatCount >= 2) break
                } else {
                    repeatCount = 0
                    lastTokenId = nextTokenId.toLong()
                }

                val nextInputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(nextTokenId.toLong())), longArrayOf(1, 1))

                val stepInputs = mutableMapOf<String, OnnxTensor>()
                stepInputs["input_ids"] = nextInputTensor
                stepInputs["encoder_attention_mask"] = attnMaskTensor

                val stepTensorsToClose = mutableListOf<OnnxTensor>()
                stepTensorsToClose.add(nextInputTensor)

                for (i in 0 until 12) {
                    val layer = layerPastList[i]
                    val decK = OnnxTensor.createTensor(env, FloatBuffer.wrap(layer.decKey), longArrayOf(1, 16, layer.decSeqLen, 64))
                    val decV = OnnxTensor.createTensor(env, FloatBuffer.wrap(layer.decVal), longArrayOf(1, 16, layer.decSeqLen, 64))

                    stepInputs["past_key_values." + i + ".decoder.key"] = decK
                    stepInputs["past_key_values." + i + ".decoder.value"] = decV
                    stepInputs["past_key_values." + i + ".encoder.key"] = staticEncoderTensors[i * 2]
                    stepInputs["past_key_values." + i + ".encoder.value"] = staticEncoderTensors[i * 2 + 1]

                    stepTensorsToClose.add(decK)
                    stepTensorsToClose.add(decV)
                }

                val stepResults = decoderWithPastSession!!.run(stepInputs)
                val stepLogitsTensor = stepResults.get("logits").get() as OnnxTensor
                nextTokenId = argmaxFromTensorAndClose(stepLogitsTensor, 0, vocabSize)
                generatedIds.add(nextTokenId.toLong())

                val newDecSeqLen = layerPastList[0].decSeqLen + 1
                for (i in 0 until 12) {
                    val newDecKeyTensor = stepResults.get("present." + i + ".decoder.key").get() as OnnxTensor
                    val newDecValTensor = stepResults.get("present." + i + ".decoder.value").get() as OnnxTensor

                    val newDecKeyArr = extractAndClose(newDecKeyTensor)
                    val newDecValArr = extractAndClose(newDecValTensor)

                    layerPastList[i] = layerPastList[i].copy(
                        decKey = newDecKeyArr,
                        decVal = newDecValArr,
                        decSeqLen = newDecSeqLen
                    )
                }

                for (t in stepTensorsToClose) {
                    try { t.close() } catch (_: Exception) {}
                }
                stepResults.close()
            }

            attnMaskTensor.close()

            val cleanMundari = tok.decode(generatedIds, skipSpecialTokens = true)
            val latency = System.currentTimeMillis() - startTime
            Log.d(TAG, "Mundari generated in " + latency + "ms: '" + cleanMundari + "'")

            return@withContext TranslationResult(
                sourceText = text,
                translatedText = cleanMundari,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                isVerified = false,
                latencyMs = latency,
                engineUsed = engineName()
            )

        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Log.e(TAG, "Mundari Translation error", e)
            return@withContext TranslationResult(
                sourceText = text,
                translatedText = "[Error] Inference error: " + e.message,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                isVerified = false,
                latencyMs = latency,
                engineUsed = engineName()
            )
        } finally {
            for (t in staticEncoderTensors) {
                try { t.close() } catch (_: Exception) {}
            }
        }
    }

    override fun isAvailable(): Boolean = isLoaded.get()

    override fun engineName(): String = "M2M100 ONNX INT8 (Mundari NMT)"

    override fun supportedPairs(): List<Pair<String, String>> = listOf(
        Pair("hin_Deva", "unr_Deva"),
        Pair("unr_Deva", "hin_Deva")
    )

    override fun release() {
        try {
            encoderSession?.close()
            decoderSession?.close()
            decoderWithPastSession?.close()
            ortEnv?.close()
            isLoaded.set(false)
        } catch (_: Exception) {}
    }

    private fun extractAndClose(tensor: OnnxTensor): FloatArray {
        try {
            val buf = tensor.floatBuffer
            buf.rewind()
            val arr = FloatArray(buf.remaining())
            buf.get(arr)
            return arr
        } finally {
            try { tensor.close() } catch (_: Exception) {}
        }
    }

    private fun argmaxFromTensorAndClose(tensor: OnnxTensor, offset: Int, length: Int): Int {
        try {
            val buf = tensor.floatBuffer
            buf.rewind()
            var maxIdx = 0
            var maxVal = Float.NEGATIVE_INFINITY
            for (i in 0 until length) {
                val v = buf.get(offset + i)
                if (v > maxVal) {
                    maxVal = v
                    maxIdx = i
                }
            }
            return maxIdx
        } finally {
            try { tensor.close() } catch (_: Exception) {}
        }
    }

    private data class LayerKeyValue(
        val decKey: FloatArray,
        val decVal: FloatArray,
        val decSeqLen: Long
    )
}
