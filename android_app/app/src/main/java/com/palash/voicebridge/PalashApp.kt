package com.palash.voicebridge

import android.app.Application
import com.palash.voicebridge.data.content.ContentLoader
import com.palash.voicebridge.data.local.AppDatabase
import com.palash.voicebridge.data.repository.AudioRepository
import com.palash.voicebridge.data.repository.ContentRepository
import com.palash.voicebridge.data.repository.TranslationRepository
import com.palash.voicebridge.ml.ModelManager
import com.palash.voicebridge.ml.asr.IndicConformerAsrEngine
import com.palash.voicebridge.ml.translation.IndicTrans2TranslationEngine
import com.palash.voicebridge.ml.translation.CompositeTranslationEngine
import com.palash.voicebridge.ml.tts.OlChikiTtsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class for PALASH VoiceBridge.
 * Initializes database, content loader, repositories, and ML engines.
 */
class PalashApp : Application() {

    // Coroutine scope for app-level background work
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Database
    lateinit var database: AppDatabase
        private set

    // Repositories
    lateinit var contentRepository: ContentRepository
        private set
    lateinit var translationRepository: TranslationRepository
        private set
    lateinit var audioRepository: AudioRepository
        private set

    // ML
    lateinit var modelManager: ModelManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize database
        database = AppDatabase.getInstance(this)

        // Initialize ML engines for 100% offline multimodal pipeline:
        // ASR: AI4Bharat IndicConformer (Hindi)
        // Translation: AI4Bharat IndicTrans2 (Hindi -> Santali Ol Chiki)
        // TTS: Offline Santali Ol Chiki Synthesizer
        val baseTranslationEngine = IndicTrans2TranslationEngine(this)
        val translationEngine = CompositeTranslationEngine(this, baseTranslationEngine)
        val asrEngine = IndicConformerAsrEngine(this)
        val ttsEngine = OlChikiTtsEngine(this)

        modelManager = ModelManager(translationEngine, asrEngine, ttsEngine)

        // Initialize repositories
        contentRepository = ContentRepository(
            database.lessonDao(),
            database.flashcardDao(),
            database.vocabularyDao()
        )

        translationRepository = TranslationRepository(
            database.phraseDao(),
            translationEngine
        )

        audioRepository = AudioRepository(this, ttsEngine)

        // Load content from JSON assets on first launch
        appScope.launch {
            val contentLoader = ContentLoader(this@PalashApp, database)
            contentLoader.loadContentIfNeeded()
        }
    }
}
