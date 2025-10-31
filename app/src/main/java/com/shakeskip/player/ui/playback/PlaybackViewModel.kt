package com.shakeskip.player.ui.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.Job
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

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()
    
    private val _shakeSettings = MutableStateFlow(ShakeSettings())
    val shakeSettings: StateFlow<ShakeSettings> = _shakeSettings.asStateFlow()
    
    private val _isShakeDetectionActive = MutableStateFlow(false)
    val isShakeDetectionActive: StateFlow<Boolean> = _isShakeDetectionActive.asStateFlow()

    private val _isDeviceShaking = MutableStateFlow(false)
    val isDeviceShaking: StateFlow<Boolean> = _isDeviceShaking.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoadingSongs = MutableStateFlow(false)
    val isLoadingSongs: StateFlow<Boolean> = _isLoadingSongs.asStateFlow()

    private val _songErrorMessage = MutableStateFlow<String?>(null)
    val songErrorMessage: StateFlow<String?> = _songErrorMessage.asStateFlow()
    
    private var isPlaybackServiceBound = false
    private var isShakeServiceBound = false
    private val playbackServiceJobs = mutableListOf<Job>()
    private val shakeServiceJobs = mutableListOf<Job>()
    
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
            val binder = service as? MusicPlaybackService.PlaybackBinder
            val boundService = binder?.getService()

            if (boundService == null) {
                Log.w(TAG, "Failed to obtain playback service binder")
                return
            }

            playbackService = boundService
            isPlaybackServiceBound = true
            Log.d(TAG, "Playback service connected")
            observePlaybackService(boundService)
            boundService.setVolume(_volume.value)
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Playback service disconnected")
            playbackServiceJobs.forEach { it.cancel() }
            playbackServiceJobs.clear()
            playbackService = null
            isPlaybackServiceBound = false
            _isPlaying.value = false
            _volume.value = 1f
        }
    }

    private fun observePlaybackService(service: MusicPlaybackService) {
        playbackServiceJobs.forEach { it.cancel() }
        playbackServiceJobs.clear()

        playbackServiceJobs += viewModelScope.launch {
            service.currentSong.collect { song ->
                _currentSong.value = song
            }
        }

        playbackServiceJobs += viewModelScope.launch {
            service.isPlaying.collect { playing ->
                _isPlaying.value = playing
            }
        }

        playbackServiceJobs += viewModelScope.launch {
            service.currentPosition.collect { position ->
                _currentPosition.value = position
            }
        }

        playbackServiceJobs += viewModelScope.launch {
            service.volume.collect { level ->
                _volume.value = level
            }
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
                
                // Set up shake callback to trigger CD skip effect
                detectionService.setShakeCallback {
                    Log.d(TAG, "Shake detected - triggering CD skip effect")
                    handleShakeDetected()
                }
                
                // Apply current settings
                applyShakeSettingsToService(detectionService)
                
                // Monitor shake detection state
                shakeServiceJobs += viewModelScope.launch {
                    detectionService.isShakeDetectionEnabled.collect { enabled ->
                        _isShakeDetectionActive.value = enabled
                    }
                }

                shakeServiceJobs += viewModelScope.launch {
                    detectionService.isShakingNow.collect { isShaking ->
                        _isDeviceShaking.value = isShaking
                    }
                }
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Shake detection service disconnected")
            shakeServiceJobs.forEach { it.cancel() }
            shakeServiceJobs.clear()
            shakeDetectionService = null
            isShakeServiceBound = false
            _isShakeDetectionActive.value = false
            _isDeviceShaking.value = false
        }
    }

    private fun handleShakeDetected() {
        if (playbackService != null) {
            playbackService?.simulateCdSkip()
        } else {
            Log.w(TAG, "Shake detected but playback service is not bound")
        }
    }
    
    /**
     * Binds to both playback and shake detection services
     */
    fun bindServices() {
        // Start and bind to playback service
        val playbackStartIntent = Intent(context, MusicPlaybackService::class.java)
        ContextCompat.startForegroundService(context, playbackStartIntent)

        val playbackBindIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_BIND_LOCAL
        }

        val playbackBound = context.bindService(
            playbackBindIntent,
            playbackConnection,
            Context.BIND_AUTO_CREATE
        )

        if (!playbackBound) {
            Log.w(TAG, "Unable to bind to playback service")
        }

        // Bind to shake detection service
        val shakeIntent = Intent(context, ShakeDetectionService::class.java)
        val shakeBound = context.bindService(
            shakeIntent,
            shakeDetectionConnection,
            Context.BIND_AUTO_CREATE
        )

        if (!shakeBound) {
            Log.w(TAG, "Unable to bind to shake detection service")
        }
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
        val playlist = _songs.value
        val targetIndex = playlist.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0

        if (playbackService != null) {
            if (playlist.isNotEmpty()) {
                playbackService?.playSongs(playlist, targetIndex)
            } else {
                playbackService?.playSong(song)
            }
        } else {
            _currentSong.value = song
            _isPlaying.value = true
        }
        
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
        
        if (playbackService != null) {
            playbackService?.playSongs(songs, startIndex)
        } else {
            val index = startIndex.coerceIn(songs.indices)
            _currentSong.value = songs.getOrNull(index)
            _isPlaying.value = true
        }
        
        // Start shake detection if enabled
        if (_shakeSettings.value.isEnabled) {
            shakeDetectionService?.startShakeDetection()
        }
    }
    
    /**
     * Toggles play/pause
     */
    fun togglePlayPause() {
        if (playbackService != null) {
            playbackService?.togglePlayPause()
        } else {
            _isPlaying.value = !_isPlaying.value
        }

        if (_shakeSettings.value.isEnabled) {
            shakeDetectionService?.startShakeDetection()
        }
    }
    
    /**
     * Skips to next track (triggered by shake or manual control)
     */
    fun skipToNext() {
        Log.d(TAG, "Skipping to next track")
        if (playbackService != null) {
            playbackService?.skipToNext()
        } else {
            val playlist = _songs.value
            if (playlist.isEmpty()) return
            val currentIndex = playlist.indexOfFirst { it.id == _currentSong.value?.id }
            val nextIndex = if (currentIndex in playlist.indices) {
                (currentIndex + 1) % playlist.size
            } else {
                0
            }
            _currentSong.value = playlist[nextIndex]
        }
    }
    
    /**
     * Skips to previous track
     */
    fun skipToPrevious() {
        Log.d(TAG, "Skipping to previous track")
        if (playbackService != null) {
            playbackService?.skipToPrevious()
        } else {
            val playlist = _songs.value
            if (playlist.isEmpty()) return
            val currentIndex = playlist.indexOfFirst { it.id == _currentSong.value?.id }
            val previousIndex = if (currentIndex in playlist.indices) {
                if (currentIndex - 1 < 0) playlist.lastIndex else currentIndex - 1
            } else {
                playlist.lastIndex
            }
            _currentSong.value = playlist[previousIndex]
        }
    }

    fun setVolume(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        if (playbackService != null) {
            playbackService?.setVolume(clamped)
        } else {
            _volume.value = clamped
        }
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
        shakeServiceJobs.forEach { it.cancel() }
        shakeServiceJobs.clear()
        playbackServiceJobs.forEach { it.cancel() }
        playbackServiceJobs.clear()
        
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
