package com.bismillah.tesfft

import org.jtransforms.fft.DoubleFFT_1D

object FFTUtils {
    fun applyNoiseReduction(audioBuffer: ByteArray): ByteArray {
        val sampleCount = audioBuffer.size / 2 // karena Short = 2 bytes

        // Konversi ByteArray ke ShortArray (PCM 16bit)
        val shortBuffer = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            shortBuffer[i] = ((audioBuffer[i * 2 + 1].toInt() shl 8) or (audioBuffer[i * 2].toInt() and 0xFF)).toShort()
        }

        // Convert ShortArray ke DoubleArray untuk FFT
        val real = DoubleArray(sampleCount)
        for (i in shortBuffer.indices) {
            real[i] = shortBuffer[i].toDouble()
        }

        // Lakukan FFT
        val fft = DoubleFFT_1D(sampleCount.toLong())
        fft.realForward(real)

        // Noise Reduction: Buang frekuensi kecil (threshold rendah)
        for (i in real.indices) {
            if (Math.abs(real[i]) < 1000) { // <-- threshold bisa kamu tweak
                real[i] = 0.0
            }
        }

        // Lakukan Inverse FFT
        fft.realInverse(real, true)

        // Convert DoubleArray ke ShortArray kembali
        val processedShorts = ShortArray(sampleCount)
        for (i in real.indices) {
            processedShorts[i] = real[i].toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        // Convert ShortArray ke ByteArray
        val output = ByteArray(sampleCount * 2)
        for (i in processedShorts.indices) {
            output[i * 2] = (processedShorts[i].toInt() and 0xFF).toByte()
            output[i * 2 + 1] = ((processedShorts[i].toInt() shr 8) and 0xFF).toByte()
        }

        return output
    }
}
