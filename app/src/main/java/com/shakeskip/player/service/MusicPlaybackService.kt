package com.shakeskip.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.shakeskip.player.MainActivity
import com.shakeskip.player.R
import com.shakeskip.player.data.model.Song
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {
    
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_playback_channel"
        private const val CHANNEL_NAME = "Music Playback"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()
        
        player = ExoPlayer.Builder(this)
            .build()
            .also {
                it.addListener(playerListener)
            }
        
        mediaSession = MediaSession.Builder(this, player)
            .build()
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }
    
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    // Handle track ended
                }
                Player.STATE_READY -> {
                    _currentPosition.value = player.currentPosition
                }
            }
        }
    }
    
    fun playSong(song: Song) {
        val mediaItem = MediaItem.fromUri(song.filePath)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        _currentSong.value = song
    }
    
    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val mediaItems = songs.map { MediaItem.fromUri(it.filePath) }
        player.setMediaItems(mediaItems, startIndex, 0)
        player.prepare()
        player.play()
        if (songs.isNotEmpty() && startIndex < songs.size) {
            _currentSong.value = songs[startIndex]
        }
    }
    
    fun play() {
        player.play()
    }
    
    fun pause() {
        player.pause()
    }
    
    fun togglePlayPause() {
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }
    
    fun skipToNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }
    
    fun skipToPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }
    
    fun seekTo(position: Long) {
        player.seekTo(position)
    }
    
    fun getCurrentPosition(): Long = player.currentPosition
    
    fun getDuration(): Long = player.duration
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music playback"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(_currentSong.value?.title ?: "ShakeSkip Player")
            .setContentText(_currentSong.value?.artist ?: "")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}