package com.shakeskip.player.sensor

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service that manages shake detection for the music player.
 * Runs in the background and triggers callbacks when shake gestures are detected.
 */
@AndroidEntryPoint
class ShakeDetectionService : Service() {
    
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var shakeDetector: ShakeDetector
    private var vibrator: Vibrator? = null
    
    private val binder = ShakeDetectionBinder()
    
    private val _isShakeDetectionEnabled = MutableStateFlow(false)
    val isShakeDetectionEnabled: StateFlow<Boolean> = _isShakeDetectionEnabled.asStateFlow()
    
    private val _shakeCount = MutableStateFlow(0)
    val shakeCount: StateFlow<Int> = _shakeCount.asStateFlow()
    
    private var shakeCallback: (() -> Unit)? = null
    
    companion object {
        private const val TAG = "ShakeDetectionService"
        private const val SAMPLING_RATE = SensorManager.SENSOR_DELAY_GAME // ~50-100Hz
        private const val VIBRATION_DURATION_MS = 50L
    }
    
    inner class ShakeDetectionBinder : Binder() {
        fun getService(): ShakeDetectionService = this@ShakeDetectionService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ShakeDetectionService created")
        
        // Initialize sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        // Initialize vibrator
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        // Initialize shake detector
        shakeDetector = ShakeDetector(
            onShakeDetected = ::handleShakeDetected
        )
        
        if (accelerometer == null) {
            Log.e(TAG, "No accelerometer sensor available!")
        }
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    /**
     * Starts shake detection
     */
    fun startShakeDetection() {
        if (_isShakeDetectionEnabled.value) {
            Log.d(TAG, "Shake detection already enabled")
            return
        }
        
        accelerometer?.let { sensor ->
            val registered = sensorManager.registerListener(
                shakeDetector,
                sensor,
                SAMPLING_RATE
            )
            
            if (registered) {
                _isShakeDetectionEnabled.value = true
                Log.d(TAG, "Shake detection started")
            } else {
                Log.e(TAG, "Failed to register sensor listener")
            }
        } ?: run {
            Log.e(TAG, "Cannot start shake detection - no accelerometer available")
        }
    }
    
    /**
     * Stops shake detection
     */
    fun stopShakeDetection() {
        if (!_isShakeDetectionEnabled.value) {
            Log.d(TAG, "Shake detection already disabled")
            return
        }
        
        sensorManager.unregisterListener(shakeDetector)
        _isShakeDetectionEnabled.value = false
        Log.d(TAG, "Shake detection stopped")
    }
    
    /**
     * Sets the callback to be invoked when a shake is detected
     */
    fun setShakeCallback(callback: () -> Unit) {
        this.shakeCallback = callback
    }
    
    /**
     * Updates the shake sensitivity threshold
     * @param threshold Value between 10-25 m/s²
     */
    fun setShakeThreshold(threshold: Float) {
        shakeDetector.setShakeThreshold(threshold)
    }
    
    /**
     * Handles shake detection events
     */
    private fun handleShakeDetected() {
        Log.d(TAG, "Shake detected - triggering callback")
        
        // Increment shake count for statistics
        _shakeCount.value++
        
        // Provide haptic feedback
        provideHapticFeedback()
        
        // Trigger the callback
        shakeCallback?.invoke()
    }
    
    /**
     * Provides haptic feedback when shake is detected
     */
    private fun provideHapticFeedback() {
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(
                    VIBRATION_DURATION_MS,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
                it.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(VIBRATION_DURATION_MS)
            }
        }
    }
    
    /**
     * Resets the shake counter
     */
    fun resetShakeCount() {
        _shakeCount.value = 0
    }
    
    override fun onDestroy() {
        stopShakeDetection()
        shakeDetector.reset()
        super.onDestroy()
        Log.d(TAG, "ShakeDetectionService destroyed")
    }
}