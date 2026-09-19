package com.palash.voicebridge.ml.translation

import android.content.Context
import android.os.Environment
import android.util.Log
import com.palash.voicebridge.domain.engine.TranslationEngine
import com.palash.voicebridge.domain.model.TranslationResult
import java.io.File

class CompositeTranslationEngine(
    private val context: Context,
    private val santaliEngine: TranslationEngine
) : TranslationEngine {

    private val TAG = "CompositeNMT"

    var modelStatusMessage: String = ""
        private set

    val mundariEngine: MundariTranslationEngine by lazy {
        val extFiles = context.getExternalFilesDir(null) ?: context.filesDir
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val searchPaths = listOf(
            File(extFiles, "models/onnx_int8"),
            File(extFiles, "model/onnx_int8"),
            File(extFiles, "models/int8"),
            File(extFiles, "model/int8"),
            File(extFiles, "models"),
            File(extFiles, "model"),
            File(extFiles, "onnx_int8"),
            File(extFiles, "int8"),
            extFiles,
            File("/storage/emulated/0/Android/data/com.palash.voicebridge/files/models/onnx_int8"),
            File("/storage/emulated/0/Android/data/com.palash.voicebridge/files/model/onnx_int8"),
            File("/storage/emulated/0/Android/data/com.palash.voicebridge/files/models"),
            File("/storage/emulated/0/Android/data/com.palash.voicebridge/files/model"),
            File("/storage/emulated/0/Android/data/com.palash.voicebridge/files"),
            File(downloadDir, "onnx_int8"),
            File(downloadDir, "int8"),
            File(downloadDir, "models"),
            File(downloadDir, "model"),
            downloadDir,
            File("/sdcard/Download/onnx_int8"),
            File("/sdcard/Download/int8"),
            File("/sdcard/Download")
        )

        var selectedDir: File? = null

        fun isModelDir(dir: File): Boolean {
            if (!dir.exists() || !dir.isDirectory) return false
            val vFile = File(dir, "vocab.json")
            val eFile = File(dir, "encoder_model.onnx")
            val dFile = File(dir, "decoder_model.onnx")
            val dpFile = File(dir, "decoder_with_past_model.onnx")
            return vFile.exists() && eFile.exists() && dFile.exists() && dpFile.exists()
        }

        for (path in searchPaths) {
            if (isModelDir(path)) {
                selectedDir = path
                Log.d(TAG, "Found Mundari INT8 ONNX Model directory at: " + path.absolutePath)
                break
            }
        }

        val targetDir = selectedDir ?: File(extFiles, "models/onnx_int8")

        if (selectedDir != null) {
            modelStatusMessage = "Mundari Engine Ready: Loaded from " + targetDir.absolutePath
            Log.d(TAG, modelStatusMessage)
        } else {
            modelStatusMessage = "Mundari Model Not Found in app private storage or Downloads"
            Log.e(TAG, modelStatusMessage)
        }

        Log.d(TAG, "Initializing MundariTranslationEngine with dir: " + targetDir.absolutePath)
        MundariTranslationEngine(targetDir)
    }

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        Log.d(TAG, "translate() called for targetLanguage: " + targetLanguage + ", text: '" + text + "'")
        return if (targetLanguage.startsWith("unr")) {
            val res = mundariEngine.translate(text, sourceLanguage, targetLanguage)
            Log.d(TAG, "Mundari Engine Result: '" + res.translatedText + "', latency: " + res.latencyMs + "ms")
            res
        } else {
            santaliEngine.translate(text, sourceLanguage, targetLanguage)
        }
    }

    override fun isAvailable(): Boolean = santaliEngine.isAvailable() || mundariEngine.isAvailable()

    override fun engineName(): String = "Composite NMT (Santali + Mundari)"

    override fun supportedPairs(): List<Pair<String, String>> = listOf(
        Pair("hin_Deva", "sat_Olck"),
        Pair("hin_Deva", "unr_Deva"),
        Pair("unr_Deva", "hin_Deva")
    )

    override fun release() {
        santaliEngine.release()
        mundariEngine.release()
    }
}
