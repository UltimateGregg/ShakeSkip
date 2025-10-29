package com.shakeskip.player.ui.playback

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.shakeskip.player.data.model.Song
import com.shakeskip.player.ui.components.ShakeGestureHint
import com.shakeskip.player.ui.components.ShakeIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackScreen(
    onOpenSettings: () -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val shakeSettings by viewModel.shakeSettings.collectAsState()
    val isShakeDetectionActive by viewModel.isShakeDetectionActive.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val isLoadingSongs by viewModel.isLoadingSongs.collectAsState()
    val songError by viewModel.songErrorMessage.collectAsState()

    val context = LocalContext.current
    val audioPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.loadSongs(forceRefresh = true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.bindServices()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadSongs()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "ShakeSkip Player") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        PlaybackContent(
            paddingValues = innerPadding,
            song = currentSong,
            isPlaying = isPlaying,
            onTogglePlay = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipToNext() },
            onSkipPrevious = { viewModel.skipToPrevious() },
            onSongSelected = { viewModel.selectSong(it) },
            onRequestPermission = { permissionLauncher.launch(audioPermission) },
            onRefreshSongs = { viewModel.loadSongs(forceRefresh = true) },
            songs = songs,
            isLoadingSongs = isLoadingSongs,
            songError = songError,
            hasAudioPermission = hasPermission,
            shakeIndicator = {
                ShakeIndicator(
                    isEnabled = shakeSettings.isEnabled,
                    isActive = isShakeDetectionActive
                )
            },
            showShakeHint = shakeSettings.isEnabled
        )
    }
}

@Composable
private fun PlaybackContent(
    paddingValues: PaddingValues,
    song: Song?,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onRequestPermission: () -> Unit,
    onRefreshSongs: () -> Unit,
    songs: List<Song>,
    isLoadingSongs: Boolean,
    songError: String?,
    hasAudioPermission: Boolean,
    shakeIndicator: @Composable () -> Unit,
    showShakeHint: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AlbumArtPlaceholder()
            SongDetails(song = song)
            shakeIndicator()
        }

        SongLibrarySection(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            songs = songs,
            isLoading = isLoadingSongs,
            errorMessage = songError,
            hasPermission = hasAudioPermission,
            currentSong = song,
            onSongSelected = onSongSelected,
            onRequestPermission = onRequestPermission,
            onRefresh = onRefreshSongs
        )

        PlaybackControls(
            isPlaying = isPlaying,
            onTogglePlay = onTogglePlay,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            showShakeHint = showShakeHint
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SongLibrarySection(
    modifier: Modifier,
    songs: List<Song>,
    isLoading: Boolean,
    errorMessage: String?,
    hasPermission: Boolean,
    currentSong: Song?,
    onSongSelected: (Song) -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Your Library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (hasPermission && songs.isNotEmpty()) {
                    FilledTonalButton(onClick = onRefresh) {
                        Text("Refresh")
                    }
                }
            }

            when {
                !hasPermission -> {
                    Text(
                        text = "Allow ShakeSkip to access your music to choose songs for playback.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(onClick = onRequestPermission) {
                        Text("Grant Permission")
                    }
                }

                isLoading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Loading your songs...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRefresh) {
                        Text("Try Again")
                    }
                }

                songs.isEmpty() -> {
                    Text(
                        text = "No audio files found on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                    ) {
                        items(songs, key = { it.id }) { librarySong ->
                            SongRow(
                                song = librarySong,
                                isCurrent = librarySong.id == currentSong?.id,
                                onClick = { onSongSelected(librarySong) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val titleColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isCurrent) Icons.Default.Check else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (isCurrent) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            }
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    showShakeHint: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onSkipPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous track"
                )
            }

            FilledIconButton(
                onClick = onTogglePlay,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }

            IconButton(onClick = onSkipNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next track"
                )
            }
        }

        if (showShakeHint) {
            ShakeGestureHint()
        }
    }
}

@Composable
private fun AlbumArtPlaceholder() {
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun SongDetails(song: Song?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = song?.title ?: "No track playing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song?.artist ?: "Select a song to start playback",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
