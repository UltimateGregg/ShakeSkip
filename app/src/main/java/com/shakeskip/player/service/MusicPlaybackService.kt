package com.shakeskip.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val binder = PlaybackBinder()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private var currentQueue: List<Song> = emptyList()
    private var skipSimulationJob: Job? = null
    private var userVolume: Float = 1f

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_playback_channel"
        private const val CHANNEL_NAME = "Music Playback"
        const val ACTION_BIND_LOCAL = "com.shakeskip.player.service.MusicPlaybackService.BIND"
    }

    inner class PlaybackBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                exoPlayer.addListener(playerListener)
            }

        userVolume = player.volume
        setPlayerVolume(userVolume)
        mediaSession = MediaSession.Builder(this, player)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onBind(intent: Intent?): IBinder? {
        return if (intent?.action == ACTION_BIND_LOCAL) {
            binder
        } else {
            super.onBind(intent)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startForeground(NOTIFICATION_ID, createNotification())
            } else {
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _currentPosition.value = player.currentPosition
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.localConfiguration?.tag
                ?.let { tag -> tag as? Song }
                ?.also { song -> _currentSong.value = song }
        }
    }

    fun playSong(song: Song) {
        currentQueue = listOf(song)
        val mediaItem = song.toMediaItem()
        player.setMediaItem(mediaItem)
        player.prepare()
        setPlayerVolume(userVolume)
        player.play()
        _currentSong.value = song
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return

        currentQueue = songs
        player.setMediaItems(
            songs.map { it.toMediaItem() },
            startIndex.coerceIn(songs.indices),
            0
        )
        player.prepare()
        setPlayerVolume(userVolume)
        player.play()

        if (startIndex in songs.indices) {
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
        when {
            player.hasNextMediaItem() -> player.seekToNext()
            currentQueue.size > 1 -> playSongs(currentQueue, 0)
        }
    }

    fun skipToPrevious() {
        when {
            player.hasPreviousMediaItem() -> player.seekToPrevious()
            currentQueue.size > 1 -> playSongs(currentQueue, currentQueue.lastIndex)
        }
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun getCurrentPosition(): Long = player.currentPosition

    fun getDuration(): Long = player.duration

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        skipSimulationJob?.cancel()
        userVolume = clamped
        setPlayerVolume(clamped)
    }

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

    fun simulateCdSkip() {
        if (!::player.isInitialized) return

        skipSimulationJob?.cancel()
        setPlayerVolume(userVolume)

        val baselineVolume = userVolume
        val job = serviceScope.launch {
            val wasPlaying = player.isPlaying

            if (wasPlaying) {
                player.pause()
            }

            setPlayerVolume(0f)

            val safeDuration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: Long.MAX_VALUE
            val currentPosition = player.currentPosition
            val skipForward = Random.nextLong(200, 520)
            val targetPosition = if (safeDuration == Long.MAX_VALUE) {
                currentPosition + skipForward
            } else {
                min(currentPosition + skipForward, safeDuration)
            }

            delay(220)
            player.seekTo(targetPosition)

            if (wasPlaying) {
                player.play()
            }

            val rampVolumes = listOf(
                0f,
                baselineVolume * 0.35f,
                baselineVolume * 0.7f,
                baselineVolume
            )

            rampVolumes.forEachIndexed { index, volumeLevel ->
                if (index != 0) {
                    delay(120)
                }
                setPlayerVolume(volumeLevel)
            }
        }

        job.invokeOnCompletion {
            setPlayerVolume(userVolume)
        }

        skipSimulationJob = job
    }

    override fun onDestroy() {
        skipSimulationJob?.cancel()
        mediaSession.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun setPlayerVolume(volume: Float) {
        player.volume = volume
        _volume.value = volume
    }

    private fun Song.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(filePath)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .build()
            )
            .setTag(this)
            .build()
    }
}
