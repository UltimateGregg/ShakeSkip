package com.shakeskip.player.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing shake detection settings
 */
data class ShakeSettings(
    val isEnabled: Boolean = true,
    val sensitivity: Float = 15f, // m/s² threshold
    val hapticFeedbackEnabled: Boolean = true
)

/**
 * Extension property for DataStore
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shake_settings")

/**
 * Manager for shake detection preferences
 */
@Singleton
class ShakePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.dataStore
    
    companion object {
        private val KEY_SHAKE_ENABLED = booleanPreferencesKey("shake_enabled")
        private val KEY_SHAKE_SENSITIVITY = floatPreferencesKey("shake_sensitivity")
        private val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
    }
    
    /**
     * Flow of shake settings
     */
    val shakeSettings: Flow<ShakeSettings> = dataStore.data.map { preferences ->
        ShakeSettings(
            isEnabled = preferences[KEY_SHAKE_ENABLED] ?: true,
            sensitivity = preferences[KEY_SHAKE_SENSITIVITY] ?: 15f,
            hapticFeedbackEnabled = preferences[KEY_HAPTIC_FEEDBACK] ?: true
        )
    }
    
    /**
     * Enable or disable shake detection
     */
    suspend fun setShakeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHAKE_ENABLED] = enabled
        }
    }
    
    /**
     * Update shake sensitivity threshold
     * @param sensitivity Value between 10-25 m/s²
     */
    suspend fun setShakeSensitivity(sensitivity: Float) {
        val clampedValue = sensitivity.coerceIn(10f, 25f)
        dataStore.edit { preferences ->
            preferences[KEY_SHAKE_SENSITIVITY] = clampedValue
        }
    }
    
    /**
     * Enable or disable haptic feedback
     */
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HAPTIC_FEEDBACK] = enabled
        }
    }
}