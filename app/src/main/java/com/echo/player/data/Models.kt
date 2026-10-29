package com.echo.player.data

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
    val durationMs: Long,
)

data class Playlist(
    val kind: String,
    val title: String,
    val trackCount: Int,
    val coverUrl: String?,
)
