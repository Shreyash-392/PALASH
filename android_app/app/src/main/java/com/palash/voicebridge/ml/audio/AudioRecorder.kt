package com.palash.voicebridge.ml.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

/**
 * Offline Audio Recorder for Speech Recognition.
 *
 * Captures 16 kHz, 16-bit Mono PCM audio.
 * Includes:
 * 1. Real-time RMS audio amplitude level for UI visualization.
 * 2. Voice Activity Detection (VAD) with silence timeout for auto-stopping.
 */
class AudioRecorder {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SILENCE_THRESHOLD_RMS = 450.0 // Noise floor threshold
        private const val SILENCE_DURATION_MS = 1500L  // Auto-stop after 1.5s silence
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startRecording(
        scope: CoroutineScope,
        onSilenceDetected: () -> Unit = {},
        onBufferReady: (ByteArray) -> Unit
    ): Boolean {
        if (isRecording) return false

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(SAMPLE_RATE / 10)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val outputStream = ByteArrayOutputStream()
                val readBuffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteArray(bufferSize)

                var speechDetected = false
                var lastSpeechTimestamp = System.currentTimeMillis()

                while (isActive && isRecording) {
                    val readShorts = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                    if (readShorts > 0) {
                        // Calculate RMS amplitude for VAD and UI metering
                        var sum = 0.0
                        for (i in 0 until readShorts) {
                            val sample = readBuffer[i]
                            sum += (sample * sample).toDouble()

                            // Convert short to little-endian bytes
                            byteBuffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                        }

                        val rms = sqrt(sum / readShorts)
                        _amplitude.value = (rms / 8000.0).toFloat().coerceIn(0f, 1f)

                        outputStream.write(byteBuffer, 0, readShorts * 2)

                        val now = System.currentTimeMillis()
                        if (rms > SILENCE_THRESHOLD_RMS) {
                            speechDetected = true
                            lastSpeechTimestamp = now
                        } else if (speechDetected && (now - lastSpeechTimestamp > SILENCE_DURATION_MS)) {
                            // Silence timeout reached after speech
                            withContext(Dispatchers.Main) {
                                onSilenceDetected()
                            }
                            break
                        }
                    }
                }

                val finalPcm = outputStream.toByteArray()
                withContext(Dispatchers.Main) {
                    onBufferReady(finalPcm)
                }
            }

            return true
        } catch (e: Exception) {
            isRecording = false
            audioRecord?.release()
            audioRecord = null
            return false
        }
    }

    fun stopRecording(): ByteArray? {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            audioRecord = null
            _amplitude.value = 0f
        }
        return null
    }

    fun isRecording(): Boolean = isRecording
}
