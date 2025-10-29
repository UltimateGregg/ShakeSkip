package com.shakeskip.player.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.SystemClock
import kotlin.math.sqrt

/**
 * Listens for accelerometer updates and invokes [onShakeDetected] when a shake gesture is detected.
 */
class ShakeDetector(
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private var shakeThreshold = DEFAULT_SHAKE_THRESHOLD
    private val gravity = FloatArray(3)
    private val linearAcceleration = FloatArray(3)
    private var lastShakeTimestamp = 0L

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        applyFilters(event.values)

        val magnitude = calculateMagnitude(linearAcceleration)
        if (magnitude < shakeThreshold) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastShakeTimestamp < DEBOUNCE_INTERVAL_MS) {
            return
        }

        lastShakeTimestamp = now
        onShakeDetected.invoke()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op: shake detection does not depend on accuracy changes.
    }

    fun setShakeThreshold(threshold: Float) {
        shakeThreshold = threshold.coerceIn(MIN_SHAKE_THRESHOLD, MAX_SHAKE_THRESHOLD)
    }

    fun reset() {
        gravity.fill(0f)
        linearAcceleration.fill(0f)
        lastShakeTimestamp = 0L
    }

    private fun applyFilters(values: FloatArray) {
        for (i in 0..2) {
            gravity[i] = GRAVITY_FILTER_ALPHA * gravity[i] + (1 - GRAVITY_FILTER_ALPHA) * values[i]
            linearAcceleration[i] = values[i] - gravity[i]
        }
    }

    private fun calculateMagnitude(values: FloatArray): Float {
        return sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
    }

    companion object {
        const val DEFAULT_SHAKE_THRESHOLD = 15f
        const val MIN_SHAKE_THRESHOLD = 10f
        const val MAX_SHAKE_THRESHOLD = 25f
        private const val DEBOUNCE_INTERVAL_MS = 500
        private const val GRAVITY_FILTER_ALPHA = 0.8f
    }
}
