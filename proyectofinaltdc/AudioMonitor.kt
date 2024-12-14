package com.example.proyectofinaltdc

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.math.log10

class AudioMonitor(private val sampleRate: Int = 44100) {
    private var audioRecord: AudioRecord? = null
    var isRecording = false
    var onDecibelRead: ((Double) -> Unit)? = null

    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    fun startRecording(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Permission to record audio not granted")
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord?.startRecording()
        isRecording = true
        Thread {
            while (isRecording) {
                val audioData = ShortArray(bufferSize)
                val readSize = audioRecord?.read(audioData, 0, bufferSize) ?: 0
                val decibelLevel = calculateDecibelLevel(audioData, readSize)
                onDecibelRead?.invoke(decibelLevel)
            }
        }.start()
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun calculateDecibelLevel(buffer: ShortArray, readSize: Int): Double {
        var sum = 0.0
        if (readSize > 0) {
            for (i in 0 until readSize) {
                sum += (buffer[i] * buffer[i])
            }
            return 10 * log10(sum / readSize)
        }
        return 0.0
    }
}

