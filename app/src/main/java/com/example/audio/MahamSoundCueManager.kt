package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates crisp, pleasant real-time sound cues (activation chime, thinking tone, etc.)
 * directly using native PCM AudioTrack. Ensures audible feedback in all environments.
 */
class MahamSoundCueManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun ensureAudibleVolume() {
        try {
            audioManager?.let { am ->
                val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (currentVol < maxVol * 0.4f) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.75f).toInt(), 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cheerful melodic activation chime (Maham awake tone: C6 -> E6 -> G6)
     */
    fun playActivationChime() {
        scope.launch(Dispatchers.Default) {
            ensureAudibleVolume()
            playChordPcm(listOf(1046.50, 1318.51, 1567.98), durationMs = 180)
        }
    }

    /**
     * Subtle listening ready cue
     */
    fun playListeningStartCue() {
        scope.launch(Dispatchers.Default) {
            ensureAudibleVolume()
            playTonePcm(frequency = 880.0, durationMs = 100)
        }
    }

    private fun playTonePcm(frequency: Double, durationMs: Int) {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                // Sine wave with soft envelope fade-out to prevent clicks
                val envelope = when {
                    i < sampleRate * 0.01 -> i / (sampleRate * 0.01)
                    i > numSamples - sampleRate * 0.02 -> (numSamples - i) / (sampleRate * 0.02)
                    else -> 1.0
                }
                val angle = 2.0 * PI * frequency * time
                samples[i] = (sin(angle) * Short.MAX_VALUE * 0.65 * envelope).toInt().toShort()
            }

            playAudioTrack(samples, sampleRate)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playChordPcm(frequencies: List<Double>, durationMs: Int) {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                val envelope = when {
                    i < sampleRate * 0.015 -> i / (sampleRate * 0.015)
                    i > numSamples - sampleRate * 0.03 -> (numSamples - i) / (sampleRate * 0.03)
                    else -> 1.0
                }

                var mixed = 0.0
                for (freq in frequencies) {
                    mixed += sin(2.0 * PI * freq * time)
                }
                mixed /= frequencies.size

                samples[i] = (mixed * Short.MAX_VALUE * 0.75 * envelope).toInt().toShort()
            }

            playAudioTrack(samples, sampleRate)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playAudioTrack(samples: ShortArray, sampleRate: Int) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, samples.size * 2)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()

        // Release after playback completed
        scope.launch(Dispatchers.Default) {
            val delayMs = (samples.size.toDouble() / sampleRate * 1000).toLong() + 50
            kotlinx.coroutines.delay(delayMs)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
