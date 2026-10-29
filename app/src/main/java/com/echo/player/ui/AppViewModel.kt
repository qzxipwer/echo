package com.echo.player.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echo.player.EchoApp
import com.echo.player.data.Playlist
import com.echo.player.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val api get() = EchoApp.api
    private val tokens get() = EchoApp.tokens

    var loggedIn by mutableStateOf(tokens.token != null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set
    var openPlaylist by mutableStateOf<Playlist?>(null)
        private set
    var openTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var results by mutableStateOf<List<Track>>(emptyList())
        private set

    init {
        if (loggedIn) loadLibrary()
    }

    private fun launchIO(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            busy = true
            error = null
            try {
                block()
            } catch (e: Exception) {
                error = e.message ?: "Ошибка"
            } finally {
                busy = false
            }
        }
    }

    fun login(token: String) = launchIO {
        tokens.token = token.trim()
        api.resetUid()
        try {
            api.uid() // проверка токена
        } catch (e: Exception) {
            tokens.token = null
            throw e
        }
        loggedIn = true
        playlists = libraryList()
    }

    fun logout() {
        tokens.token = null
        api.resetUid()
        loggedIn = false
        playlists = emptyList()
        openPlaylist = null
        results = emptyList()
    }

    private fun libraryList(): List<Playlist> =
        listOf(Playlist("likes", "Мне нравится", 0, null)) + api.playlists()

    fun loadLibrary() = launchIO { playlists = libraryList() }

    fun open(p: Playlist) {
        openPlaylist = p
        openTracks = emptyList()
        launchIO { openTracks = if (p.kind == "likes") api.likes() else api.playlistTracks(p.kind) }
    }

    fun closePlaylist() {
        openPlaylist = null
    }

    fun search(query: String) {
        if (query.isBlank()) return
        launchIO { results = api.search(query.trim()) }
    }
}
