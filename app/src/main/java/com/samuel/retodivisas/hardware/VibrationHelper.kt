package com.samuel.retodivisas.hardware

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class VibrationHelper(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun hasVibrator(): Boolean {
        return vibrator?.hasVibrator() == true
    }

    fun vibrateClick(milliseconds: Long = 50) {
        vibrateOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
    }

    fun vibrateSuccess() {
        val timings = longArrayOf(0, 80, 60, 100)
        val amplitudes = intArrayOf(0, 150, 0, 255)
        vibrateWaveform(timings, amplitudes, -1)
    }

    fun vibrateError() {
        val timings = longArrayOf(0, 100, 50, 100, 50, 150)
        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
        vibrateWaveform(timings, amplitudes, -1)
    }

    fun vibrateOneShot(durationMs: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val effect = VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
        v.vibrate(effect)
    }

    fun vibrateWaveform(timings: LongArray, amplitudes: IntArray, repeat: Int = -1) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val effect = VibrationEffect.createWaveform(timings, amplitudes, repeat)
        v.vibrate(effect)
    }

    fun cancel() {
        vibrator?.cancel()
    }
}
