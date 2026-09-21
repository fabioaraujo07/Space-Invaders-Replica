package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Sintetizador sonoro em tempo real e gestor háptico fiel aos efeitos de hardware analógico de 1978.
 * Não requer ficheiros .wav/.mp3 externos: gera ondas quadradas e ruído diretamente via AudioTrack PCM 16-bit.
 */
class RetroSoundManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val sampleRate = 22050

    // Os 4 tons descendentes icónicos dos passos da marcha alien (Notas clássicas aproximadas)
    private val MARCH_FREQUENCIES = doubleArrayOf(98.0, 92.5, 87.3, 82.4) // G2, F#2, F2, E2

    // AudioTracks pré-sintetizados para latência ultra-baixa (zero lag na marcha)
    private val marchTracks = Array(4) { idx ->
        createToneTrack(MARCH_FREQUENCIES[idx], durationMs = 60, volume = 0.45f)
    }

    private val playerShootTrack = createShootTrack()
    private val invaderExplodeTrack = createExplosionTrack(durationMs = 90, volume = 0.5f)
    private val playerExplodeTrack = createExplosionTrack(durationMs = 280, volume = 0.7f)
    private val ufoTrack = createUfoTrack()

    private fun createToneTrack(frequencyHz: Double, durationMs: Int, volume: Float): AudioTrack {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        val period = sampleRate / frequencyHz

        for (i in 0 until numSamples) {
            // Onda quadrada suave com envelope linear
            val phase = (i % period) / period
            val raw = if (phase < 0.5) 1.0 else -1.0
            val env = 1.0 - (i.toDouble() / numSamples) // Fade out
            buffer[i] = (raw * Short.MAX_VALUE * volume * env).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
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
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        return track
    }

    private fun createShootTrack(): AudioTrack {
        // Sweep descendente de frequência rápida (típico do canhão laser de 1978)
        val durationMs = 120
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)

        var currentPhase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 1200.0 - (progress * 850.0) // 1200Hz desce até 350Hz
            currentPhase += 2.0 * PI * freq / sampleRate
            val raw = sin(currentPhase)
            val env = 1.0 - progress
            buffer[i] = (raw * Short.MAX_VALUE * 0.4f * env).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        return track
    }

    private fun createExplosionTrack(durationMs: Int, volume: Float): AudioTrack {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        var lastNoise = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            // Ruído branco com filtro passa-baixo suave para simular a explosão arcade clássica
            val whiteNoise = (Math.random() * 2.0 - 1.0)
            lastNoise = (lastNoise * 0.8) + (whiteNoise * 0.2)
            val env = (1.0 - progress) * (1.0 - progress)
            buffer[i] = (lastNoise * Short.MAX_VALUE * volume * env).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        return track
    }

    private fun createUfoTrack(): AudioTrack {
        val durationMs = 180
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            // Warble oscilatório suave
            val freq = 420.0 + (sin(progress * 20.0) * 80.0)
            phase += 2.0 * PI * freq / sampleRate
            val sample = sin(phase)
            buffer[i] = (sample * Short.MAX_VALUE * 0.35f).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        return track
    }

    private fun playTrack(track: AudioTrack) {
        try {
            track.stop()
            track.reloadStaticData()
            track.play()
        } catch (_: Exception) {
            // Ignora se estiver a decorrer libertação
        }
    }

    fun playMarchBeat(beatIndex: Int) {
        val idx = beatIndex.coerceIn(0, 3)
        playTrack(marchTracks[idx])
    }

    fun playPlayerShoot() {
        playTrack(playerShootTrack)
    }

    fun playInvaderExplosion() {
        playTrack(invaderExplodeTrack)
        vibrate(durationMs = 25, amplitude = 90)
    }

    fun playPlayerExplosion() {
        playTrack(playerExplodeTrack)
        vibrate(durationMs = 180, amplitude = 220)
    }

    fun playUfoSound() {
        playTrack(ufoTrack)
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // Ignora se não houver hardware de vibração
        }
    }

    fun release() {
        marchTracks.forEach { it.release() }
        playerShootTrack.release()
        invaderExplodeTrack.release()
        playerExplodeTrack.release()
        ufoTrack.release()
    }
}
