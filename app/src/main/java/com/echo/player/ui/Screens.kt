package com.echo.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.echo.player.data.Playlist
import com.echo.player.data.Track
import com.echo.player.player.PlayerHolder
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun App(vm: AppViewModel, player: PlayerHolder) {
    if (!vm.loggedIn) {
        LoginScreen(vm)
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var full by remember { mutableStateOf(false) }

    BackHandler(enabled = full || vm.openPlaylist != null) {
        if (full) full = false else vm.closePlaylist()
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                Column {
                    MiniPlayer(player) { full = true }
                    NavigationBar(containerColor = Surface1) {
                        val colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = TextDim,
                            unselectedTextColor = TextDim,
                            indicatorColor = Color.Transparent,
                        )
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0; vm.closePlaylist() },
                            icon = { Icon(Icons.Filled.Home, null) },
                            label = { Text("Медиатека") },
                            colors = colors,
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1; vm.closePlaylist() },
                            icon = { Icon(Icons.Filled.Search, null) },
                            label = { Text("Поиск") },
                            colors = colors,
                        )
                    }
                }
            },
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when {
                    vm.openPlaylist != null -> TrackListScreen(vm, player)
                    tab == 0 -> LibraryScreen(vm)
                    else -> SearchScreen(vm, player)
                }
            }
        }

        AnimatedVisibility(
            visible = full,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            FullPlayer(player) { full = false }
        }
    }
}

// ---------- Вход ----------

@Composable
fun LoginScreen(vm: AppViewModel) {
    var token by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(Bg).systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Echo", color = Accent, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Вставьте токен Яндекс Музыки. Он хранится на телефоне в зашифрованном виде. " +
                "Токен равен паролю от аккаунта — никому его не показывайте.",
            color = TextDim,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Токен") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.login(token) },
            enabled = token.isNotBlank() && !vm.busy,
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
            else Text("Войти", fontWeight = FontWeight.Bold)
        }
        vm.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFFF6B6B))
        }
    }
}

// ---------- Медиатека ----------

@Composable
fun LibraryScreen(vm: AppViewModel) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Моя медиатека", color = Color.White, fontSize = 26.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.logout() }) { Text("Выйти", color = TextDim) }
            }
        }
        vm.error?.let { msg ->
            item { Text(msg, color = Color(0xFFFF6B6B), modifier = Modifier.padding(16.dp)) }
        }
        if (vm.busy && vm.playlists.isEmpty()) {
            item { Loading() }
        }
        items(vm.playlists) { p -> PlaylistRow(p) { vm.open(p) } }
    }
}

@Composable
fun PlaylistRow(p: Playlist, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(p.coverUrl, Modifier.size(60.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(p.title, color = Color.White, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (p.trackCount > 0) Text("Плейлист · ${p.trackCount} треков", color = TextDim, fontSize = 13.sp)
        }
    }
}

@Composable
fun TrackListScreen(vm: AppViewModel, player: PlayerHolder) {
    val p = vm.openPlaylist ?: return
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.closePlaylist() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
            }
            Text(
                p.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Box(
                    Modifier.padding(16.dp).size(56.dp).clip(CircleShape).background(Accent)
                        .clickable(enabled = vm.openTracks.isNotEmpty()) { player.play(vm.openTracks, 0) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.PlayArrow, "Играть", tint = Color.Black, modifier = Modifier.size(34.dp)) }
            }
            if (vm.busy && vm.openTracks.isEmpty()) item { Loading() }
            vm.error?.let { msg ->
                item { Text(msg, color = Color(0xFFFF6B6B), modifier = Modifier.padding(16.dp)) }
            }
            itemsIndexed(vm.openTracks) { i, t ->
                TrackRow(t, player.current?.mediaId == t.id) { player.play(vm.openTracks, i) }
            }
        }
    }
}

// ---------- Поиск ----------

@Composable
fun SearchScreen(vm: AppViewModel, player: PlayerHolder) {
    var query by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Text(
            "Поиск", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Что хотите послушать?") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search(query) }),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        if (vm.busy && vm.results.isEmpty()) Loading()
        vm.error?.let { Text(it, color = Color(0xFFFF6B6B), modifier = Modifier.padding(16.dp)) }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(vm.results) { i, t ->
                TrackRow(t, player.current?.mediaId == t.id) { player.play(vm.results, i) }
            }
        }
    }
}

// ---------- Общие элементы ----------

@Composable
fun Loading() {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Accent)
    }
}

@Composable
fun Cover(url: String?, modifier: Modifier, corner: Dp = 4.dp) {
    Box(modifier.clip(RoundedCornerShape(corner)).background(Surface2)) {
        if (url != null) {
            AsyncImage(
                model = url, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun TrackRow(t: Track, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(t.coverUrl, Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.title, color = if (active) Accent else Color.White, fontSize = 16.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(t.artist, color = TextDim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------- Плеер ----------

@Composable
fun MiniPlayer(player: PlayerHolder, onOpen: () -> Unit) {
    val item = player.current ?: return
    val md = item.mediaMetadata
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp)).background(Surface2)
            .clickable(onClick = onOpen).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(md.artworkUri?.toString(), Modifier.size(44.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(md.title?.toString().orEmpty(), color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(md.artist?.toString().orEmpty(), color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { player.toggle() }) {
            Icon(
                if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                "Пауза/играть", tint = Color.White, modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
fun FullPlayer(player: PlayerHolder, onClose: () -> Unit) {
    val item = player.current
    val md = item?.mediaMetadata

    var pos by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(player.isPlaying, item) {
        while (true) {
            if (!dragging) pos = player.position()
            delay(500)
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3A3A3A), Bg)))
            .systemBarsPadding().padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.KeyboardArrowDown, "Свернуть", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Cover(md?.artworkUri?.toString(), Modifier.fillMaxWidth().aspectRatio(1f), corner = 8.dp)
        Spacer(Modifier.height(32.dp))
        Text(
            md?.title?.toString().orEmpty(), color = Color.White, fontSize = 24.sp,
            fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Text(md?.artist?.toString().orEmpty(), color = TextDim, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(16.dp))

        val maxMs = max(player.duration, 1L).toFloat()
        Slider(
            value = pos.toFloat().coerceIn(0f, maxMs),
            onValueChange = { dragging = true; pos = it.toLong() },
            onValueChangeFinished = { player.seekTo(pos); dragging = false },
            valueRange = 0f..maxMs,
            colors = SliderDefaults.colors(
                thumbColor = Color.White, activeTrackColor = Color.White,
                inactiveTrackColor = Color(0x55FFFFFF),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(pos), color = TextDim, fontSize = 12.sp)
            Text(fmt(player.duration), color = TextDim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { player.prev() }, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Filled.SkipPrevious, "Назад", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(Color.White).clickable { player.toggle() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "Пауза/играть", tint = Color.Black, modifier = Modifier.size(44.dp),
                )
            }
            IconButton(onClick = { player.next() }, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Filled.SkipNext, "Вперёд", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
    }
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
