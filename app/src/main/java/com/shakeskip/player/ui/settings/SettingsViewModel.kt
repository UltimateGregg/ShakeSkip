package com.shakeskip.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shakeskip.player.data.preferences.ShakePreferencesManager
import com.shakeskip.player.data.preferences.ShakeSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing app settings
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val shakePreferencesManager: ShakePreferencesManager
) : ViewModel() {
    
    /**
     * Current shake settings state
     */
    val shakeSettings: StateFlow<ShakeSettings> = shakePreferencesManager.shakeSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ShakeSettings()
        )
    
    /**
     * Enable or disable shake detection
     */
    fun setShakeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            shakePreferencesManager.setShakeEnabled(enabled)
        }
    }
    
    /**
     * Update shake sensitivity
     * @param sensitivity Value between 10-25 m/s²
     */
    fun setShakeSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            shakePreferencesManager.setShakeSensitivity(sensitivity)
        }
    }
    
    /**
     * Enable or disable haptic feedback
     */
    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            shakePreferencesManager.setHapticFeedbackEnabled(enabled)
        }
    }
}