package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Short vibration patterns. Uses the Vibrator service directly: Compose's
 * haptic feedback is silently skipped when "touch vibration" is off in the
 * phone settings, which is the default on many devices.
 */
object Haptics {
    private fun vibrator(context: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private fun play(context: Context, timings: LongArray, amplitudes: IntArray) {
        val v = vibrator(context) ?: return
        if (!v.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(timings, -1)
            }
        }
    }

    /** Two quick taps: operation saved. */
    fun success(context: Context) = play(context, longArrayOf(0, 35, 70, 55), intArrayOf(0, 180, 0, 255))

    /** One firm pulse: warning / problem. */
    fun warning(context: Context) = play(context, longArrayOf(0, 160), intArrayOf(0, 255))

    /** Light tick: long press, drag start. */
    fun tick(context: Context) = play(context, longArrayOf(0, 18), intArrayOf(0, 160))
}
