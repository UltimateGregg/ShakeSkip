package com.shakeskip.player.ui.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shakeskip.player.data.model.Song
import com.shakeskip.player.data.preferences.ShakePreferencesManager
import com.shakeskip.player.data.preferences.ShakeSettings
import com.shakeskip.player.data.repository.SongRepository
import com.shakeskip.player.sensor.ShakeDetectionService
import com.shakeskip.player.service.MusicPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing playback and shake detection integration
 */
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shakePreferencesManager: ShakePreferencesManager,
    private val songRepository: SongRepository
) : ViewModel() {
    
    private var playbackService: MusicPlaybackService? = null
    private var shakeDetectionService: ShakeDetectionService? = null
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _shakeSettings = MutableStateFlow(ShakeSettings())
    val shakeSettings: StateFlow<ShakeSettings> = _shakeSettings.asStateFlow()
    
    private val _isShakeDetectionActive = MutableStateFlow(false)
    val isShakeDetectionActive: StateFlow<Boolean> = _isShakeDetectionActive.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoadingSongs = MutableStateFlow(false)
    val isLoadingSongs: StateFlow<Boolean> = _isLoadingSongs.asStateFlow()

    private val _songErrorMessage = MutableStateFlow<String?>(null)
    val songErrorMessage: StateFlow<String?> = _songErrorMessage.asStateFlow()
    
    private var isPlaybackServiceBound = false
    private var isShakeServiceBound = false
    
    companion object {
        private const val TAG = "PlaybackViewModel"
    }
    
    init {
        // Load shake settings
        viewModelScope.launch {
            shakePreferencesManager.shakeSettings.collect { settings ->
                _shakeSettings.value = settings
                applyShakeSettingsToService()
            }
        }
    }

    private fun applyShakeSettingsToService(service: ShakeDetectionService? = shakeDetectionService) {
        val detectionService = service ?: return
        val settings = _shakeSettings.value

        detectionService.setShakeThreshold(settings.sensitivity)

        if (settings.isEnabled) {
            detectionService.startShakeDetection()
        } else {
            detectionService.stopShakeDetection()
        }
    }
    
    /**
     * Service connection for MusicPlaybackService
     */
    private val playbackConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // This would be implemented if we made MusicPlaybackService bindable
            Log.d(TAG, "Playback service connected")
            isPlaybackServiceBound = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isPlaybackServiceBound = false
            Log.d(TAG, "Playback service disconnected")
        }
    }
    
    /**
     * Service connection for ShakeDetectionService
     */
    private val shakeDetectionConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? ShakeDetectionService.ShakeDetectionBinder
            shakeDetectionService = binder?.getService()
            
            shakeDetectionService?.let { detectionService ->
                Log.d(TAG, "Shake detection service connected")
                isShakeServiceBound = true
                
                // Set up shake callback to skip to next track
                detectionService.setShakeCallback {
                    Log.d(TAG, "Shake detected - skipping to next track")
                    skipToNext()
                }
                
                // Apply current settings
                applyShakeSettingsToService(detectionService)
                
                // Monitor shake detection state
                viewModelScope.launch {
                    detectionService.isShakeDetectionEnabled.collect { enabled ->
                        _isShakeDetectionActive.value = enabled
                    }
                }
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            shakeDetectionService = null
            isShakeServiceBound = false
            _isShakeDetectionActive.value = false
            Log.d(TAG, "Shake detection service disconnected")
        }
    }
    
    /**
     * Binds to both playback and shake detection services
     */
    fun bindServices() {
        // Bind to shake detection service
        val shakeIntent = Intent(context, ShakeDetectionService::class.java)
        context.bindService(shakeIntent, shakeDetectionConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Loads songs from device storage. Requires the caller to have already requested permission.
     */
    fun loadSongs(forceRefresh: Boolean = false) {
        if (_isLoadingSongs.value) return
        if (_songs.value.isNotEmpty() && !forceRefresh) return

        viewModelScope.launch {
            _isLoadingSongs.value = true
            _songErrorMessage.value = null
            try {
                val loaded = songRepository.loadSongs()
                _songs.value = loaded

                if (_currentSong.value == null && loaded.isNotEmpty()) {
                    _currentSong.value = loaded.first()
                }
            } catch (security: SecurityException) {
                Log.w(TAG, "Audio permission missing", security)
                _songErrorMessage.value = "Permission required to access your music."
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load songs", t)
                _songErrorMessage.value = "Unable to load songs. Please try again."
            } finally {
                _isLoadingSongs.value = false
            }
        }
    }
    
    /**
     * Plays a song
     */
    fun playSong(song: Song) {
        // This would interact with the playback service
        _currentSong.value = song
        _isPlaying.value = true
        
        // Start shake detection if enabled
        if (_shakeSettings.value.isEnabled) {
            shakeDetectionService?.startShakeDetection()
        }
    }

    /**
     * Updates current selection and starts playback.
     */
    fun selectSong(song: Song) {
        playSong(song)
    }
    
    /**
     * Plays a list of songs
     */
    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        
        _currentSong.value = songs.getOrNull(startIndex)
        _isPlaying.value = true
        
        // Start shake detection if enabled
        if (_shakeSettings.value.isEnabled) {
            shakeDetectionService?.startShakeDetection()
        }
    }
    
    /**
     * Toggles play/pause
     */
    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
        
        if (_shakeSettings.value.isEnabled) {
            shakeDetectionService?.startShakeDetection()
        }
    }
    
    /**
     * Skips to next track (triggered by shake or manual control)
     */
    fun skipToNext() {
        Log.d(TAG, "Skipping to next track")
        // This would call playbackService?.skipToNext()
        // For now, we'll just log it
    }
    
    /**
     * Skips to previous track
     */
    fun skipToPrevious() {
        Log.d(TAG, "Skipping to previous track")
        // This would call playbackService?.skipToPrevious()
    }
    
    /**
     * Updates shake sensitivity
     */
    fun updateShakeSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            shakePreferencesManager.setShakeSensitivity(sensitivity)
        }
    }
    
    /**
     * Toggles shake detection on/off
     */
    fun toggleShakeDetection() {
        viewModelScope.launch {
            val newState = !_shakeSettings.value.isEnabled
            shakePreferencesManager.setShakeEnabled(newState)
        }
    }
    
    /**
     * Gets shake statistics
     */
    fun getShakeCount(): Int {
        return shakeDetectionService?.shakeCount?.value ?: 0
    }
    
    override fun onCleared() {
        // Unbind services
        if (isShakeServiceBound) {
            context.unbindService(shakeDetectionConnection)
            isShakeServiceBound = false
        }
        
        if (isPlaybackServiceBound) {
            context.unbindService(playbackConnection)
            isPlaybackServiceBound = false
        }
        
        super.onCleared()
    }
}
