package com.shakeskip.player.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: Long = 0L,
    val filePath: String
)
