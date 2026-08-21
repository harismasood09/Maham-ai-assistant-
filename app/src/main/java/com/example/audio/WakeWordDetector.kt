package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

class WakeWordDetector(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onWakeWordDetected: (phrase: String) -> Unit
) {

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private var audioRecordJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizerActive = false

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startListening() {
        if (_isListening.value) return
        _isListening.value = true

        startEnergyMonitoring()
    }

    fun stopListening() {
        _isListening.value = false
        audioRecordJob?.cancel()
        audioRecordJob = null
        stopRecognizer()
    }

    private fun startEnergyMonitoring() {
        audioRecordJob?.cancel()
        audioRecordJob = scope.launch(Dispatchers.IO) {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufferSize <= 0) return@launch

            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufferSize * 2
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord.release()
                    return@launch
                }

                audioRecord.startRecording()
                val buffer = ShortArray(minBufferSize)

                while (isActive && _isListening.value) {
                    val readCount = audioRecord.read(buffer, 0, buffer.size)
                    if (readCount > 0) {
                        // Calculate RMS energy
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / readCount)
                        val db = if (rms > 0) 20 * kotlin.math.log10(rms) else 0.0

                        // Voice activity detected above threshold (whisper/speech energy > 48dB)
                        if (db > 48 && !isRecognizerActive) {
                            // Launch quick keyword spotter
                            scope.launch(Dispatchers.Main) {
                                triggerSpotter()
                            }
                            delay(2500) // Debounce
                        }
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun triggerSpotter() {
        if (isRecognizerActive) return
        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isRecognizerActive = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isRecognizerActive = false
                }
                override fun onError(error: Int) {
                    isRecognizerActive = false
                }
                override fun onResults(results: Bundle?) {
                    isRecognizerActive = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        for (text in matches) {
                            val lower = text.lowercase(Locale.ROOT)
                            if (isWakeWordMatch(lower)) {
                                onWakeWordDetected(text)
                                return
                            }
                        }
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        for (text in matches) {
                            val lower = text.lowercase(Locale.ROOT)
                            if (isWakeWordMatch(lower)) {
                                onWakeWordDetected(text)
                                stopRecognizer()
                                return
                            }
                        }
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            isRecognizerActive = false
        }
    }

    private fun isWakeWordMatch(text: String): Boolean {
        return text.contains("maham") ||
                text.contains("ماہم") ||
                text.contains("meham") ||
                text.contains("mahm") ||
                text.contains("hey maham") ||
                text.contains("سنو ماہم") ||
                text.contains("ماهام")
    }

    private fun stopRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        speechRecognizer = null
        isRecognizerActive = false
    }

    fun shutdown() {
        stopListening()
    }
}
