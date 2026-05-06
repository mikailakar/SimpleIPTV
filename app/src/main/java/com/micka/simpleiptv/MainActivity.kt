@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.micka.simpleiptv

import android.content.Context
import android.os.Bundle
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.UUID
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.style.TextOverflow
import android.util.Log
import org.json.JSONArray
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import java.io.Serializable
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.ui.draw.rotate
import android.media.AudioManager
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.media3.ui.TrackSelectionDialogBuilder
import androidx.media3.common.C
import androidx.compose.material.icons.filled.Fullscreen
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.app.PendingIntent
import android.app.RemoteAction
import android.graphics.drawable.Icon
import androidx.media3.common.Player
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.material.icons.filled.AspectRatio
import android.util.Base64
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.filled.Code
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.rememberLazyListState

// --- DATA MODELS ---

data class Playlist(
    val id: String, val name: String, val type: String, val url: String, val username: String = "", val password: String = ""
)

data class Channel(
    val name: String,
    val streamUrl: String,
    val category: String = "OTHER",
    val logoUrl: String? = null,
    val number: String = "",
    val id: String = streamUrl
) : Serializable

data class EpgProgram(
    val title: String,
    val start: String,
    val end: String,
    val description: String
)

// --- LOCAL STORAGE & PREFERENCES ---

fun savePlaylists(context: Context, playlists: List<Playlist>) {
    val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
    val stringData = playlists.joinToString("|||") { "${it.id}::${it.name}::${it.type}::${it.url}::${it.username}::${it.password}" }
    prefs.edit { putString("PLAYLIST_DATA", stringData) }
}

fun loadPlaylists(context: Context): List<Playlist> {
    val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
    val stringData = prefs.getString("PLAYLIST_DATA", "") ?: ""
    if (stringData.isEmpty()) return emptyList()
    return try {
        stringData.split("|||").map {
            val p = it.split("::")
            Playlist(p[0], p[1], p[2], p[3], p.getOrElse(4) { "" }, p.getOrElse(5) { "" })
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

fun setLastOpenedPlaylistId(context: Context, id: String?) {
    context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).edit {
        putString("LAST_OPENED_ID", id)
    }
}

fun getLastOpenedPlaylistId(context: Context): String? {
    return context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).getString("LAST_OPENED_ID", null)
}

fun saveLastSelection(context: Context, playlistId: String, category: String, channelId: String?) {
    val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
    prefs.edit {
        putString("LAST_CAT_$playlistId", category)
            .putString("LAST_CHAN_$playlistId", channelId)
    }
}

fun getLastCategory(context: Context, playlistId: String): String =
    context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).getString("LAST_CAT_$playlistId", "ALL") ?: "ALL"

fun getLastChannelId(context: Context, playlistId: String): String? =
    context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).getString("LAST_CHAN_$playlistId", null)

fun saveFavorites(context: Context, playlistId: String, favorites: List<String>) {
    val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
    prefs.edit { putString("FAVS_LIST_$playlistId", favorites.joinToString("|||")) }
}

fun loadFavorites(context: Context, playlistId: String): List<String> {
    val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
    val str = prefs.getString("FAVS_LIST_$playlistId", null)
    if (str != null) return if (str.isEmpty()) emptyList() else str.split("|||")

    // Migrate legacy favorites format if necessary
    val oldSet = prefs.getStringSet("FAVS_$playlistId", emptySet()) ?: emptySet()
    return oldSet.toList()
}

fun getSavedResizeMode(context: Context): Int {
    return context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        .getInt("RESIZE_MODE", AspectRatioFrameLayout.RESIZE_MODE_FIT)
}

fun saveResizeMode(context: Context, mode: Int) {
    context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        .edit { putInt("RESIZE_MODE", mode) }
}

fun getCustomCategoryOrder(context: Context): List<String> {
    val str = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).getString("CUSTOM_CAT_ORDER", "") ?: ""
    return if (str.isEmpty()) emptyList() else str.split("|||")
}

fun saveCustomCategoryOrder(context: Context, order: List<String>) {
    context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).edit {
        putString("CUSTOM_CAT_ORDER", order.joinToString("|||"))
    }
}

fun getSetting(context: Context, key: String, default: String): String {
    return context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).getString(key, default) ?: default
}

fun saveSetting(context: Context, key: String, value: String) {
    context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).edit { putString(key, value) }
}

fun getHiddenCategories(context: Context): Set<String> {
    return context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).getStringSet("HIDDEN_CATS", emptySet()) ?: emptySet()
}

fun saveHiddenCategories(context: Context, hidden: Set<String>) {
    context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).edit {
        putStringSet("HIDDEN_CATS", hidden)
    }
}

fun getUpdateInterval(context: Context): Long {
    return context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).getLong("UPDATE_INTERVAL", 86400000L)
}

fun saveUpdateInterval(context: Context, ms: Long) {
    context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).edit {
        putLong("UPDATE_INTERVAL", ms)
    }
}

// --- PARSERS & FETCHERS ---

fun parseM3U(m3uData: String): List<Channel> {
    val channels = mutableListOf<Channel>()
    val lines = m3uData.lines()

    var currentCategory = "OTHER"
    var currentLogo: String? = null
    var currentName = "Unknown Channel"
    var currentOfficialChno: String? = null
    var hasExplicitGroup = false

    var globalChannelCounter = 1
    val separatorRegex = Regex("""^[*=\-~|>]+(.*?)[*=\-~|<]+$""")

    for (line in lines) {
        val trimmedLine = line.trim()
        if (trimmedLine.isEmpty()) continue

        if (trimmedLine.startsWith("#EXTGRP:")) {
            currentCategory = trimmedLine.substringAfter(":", "OTHER").trim()
        }
        else if (trimmedLine.startsWith("#EXTINF")) {
            hasExplicitGroup = false

            val groupMatch = Regex("""(?:group-title|tvg-group)="([^"]*)"""").find(trimmedLine)
            if (groupMatch != null && groupMatch.groupValues[1].isNotBlank()) {
                currentCategory = groupMatch.groupValues[1].trim()
                hasExplicitGroup = true
            }

            val logoMatch = Regex("""tvg-logo="([^"]*)"""").find(trimmedLine)
            currentLogo = logoMatch?.groupValues?.get(1)

            val chnoMatch = Regex("""tvg-chno="([^"]*)"""").find(trimmedLine)
            currentOfficialChno = chnoMatch?.groupValues?.get(1)

            currentName = trimmedLine.substringAfterLast(",", "Unknown Channel").trim()
        }
        else if (trimmedLine.startsWith("http")) {
            val separatorMatch = separatorRegex.find(currentName)

            if (separatorMatch != null && separatorMatch.groupValues[1].isNotBlank()) {
                if (!hasExplicitGroup) {
                    currentCategory = separatorMatch.groupValues[1].trim()
                }
            }

            val finalNumber = currentOfficialChno?.takeIf { it.isNotBlank() } ?: globalChannelCounter.toString()

            channels.add(Channel(currentName, trimmedLine, currentCategory, currentLogo, finalNumber))
            globalChannelCounter++

            // Reset state for next channel iteration
            currentLogo = null
            currentName = "Unknown Channel"
            currentOfficialChno = null
            hasExplicitGroup = false
        }
    }
    return channels
}

fun parseXtreamJson(categoriesJson: String, streamsJson: String, baseUrl: String, user: String, pass: String): List<Channel> {
    try {
        val categoriesArray = JSONArray(categoriesJson)
        val categoryMap = mutableMapOf<String, String>()
        val categoryOrder = mutableListOf<String>()

        for (i in 0 until categoriesArray.length()) {
            val catObj = categoriesArray.getJSONObject(i)
            val id = catObj.optString("category_id")
            val name = catObj.optString("category_name", "OTHER").trim()

            categoryMap[id] = name
            if (!categoryOrder.contains(name)) {
                categoryOrder.add(name)
            }
        }

        val tempChannels = mutableListOf<Channel>()
        val streamsArray = JSONArray(streamsJson)

        for (i in 0 until streamsArray.length()) {
            val streamObj = streamsArray.getJSONObject(i)

            val name = streamObj.optString("name", "Unknown Channel").trim()
            val streamId = streamObj.optString("stream_id")
            val categoryId = streamObj.optString("category_id")
            val logo = streamObj.optString("stream_icon").takeIf { it.isNotBlank() }

            val jsonNum = streamObj.optString("num")
            val channelNumber = jsonNum.takeIf { it.isNotBlank() && it != "0" } ?: (i + 1).toString()

            val categoryName = categoryMap[categoryId] ?: "OTHER"
            val streamUrl = "$baseUrl/$user/$pass/$streamId"

            tempChannels.add(Channel(name, streamUrl, categoryName, logo, channelNumber))
        }

        return tempChannels.sortedBy { channel ->
            val index = categoryOrder.indexOf(channel.category)
            if (index == -1) Int.MAX_VALUE else index
        }

    } catch (e: Exception) {
        Log.e("IPTV_ERROR", "Failed to parse JSON", e)
        return emptyList()
    }
}

fun attemptXtreamExtraction(name: String, m3uUrl: String, existingId: String? = null): Playlist? {
    return try {
        val uri = m3uUrl.toUri()
        val username = uri.getQueryParameter("username")
        val password = uri.getQueryParameter("password")

        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            val baseUrl = "${uri.scheme}://${uri.authority}"
            Playlist(
                id = existingId ?: UUID.randomUUID().toString(),
                name = name,
                type = "XTREAM",
                url = baseUrl,
                username = username,
                password = password
            )
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun fetchXtreamEpg(playlist: Playlist?, channel: Channel?): List<EpgProgram> {
    if (playlist == null || playlist.type != "XTREAM" || channel == null) return emptyList()

    return try {
        withContext(Dispatchers.IO) {
            val streamId = channel.streamUrl.substringAfterLast("/")
            val baseUrl = if (playlist.url.endsWith("/")) playlist.url.dropLast(1) else playlist.url

            val apiUrl = "$baseUrl/player_api.php?username=${playlist.username}&password=${playlist.password}&action=get_short_epg&stream_id=$streamId"

            val response = URL(apiUrl).readText()
            val json = org.json.JSONObject(response)
            val listings = json.optJSONArray("epg_listings") ?: return@withContext emptyList()

            val programs = mutableListOf<EpgProgram>()
            for (i in 0 until listings.length()) {
                val item = listings.getJSONObject(i)

                val rawTitle = item.optString("title", "")
                val rawDesc = item.optString("description", "")

                val title = try { String(Base64.decode(rawTitle, Base64.DEFAULT)) } catch(_: Exception){ rawTitle }
                val desc = try { String(Base64.decode(rawDesc, Base64.DEFAULT)) } catch(_: Exception){ rawDesc }

                val start = item.optString("start", "").substringAfter(" ").take(5)
                val end = item.optString("end", "").substringAfter(" ").take(5)

                programs.add(EpgProgram(
                    title = title.ifBlank { rawTitle },
                    start = start,
                    end = end,
                    description = desc.ifBlank { rawDesc }
                ))
            }
            programs
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

// --- MAIN ACTIVITY ---

var isAppInPipMode = mutableStateOf(false)
var globalPlayer: ExoPlayer? = null

class MainActivity : ComponentActivity() {

    private val actionPipPlayPause = "pip_play_pause"

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == actionPipPlayPause) {
                globalPlayer?.let { player ->
                    if (player.isPlaying) player.pause() else player.play()
                    updatePipParams()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen awake while app is running
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            IntentFilter(actionPipPlayPause),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0F172A))) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(pipReceiver) } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getPipParams(): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isPlaying = globalPlayer?.isPlaying == true

            val iconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            val icon = Icon.createWithResource(this, iconRes)

            val intent = Intent(actionPipPlayPause).setPackage(packageName)
            val pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val action = RemoteAction(icon, if (isPlaying) "Pause" else "Play", "Play/Pause", pendingIntent)

            return PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(listOf(action))
                .build()
        }
        return null
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getPipParams()?.let { setPictureInPictureParams(it) }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (globalPlayer?.isPlaying == true) {
                getPipParams()?.let { enterPictureInPictureMode(it) }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isAppInPipMode.value = isInPictureInPictureMode
    }
}

// --- NAVIGATION MANAGER ---

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val playlists = remember { loadPlaylists(context) }

    val sharedPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            globalPlayer = this
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sharedPlayer.release()
            globalPlayer = null
        }
    }

    var activePlaylist by remember {
        mutableStateOf(getLastOpenedPlaylistId(context)?.let { id -> playlists.find { it.id == id } })
    }
    var currentScreen by remember {
        mutableStateOf(if (activePlaylist != null) "LOADING" else "DASHBOARD")
    }

    var activeChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var currentPlaybackQueue by remember { mutableStateOf<List<Channel>>(emptyList()) }

    when (currentScreen) {
        "DASHBOARD" -> {
            DashboardScreen(
                onPlaylistClicked = { playlist ->
                    activePlaylist = playlist
                    setLastOpenedPlaylistId(context, playlist.id)
                    currentScreen = "LOADING"
                }
            )
        }
        "LOADING" -> {
            LoadingScreen(
                playlist = activePlaylist!!,
                onLoaded = { channels ->
                    activeChannels = channels
                    currentScreen = "CHANNELS"
                },
                onError = {
                    setLastOpenedPlaylistId(context, null)
                    currentScreen = "DASHBOARD"
                }
            )
        }
        "CHANNELS" -> {
            ChannelListScreen(
                playlistId = activePlaylist?.id ?: "default",
                channels = activeChannels,
                sharedPlayer = sharedPlayer,
                onChannelClick = { channel, queue ->
                    selectedChannel = channel
                    currentPlaybackQueue = queue
                    currentScreen = "PLAYER"
                },
                onBack = {
                    sharedPlayer.stop()
                    sharedPlayer.release()
                    (context as? android.app.Activity)?.finish()
                },
                onSettingsClick = {
                    sharedPlayer.stop()
                    currentScreen = "SETTINGS"
                }
            )
        }
        "PLAYER" -> {
            val pId = activePlaylist?.id ?: ""
            val isFav = loadFavorites(context, pId).contains(selectedChannel?.id)

            VideoPlayerScreen(
                playlistId = pId,
                sharedPlayer = sharedPlayer,
                currentChannel = selectedChannel,
                isFavorite = isFav,
                onToggleFavorite = {
                    selectedChannel?.let { ch ->
                        val currentFavs = loadFavorites(context, pId)
                        val newFavs = if (currentFavs.contains(ch.id)) currentFavs - ch.id else currentFavs + ch.id
                        saveFavorites(context, pId, newFavs)
                    }
                },
                onNextChannel = {
                    val currentIndex = currentPlaybackQueue.indexOf(selectedChannel)
                    if (currentIndex != -1 && currentIndex < currentPlaybackQueue.size - 1) {
                        val nextChannel = currentPlaybackQueue[currentIndex + 1]
                        selectedChannel = nextChannel
                        saveLastSelection(context, pId, getLastCategory(context, pId), nextChannel.id)
                    }
                },
                onPrevChannel = {
                    val currentIndex = currentPlaybackQueue.indexOf(selectedChannel)
                    if (currentIndex > 0) {
                        val prevChannel = currentPlaybackQueue[currentIndex - 1]
                        selectedChannel = prevChannel
                        saveLastSelection(context, pId, getLastCategory(context, pId), prevChannel.id)
                    }
                },
                onBackClick = { currentScreen = "CHANNELS" }
            )
        }
        "SETTINGS" -> {
            SettingsScreen(
                playlistId = activePlaylist?.id ?: "default",
                channels = activeChannels,
                onBack = { currentScreen = "CHANNELS" },
                onChangePlaylist = {
                    setLastOpenedPlaylistId(context, null)
                    currentScreen = "DASHBOARD"
                    sharedPlayer.stop()
                }
            )
        }
    }
}

// --- DASHBOARD SCREEN ---

@Composable
fun DashboardScreen(onPlaylistClicked: (Playlist) -> Unit) {
    val context = LocalContext.current
    var playlists by remember { mutableStateOf(loadPlaylists(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(playlists) { savePlaylists(context, playlists) }

    Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF1E112A), Color(0xFF0A1020))))) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).background(Color.White.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tv, contentDescription = "Logo", tint = Color.Red, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Playlists", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 70.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists) { playlist ->
                    PlaylistCircleItem(
                        name = playlist.name,
                        onClick = { onPlaylistClicked(playlist) },
                        onEdit = { editingPlaylist = playlist; showAddDialog = true },
                        onDelete = { playlists = playlists.filter { it.id != playlist.id } }
                    )
                }

                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape).clickable { editingPlaylist = null; showAddDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Gray, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add Playlist", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Legal & Open Source Disclaimer
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "SimpleIPTV is strictly a media player application.",
                        color = Color.Gray.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 10.sp
                    )
                    Text(
                        text = "It does not provide, include, or sell any playlists, streams, or content subscriptions.",
                        color = Color.Gray.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 10.sp
                    )
                    Text(
                        text = "SimpleIPTV is free and open-source.",
                        color = Color(0xFFE91E63).copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 10.sp
                    )
                }

                IconButton(
                    onClick = { uriHandler.openUri("https://github.com/mikailakar/SimpleIPTV") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp)
                        .size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "GitHub Repository",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlaylistDialog(
            initialPlaylist = editingPlaylist,
            onDismiss = { showAddDialog = false; editingPlaylist = null },
            onSave = { savedPlaylist ->
                playlists = if (editingPlaylist == null) playlists + savedPlaylist else playlists.map { if (it.id == savedPlaylist.id) savedPlaylist else it }
                showAddDialog = false; editingPlaylist = null
            }
        )
    }
}

@Composable
fun PlaylistCircleItem(name: String, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Box(modifier = Modifier.size(70.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFE91E63).copy(alpha = 0.2f))
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(28.dp))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .background(Color(0xFF333A47), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable { menuExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Options", tint = Color.White, modifier = Modifier.size(12.dp))
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, modifier = Modifier.background(Color(0xFF1E2330))) {
                    DropdownMenuItem(text = { Text("Edit", color = Color.White, fontSize = 12.sp) }, onClick = { menuExpanded = false; onEdit() })
                    DropdownMenuItem(text = { Text("Delete", color = Color.Red, fontSize = 12.sp) }, onClick = { menuExpanded = false; onDelete() })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- LOADING SCREEN ---

@Composable
fun LoadingScreen(playlist: Playlist, onLoaded: (List<Channel>) -> Unit, onError: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)

                    val objectCacheFile = File(context.filesDir, "${playlist.id}_parsed.dat")
                    val lastParseTime = prefs.getLong("LAST_PARSE_${playlist.id}", 0L)
                    val updateInterval = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).getLong("UPDATE_INTERVAL", 86400000L)

                    val isCacheValid = if (updateInterval == 0L) {
                        false
                    } else {
                        (System.currentTimeMillis() - lastParseTime) < updateInterval
                    }

                    var finalChannels: List<Channel> = emptyList()

                    if (isCacheValid && objectCacheFile.exists()) {
                        try {
                            ObjectInputStream(objectCacheFile.inputStream()).use {
                                @Suppress("UNCHECKED_CAST")
                                finalChannels = it.readObject() as List<Channel>
                            }
                        } catch (e: Exception) {
                            objectCacheFile.delete()
                            e.printStackTrace()
                        }
                    }

                    if (finalChannels.isEmpty()) {
                        val baseUrl = if (playlist.url.endsWith("/")) playlist.url.dropLast(1) else playlist.url

                        finalChannels = if (playlist.type == "XTREAM") {
                            val apiUrl = "$baseUrl/player_api.php?username=${playlist.username}&password=${playlist.password}"
                            val catJson = URL("$apiUrl&action=get_live_categories").readText()
                            val streamJson = URL("$apiUrl&action=get_live_streams").readText()
                            parseXtreamJson(catJson, streamJson, baseUrl, playlist.username, playlist.password)
                        } else {
                            val downloadedText = URL(playlist.url).readText()
                            parseM3U(downloadedText)
                        }

                        if (finalChannels.isNotEmpty()) {
                            ObjectOutputStream(objectCacheFile.outputStream()).use {
                                it.writeObject(finalChannels)
                            }
                            prefs.edit {
                                putLong(
                                    "LAST_PARSE_${playlist.id}",
                                    System.currentTimeMillis()
                                )
                            }
                        }
                    }

                    if (finalChannels.isEmpty()) throw Exception("No channels found")

                    withContext(Dispatchers.Main) {
                        onLoaded(finalChannels)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load playlist. Check internet connection.", Toast.LENGTH_LONG).show()
                    Log.e("IPTV_ERROR", "Loading Error", e)
                    onError()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFFACC15))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading channels for '${playlist.name}'...", color = Color.White)
        }
    }
}

// --- CHANNEL LIST SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(
    playlistId: String,
    channels: List<Channel>,
    sharedPlayer: ExoPlayer,
    onChannelClick: (Channel, List<Channel>) -> Unit,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    var favoriteIds by remember { mutableStateOf(loadFavorites(context, playlistId)) }

    val catSort = getSetting(context, "CAT_SORT", "DEFAULT")
    val chanSort = getSetting(context, "CHAN_SORT", "DEFAULT")
    val bgColorSetting = getSetting(context, "BG_COLOR", "DARK")
    val hiddenCats = getHiddenCategories(context)

    val rootBgColor = if (bgColorSetting == "BLACK") Color.Black else Color(0xFF070B14)

    val categoryListState = rememberLazyListState()
    val channelListState = rememberLazyListState()

    // Render minimalist UI during Picture-in-Picture mode
    if (isAppInPipMode.value) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { PlayerView(it).apply { player = sharedPlayer; useController = false } },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    var showMiniControls by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(sharedPlayer.isPlaying) }
    var showEpgDialog by remember { mutableStateOf(false) }

    var previewChannel by remember {
        mutableStateOf(channels.find { it.id == getLastChannelId(context, playlistId) } ?: channels.firstOrNull())
    }

    val playlists = remember { loadPlaylists(context) }
    val currentPlaylist = remember(playlistId) { playlists.find { it.id == playlistId } }
    var epgList by remember { mutableStateOf<List<EpgProgram>>(emptyList()) }
    var isEpgLoading by remember { mutableStateOf(false) }

    LaunchedEffect(showEpgDialog, previewChannel) {
        if (showEpgDialog) {
            isEpgLoading = true
            epgList = fetchXtreamEpg(currentPlaylist, previewChannel)
            isEpgLoading = false
        }
    }

    LaunchedEffect(showMiniControls, isPlaying) {
        if (showMiniControls && isPlaying) {
            delay(3000)
            showMiniControls = false
        }
    }

    DisposableEffect(Unit) {
        if (previewChannel != null && sharedPlayer.playbackState == Player.STATE_IDLE) {
            try {
                val mediaItem = MediaItem.fromUri(previewChannel!!.streamUrl)
                sharedPlayer.setMediaItem(mediaItem)
                sharedPlayer.prepare()
                sharedPlayer.play()
            } catch (e: Exception) { e.printStackTrace() }
        }
        onDispose { }
    }

    var selectedCategory by remember {
        mutableStateOf(
            previewChannel?.let { ch ->
                val savedCategory = getLastCategory(context, playlistId)

                if (savedCategory == "FAVORITES" && favoriteIds.contains(ch.id)) {
                    "FAVORITES"
                } else {
                    ch.category
                }
            } ?: "ALL"
        )
    }

    var channelSearchQuery by remember { mutableStateOf("") }
    var categorySearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(selectedCategory, previewChannel) {
        saveLastSelection(context, playlistId, selectedCategory, previewChannel?.id)
    }

    LaunchedEffect(previewChannel) {
        previewChannel?.let { channel ->
            val currentPlayingUrl = sharedPlayer.currentMediaItem?.mediaId
            if (currentPlayingUrl != channel.streamUrl) {
                val mediaItem = MediaItem.Builder()
                    .setUri(channel.streamUrl)
                    .setMediaId(channel.streamUrl)
                    .build()

                sharedPlayer.setMediaItem(mediaItem)
                sharedPlayer.prepare()
                sharedPlayer.playWhenReady = true
                isPlaying = true
            }
        }
    }

    var currentTime by remember { mutableStateOf(SimpleDateFormat("EEE, MMM dd  hh:mm a", Locale.US).format(Date())) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(1000)
            currentTime = SimpleDateFormat("EEE, MMM dd  hh:mm a", Locale.US).format(Date())
        }
    }

    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var videoFps by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(previewChannel) {
        while(true) {
            try {
                val format = sharedPlayer.videoFormat
                if (format != null) {
                    videoWidth = format.width.takeIf { it != -1 } ?: 0
                    videoHeight = format.height.takeIf { it != -1 } ?: 0
                    videoFps = format.frameRate.takeIf { it != -1f } ?: 0f
                } else {
                    videoWidth = 0; videoHeight = 0; videoFps = 0f
                }
            } catch (e: Exception) { e.printStackTrace() }
            delay(1000)
        }
    }

    val customCatOrder = remember(catSort) { getCustomCategoryOrder(context) }

    val categories = remember(channels, hiddenCats, catSort, customCatOrder) {
        val baseCats = channels.map { it.category }.distinct().filter { !hiddenCats.contains(it) }
        val sortedCats = when(catSort) {
            "A-Z" -> baseCats.sorted()
            "Z-A" -> baseCats.sortedDescending()
            "CUSTOM" -> baseCats.sortedBy { cat ->
                val idx = customCatOrder.indexOf(cat)
                if (idx == -1) Int.MAX_VALUE else idx
            }
            else -> baseCats
        }
        listOf("ALL", "FAVORITES") + sortedCats
    }

    val filteredCategories = categories.filter { it.contains(categorySearchQuery, ignoreCase = true) }

    val filteredChannels = remember(channels, selectedCategory, channelSearchQuery, hiddenCats, favoriteIds, chanSort) {
        if (selectedCategory == "FAVORITES") {
            val favs = favoriteIds.mapNotNull { id -> channels.find { it.id == id } }
            favs.filter {
                it.name.contains(channelSearchQuery, ignoreCase = true) ||
                        it.number.contains(channelSearchQuery, ignoreCase = true)
            }
        } else {
            channels.filter { channel ->
                val matchesCategory = if (selectedCategory == "ALL") !hiddenCats.contains(channel.category) else channel.category == selectedCategory

                val matchesSearch = channel.name.contains(channelSearchQuery, ignoreCase = true) ||
                        channel.number.contains(channelSearchQuery, ignoreCase = true)

                matchesCategory && matchesSearch
            }.sortedWith(compareBy {
                when(chanSort) {
                    "A-Z" -> it.name
                    "Z-A" -> it.name
                    else -> ""
                }
            }).let { if (chanSort == "Z-A") it.reversed() else it }
        }
    }

    LaunchedEffect(configuration.orientation) {
        val activeCategoryIndex = filteredCategories.indexOf(selectedCategory)
        if (activeCategoryIndex > 0) {
            categoryListState.scrollToItem((activeCategoryIndex - 2).coerceAtLeast(0))
        }

        val activeChannelIndex = filteredChannels.indexOf(previewChannel)
        if (activeChannelIndex != -1) {
            channelListState.scrollToItem((activeChannelIndex - 3).coerceAtLeast(0))
        }
    }

    LaunchedEffect(selectedCategory) {
        val activeChannelIndex = filteredChannels.indexOf(previewChannel)

        if (activeChannelIndex != -1) {
            channelListState.scrollToItem((activeChannelIndex - 3).coerceAtLeast(0))
        } else {
            channelListState.scrollToItem(0)
        }
    }

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize().background(rootBgColor)) {

        Column(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(18.dp)) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Live", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentTime.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            sharedPlayer.stop()
                            sharedPlayer.release()
                            (context as? android.app.Activity)?.finish()
                        },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout/Exit",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {

                // Landscape Layout
                Row(modifier = Modifier.fillMaxSize()) {

                    Column(modifier = Modifier.weight(0.15f).fillMaxHeight()) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                            DarkSearchField(categorySearchQuery, "Search Category") { categorySearchQuery = it }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(state = categoryListState) {
                            items(filteredCategories) { cat ->
                                val isBrowsing = selectedCategory == cat
                                val isPlaying = previewChannel?.category == cat

                                val count = when(cat) {
                                    "ALL" -> channels.size
                                    "FAVORITES" -> favoriteIds.size
                                    else -> channels.count { it.category == cat }
                                }.toString()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCategory = cat }
                                        .background(if (isBrowsing) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                        .padding(vertical = 4.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (cat == "FAVORITES") {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    Text(
                                        text = cat,
                                        color = if (isBrowsing || isPlaying) Color.White else Color.Gray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 10.sp
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(count, color = Color.Gray, fontSize = 8.sp)

                                    if (isPlaying) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(modifier = Modifier.width(2.dp).height(10.dp).background(Color(0xFFE91E63)))
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(0.25f).fillMaxHeight()) {
                        DarkSearchField(channelSearchQuery, "Search Channels") { channelSearchQuery = it }
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(state = channelListState) {
                            items(filteredChannels) { channel ->
                                val isPreviewed = previewChannel?.id == channel.id
                                val isFavorite = favoriteIds.contains(channel.id)
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { previewChannel = channel }
                                        .background(if (isPreviewed) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                        .padding(vertical = 3.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(channel.number, color = Color.Gray, fontSize = 8.sp, modifier = Modifier.width(24.dp))

                                    if (isFavorite && selectedCategory != "FAVORITES") {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(12.dp))
                                    } else if (!channel.logoUrl.isNullOrBlank()) {
                                        SubcomposeAsyncImage(model = channel.logoUrl, contentDescription = channel.name, modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)), error = { Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White.copy(alpha = 0.8f)) })
                                    } else {
                                        Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(channel.name, color = Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(0.60f).fillMaxHeight()) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showMiniControls = !showMiniControls }
                        ) {
                            AndroidView(
                                factory = { PlayerView(it).apply { player = sharedPlayer; useController = false } },
                                modifier = Modifier.fillMaxSize()
                            )

                            @Suppress("RemoveRedundantQualifierName")
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showMiniControls,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {

                                        IconButton(
                                            onClick = {
                                                if (sharedPlayer.isPlaying) sharedPlayer.pause() else sharedPlayer.play()
                                                isPlaying = sharedPlayer.isPlaying
                                            },
                                            modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                        ) {
                                            Icon(
                                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                showMiniControls = false
                                                previewChannel?.let { onChannelClick(it, filteredChannels) }
                                            },
                                            modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(32.dp))
                                        }

                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth().padding(end = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = previewChannel?.name ?: "", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                if (videoWidth > 0 && videoHeight > 0) {
                                    val badge = when { videoHeight >= 2160 -> "4K"; videoHeight >= 1440 -> "2K"; videoHeight >= 1080 -> "FHD"; videoHeight >= 720 -> "HD"; else -> "SD" }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) { Text(badge, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                                        Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) { Text("${videoWidth}x${videoHeight}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.SemiBold) }
                                        if (videoFps > 0f) Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) { Text("${videoFps.toInt()} FPS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.SemiBold) }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                val isCurrentFav = favoriteIds.contains(previewChannel?.id)

                                IconButton(
                                    onClick = { showEpgDialog = true },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = "EPG",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        previewChannel?.let { ch ->
                                            val newFavorites = if (isCurrentFav) favoriteIds - ch.id else favoriteIds + ch.id
                                            favoriteIds = newFavorites; saveFavorites(context, playlistId, newFavorites)
                                        }
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Favorite",
                                        tint = if (isCurrentFav) Color(0xFFFACC15) else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

            } else {

                // Portrait Layout
                Column(modifier = Modifier.fillMaxSize()) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showMiniControls = !showMiniControls }
                    ) {
                        AndroidView(
                            factory = { PlayerView(it).apply { player = sharedPlayer; useController = false } },
                            modifier = Modifier.fillMaxSize()
                        )

                        @Suppress("RemoveRedundantQualifierName")
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showMiniControls,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {

                                    IconButton(
                                        onClick = {
                                            if (sharedPlayer.isPlaying) sharedPlayer.pause() else sharedPlayer.play()
                                            isPlaying = sharedPlayer.isPlaying
                                        },
                                        modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            showMiniControls = false
                                            previewChannel?.let { onChannelClick(it, filteredChannels) }
                                        },
                                        modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(32.dp))
                                    }

                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(modifier = Modifier.fillMaxSize()) {

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = previewChannel?.name ?: "", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (videoWidth > 0 && videoHeight > 0) {
                                    val badge = when { videoHeight >= 2160 -> "4K"; videoHeight >= 1440 -> "2K"; videoHeight >= 1080 -> "FHD"; videoHeight >= 720 -> "HD"; else -> "SD" }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(badge, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("${videoWidth}x${videoHeight}", color = Color.Gray, fontSize = 10.sp)
                                        if (videoFps > 0f) {
                                            Text("${videoFps.toInt()} FPS", color = Color.Gray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                val isCurrentFav = favoriteIds.contains(previewChannel?.id)

                                IconButton(
                                    onClick = { showEpgDialog = true },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = "EPG",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        previewChannel?.let { ch ->
                                            val newFavorites = if (isCurrentFav) favoriteIds - ch.id else favoriteIds + ch.id
                                            favoriteIds = newFavorites; saveFavorites(context, playlistId, newFavorites)
                                        }
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Favorite",
                                        tint = if (isCurrentFav) Color(0xFFFACC15) else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        var isShowingCategories by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .clickable { isShowingCategories = !isShowingCategories }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = if (isShowingCategories) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu, contentDescription = "Categories", tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = if (isShowingCategories) "Select Category" else selectedCategory, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isShowingCategories) {
                            DarkSearchField(categorySearchQuery, "Search Category") { categorySearchQuery = it }
                        } else {
                            DarkSearchField(channelSearchQuery, "Search Channels") { channelSearchQuery = it }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = if (isShowingCategories) categoryListState else channelListState
                        ) {
                            if (isShowingCategories) {
                                items(filteredCategories) { cat ->
                                    val isSelected = selectedCategory == cat
                                    val isPlaying = previewChannel?.category == cat

                                    val count = when(cat) {
                                        "ALL" -> channels.size
                                        "FAVORITES" -> favoriteIds.size
                                        else -> channels.count { it.category == cat }
                                    }.toString()

                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { selectedCategory = cat; isShowingCategories = false }
                                            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (cat == "FAVORITES") {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }

                                        Text(
                                            text = cat,
                                            color = if (isSelected || isPlaying) Color.White else Color.Gray,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Text(count, color = Color.Gray, fontSize = 12.sp)

                                        if (isPlaying) {
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Box(modifier = Modifier.width(3.dp).height(14.dp).background(Color(0xFFE91E63)))
                                        }
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                                }
                            } else {
                                items(filteredChannels) { channel ->
                                    val isPreviewed = previewChannel?.id == channel.id
                                    val isFavorite = favoriteIds.contains(channel.id)

                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { previewChannel = channel }
                                            .background(if (isPreviewed) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(channel.number, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.width(32.dp))

                                        if (isFavorite && selectedCategory != "FAVORITES") {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                                        } else if (!channel.logoUrl.isNullOrBlank()) {
                                            SubcomposeAsyncImage(model = channel.logoUrl, contentDescription = channel.name, modifier = Modifier.size(16.dp).clip(RoundedCornerShape(2.dp)), error = { Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White.copy(alpha = 0.8f)) })
                                        } else {
                                            Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(channel.name, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Program Guide Dialog
    if (showEpgDialog) {
        Dialog(onDismissRequest = { showEpgDialog = false }) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E1E2A)
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Program Guide", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(previewChannel?.name ?: "Unknown Channel", color = Color(0xFFE91E63), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { showEpgDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth().height(350.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isEpgLoading) {
                            CircularProgressIndicator(color = Color(0xFFE91E63), modifier = Modifier.size(24.dp))
                        } else if (epgList.isEmpty()) {
                            Text("No EPG data currently available.", color = Color.Gray, fontSize = 10.sp)
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                items(epgList) { prog ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                        Text("${prog.start}\n${prog.end}", color = Color(0xFFFACC15), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), lineHeight = 12.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(prog.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            if (prog.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(prog.description, color = Color.Gray, fontSize = 9.sp, lineHeight = 11.sp)
                                            }
                                        }
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DarkSearchField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 9.sp),
        cursorBrush = SolidColor(Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = Color.Gray, fontSize = 9.sp)
                }
                innerTextField()
            }
        }
    )
}

// --- SETTINGS SCREEN ---

@Composable
fun SettingsScreen(
    playlistId: String,
    channels: List<Channel>,
    onBack: () -> Unit,
    onChangePlaylist: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var showCreditsDialog by remember { mutableStateOf(false) }

    var catSort by remember { mutableStateOf(getSetting(context, "CAT_SORT", "DEFAULT")) }
    var chanSort by remember { mutableStateOf(getSetting(context, "CHAN_SORT", "DEFAULT")) }
    var updateInterval by remember { mutableLongStateOf(getUpdateInterval(context)) }

    var bgColorSetting by remember { mutableStateOf(getSetting(context, "BG_COLOR", "DARK")) }

    var showHideDialog by remember { mutableStateOf(false) }
    var hiddenCats by remember { mutableStateOf(getHiddenCategories(context)) }

    var showCatReorderDialog by remember { mutableStateOf(false) }
    var showFavReorderDialog by remember { mutableStateOf(false) }

    val allCategories = remember { channels.map { it.category }.distinct() }
    var favoriteIds by remember { mutableStateOf(loadFavorites(context, playlistId)) }

    val rootBgColor = if (bgColorSetting == "BLACK") Color.Black else Color(0xFF070B14)

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier.fillMaxSize().background(rootBgColor).padding(20.dp).verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Settings", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        @Composable
        fun SettingRow(title: String, currentLabel: String, options: List<Pair<String, Any>>, onSelect: (Any) -> Unit) {
            var showDialog by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth().clickable { showDialog = true }.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, fontSize = 12.sp)
                Text(currentLabel, color = Color.Gray, fontSize = 10.sp)
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))

            if (showDialog) {
                Dialog(onDismissRequest = { showDialog = false }) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1E1E2A)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            options.forEach { (label, data) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onSelect(data); showDialog = false }.height(36.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = (currentLabel == label), onClick = { onSelect(data); showDialog = false })
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(label, fontSize = 12.sp, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63), modifier = Modifier.clickable { showDialog = false }.padding(4.dp))
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onChangePlaylist).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Change Playlist", color = Color.White, fontSize = 12.sp)
            Text("Logout", color = Color.Gray, fontSize = 10.sp)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))

        SettingRow("Category Sorting", currentLabel = catSort, options = listOf("DEFAULT" to "DEFAULT", "A-Z" to "A-Z", "Z-A" to "Z-A", "CUSTOM" to "CUSTOM")) { sort ->
            catSort = sort as String
            saveSetting(context, "CAT_SORT", sort)
            if (sort == "CUSTOM") showCatReorderDialog = true
        }

        // Custom Category Reordering UI
        if (catSort == "CUSTOM") {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showCatReorderDialog = true }.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reorder Categories", color = Color(0xFFFACC15), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("EDIT", color = Color.Gray, fontSize = 10.sp)
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
        }

        SettingRow("Live Channel Sorting", currentLabel = chanSort, options = listOf("DEFAULT" to "DEFAULT", "A-Z" to "A-Z", "Z-A" to "Z-A")) { sort ->
            chanSort = sort as String; saveSetting(context, "CHAN_SORT", sort)
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { showFavReorderDialog = true }.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reorder Favorite Channels", color = Color.White, fontSize = 12.sp)
            Text("CUSTOM", color = Color.Gray, fontSize = 10.sp)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))

        Row(
            modifier = Modifier.fillMaxWidth().clickable { showHideDialog = true }.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hide Categories", color = Color.White, fontSize = 12.sp)
            Text("${hiddenCats.size} Hidden", color = Color.Gray, fontSize = 10.sp)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))

        val intervalLabel = when(updateInterval) { 0L -> "Everytime" ; 3600000L -> "Every Hour" ; 172800000L -> "Every 2 Days" ; else -> "Everyday" }
        SettingRow("Update Playlist Time", currentLabel = intervalLabel, options = listOf("Everytime" to 0L, "Every Hour" to 3600000L, "Everyday" to 86400000L, "Every 2 Days" to 172800000L)) { ms ->
            updateInterval = ms as Long; saveUpdateInterval(context, ms)
        }

        SettingRow("Background Color", currentLabel = bgColorSetting, options = listOf("DARK" to "DARK", "BLACK" to "BLACK")) { color ->
            bgColorSetting = color as String
            saveSetting(context, "BG_COLOR", color)
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { showCreditsDialog = true }.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Credits", color = Color.White, fontSize = 12.sp)
            Icon(Icons.Default.Code, contentDescription = "Credits", tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
    }

    // Credits & Open Source Dialog
    if (showCreditsDialog) {
        Dialog(onDismissRequest = { showCreditsDialog = false }) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1E1E2A)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.simple1024),
                            contentDescription = "App Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("SimpleIPTV", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "This app is strictly a media player. It does not provide, include, or sell any playlists or streams.",
                        color = Color.Gray.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SimpleIPTV is free and open-source.",
                        color = Color(0xFFE91E63).copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { uriHandler.openUri("https://github.com/mikailakar/SimpleIPTV") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = "GitHub", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Source Code", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Close",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.clickable { showCreditsDialog = false }.padding(8.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun <T> ReorderDialog(title: String, items: List<T>, itemLabel: (T) -> String, onDismiss: () -> Unit, onSave: (List<T>) -> Unit) {
        var currentList by remember { mutableStateOf(items) }

        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1E1E2A)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63), modifier = Modifier.clickable { onSave(currentList); onDismiss() }.padding(4.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(currentList.size) { index ->
                            val item = currentList[index]
                            Row(modifier = Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(itemLabel(item), fontSize = 12.sp, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f))
                                Row {
                                    IconButton(
                                        onClick = { if (index > 0) { val list = currentList.toMutableList(); val temp = list[index]; list[index] = list[index-1]; list[index-1] = temp; currentList = list } },
                                        modifier = Modifier.size(24.dp)
                                    ) { Icon(Icons.Default.KeyboardArrowUp, null, tint = if (index > 0) Color.White else Color.Gray) }
                                    IconButton(
                                        onClick = { if (index < currentList.size - 1) { val list = currentList.toMutableList(); val temp = list[index]; list[index] = list[index+1]; list[index+1] = temp; currentList = list } },
                                        modifier = Modifier.size(24.dp)
                                    ) { Icon(Icons.Default.KeyboardArrowDown, null, tint = if (index < currentList.size - 1) Color.White else Color.Gray) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCatReorderDialog) {
        val savedOrder = getCustomCategoryOrder(context)
        val initialList = savedOrder.filter { allCategories.contains(it) } + allCategories.filter { !savedOrder.contains(it) }

        ReorderDialog(title = "Reorder Categories", items = initialList, itemLabel = { it }, onDismiss = { showCatReorderDialog = false }) { newList ->
            saveCustomCategoryOrder(context, newList)
        }
    }

    if (showFavReorderDialog) {
        val favoriteChannels = favoriteIds.mapNotNull { id -> channels.find { it.id == id } }
        ReorderDialog(title = "Reorder Favorites", items = favoriteChannels, itemLabel = { it.name }, onDismiss = { showFavReorderDialog = false }) { newList ->
            val newIds = newList.map { it.id }
            favoriteIds = newIds
            saveFavorites(context, playlistId, newIds)
        }
    }

    if (showHideDialog) {
        Dialog(onDismissRequest = { showHideDialog = false }) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1E1E2A)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Hide Categories", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63), modifier = Modifier.clickable { showHideDialog = false }.padding(4.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(allCategories) { cat ->
                            val isHidden = hiddenCats.contains(cat)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val newSet = if (isHidden) hiddenCats - cat else hiddenCats + cat
                                    hiddenCats = newSet
                                    saveHiddenCategories(context, newSet)
                                }.height(36.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isHidden, onCheckedChange = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(cat, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- FULLSCREEN VIDEO PLAYER ---

@Composable
fun VideoPlayerScreen(
    playlistId: String,
    sharedPlayer: ExoPlayer,
    currentChannel: Channel?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onNextChannel: () -> Unit,
    onPrevChannel: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()

    var previousVolume by remember { mutableFloatStateOf(maxVolume / 3f) }

    DisposableEffect(context) {
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            volumeReceiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
            ContextCompat.RECEIVER_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(volumeReceiver)
        }
    }

    var brightness by remember { mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f) }

    var isPlaying by remember { mutableStateOf(sharedPlayer.isPlaying) }
    var showControls by remember { mutableStateOf(true) }
    var localIsFavorite by remember(currentChannel, isFavorite) { mutableStateOf(isFavorite) }

    var showEpgDialog by remember { mutableStateOf(false) }
    val playlists = remember { loadPlaylists(context) }
    val currentPlaylist = remember(playlistId) { playlists.find { it.id == playlistId } }
    var epgList by remember { mutableStateOf<List<EpgProgram>>(emptyList()) }
    var isEpgLoading by remember { mutableStateOf(false) }

    LaunchedEffect(showEpgDialog, currentChannel) {
        if (showEpgDialog) {
            isEpgLoading = true
            epgList = fetchXtreamEpg(currentPlaylist, currentChannel)
            isEpgLoading = false
        }
    }

    var resizeMode by remember { mutableIntStateOf(getSavedResizeMode(context)) }

    DisposableEffect(sharedPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
            }
        }
        sharedPlayer.addListener(listener)

        onDispose {
            sharedPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var videoFps by remember { mutableFloatStateOf(0f) }
    var videoResolution by remember { mutableStateOf("SD") }

    LaunchedEffect(sharedPlayer) {
        while (true) {
            val format = sharedPlayer.videoFormat
            if (format != null) {
                videoWidth = format.width.takeIf { it != -1 } ?: 0
                videoHeight = format.height.takeIf { it != -1 } ?: 0
                videoFps = format.frameRate.takeIf { it != -1f } ?: 0f
                videoResolution = when {
                    videoHeight >= 2160 -> "4K"
                    videoHeight >= 1440 -> "2K"
                    videoHeight >= 1080 -> "FHD"
                    videoHeight >= 720 -> "HD"
                    videoHeight > 0 -> "SD"
                    else -> ""
                }
            } else {
                videoWidth = 0; videoHeight = 0; videoResolution = ""
            }
            delay(1000)
        }
    }

    LaunchedEffect(currentChannel) {
        currentChannel?.let { channel ->
            val currentPlayingUrl = sharedPlayer.currentMediaItem?.mediaId
            if (currentPlayingUrl != channel.streamUrl) {
                val mediaItem = MediaItem.Builder()
                    .setUri(channel.streamUrl)
                    .setMediaId(channel.streamUrl)
                    .build()

                sharedPlayer.setMediaItem(mediaItem)
                sharedPlayer.prepare()
                sharedPlayer.playWhenReady = true
                isPlaying = true
            }
        }
    }

    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showControls = !showControls }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(context).apply {
                    player = sharedPlayer
                    useController = false
                    this.resizeMode = resizeMode
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
            }
        )

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        IconButton(onClick = { showEpgDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "EPG", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                resizeMode = when(resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                                saveResizeMode(context, resizeMode)
                            }
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Resize", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                localIsFavorite = !localIsFavorite
                                onToggleFavorite()
                            }
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Favorite", tint = if (localIsFavorite) Color(0xFFFACC15) else Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.CenterStart),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.BrightnessMedium, contentDescription = "Brightness", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.height(160.dp).width(40.dp), contentAlignment = Alignment.Center) {
                        Slider(
                            value = brightness,
                            onValueChange = {
                                brightness = it
                                val layoutParams = activity?.window?.attributes
                                layoutParams?.screenBrightness = it
                                activity?.window?.attributes = layoutParams
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .requiredWidth(160.dp)
                                .rotate(-90f),
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFE91E63))
                        )
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevChannel, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.fillMaxSize())
                    }
                    IconButton(
                        onClick = {
                            if (sharedPlayer.isPlaying) sharedPlayer.pause() else sharedPlayer.play()
                            isPlaying = sharedPlayer.isPlaying
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.fillMaxSize())
                    }
                    IconButton(onClick = onNextChannel, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.fillMaxSize())
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    IconButton(
                        onClick = {
                            if (volume > 0f) {
                                previousVolume = volume
                                volume = 0f
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                            } else {
                                val restoreVol = if (previousVolume > 0f) previousVolume else (maxVolume / 3f)
                                volume = restoreVol
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restoreVol.toInt(), 0)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (volume > 0f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Mute/Unmute",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.height(160.dp).width(40.dp), contentAlignment = Alignment.Center) {
                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
                            },
                            valueRange = 0f..maxVolume,
                            modifier = Modifier
                                .requiredWidth(160.dp)
                                .rotate(-90f),
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFE91E63))
                        )
                    }
                }

                if (isLandscape) {

                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (!currentChannel?.logoUrl.isNullOrBlank()) {
                                SubcomposeAsyncImage(
                                    model = currentChannel.logoUrl,
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                                    error = { Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column {
                                Text(currentChannel?.category ?: "UNKNOWN", color = Color(0xFFE91E63), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(currentChannel?.number ?: "", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(currentChannel?.name ?: "Unknown Channel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconButton(onClick = { TrackSelectionDialogBuilder(context, "Subtitles", sharedPlayer, C.TRACK_TYPE_TEXT).setTheme(android.R.style.Theme_DeviceDefault_Dialog).setShowDisableOption(true).build().show() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                            }
                            IconButton(onClick = { TrackSelectionDialogBuilder(context, "Audio Track", sharedPlayer, C.TRACK_TYPE_AUDIO).setTheme(android.R.style.Theme_DeviceDefault_Dialog).build().show() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Audiotrack, contentDescription = "Audio Track", tint = Color.White)
                            }
                            if (videoWidth > 0 && videoHeight > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(videoResolution, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("${videoWidth}x${videoHeight}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    if (videoFps > 0f) {
                                        Text("${videoFps.toInt()} FPS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                } else {

                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (!currentChannel?.logoUrl.isNullOrBlank()) {
                                SubcomposeAsyncImage(
                                    model = currentChannel.logoUrl,
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                                    error = { Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Column {
                                Text(currentChannel?.category ?: "UNKNOWN", color = Color(0xFFE91E63), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(currentChannel?.number ?: "", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(currentChannel?.name ?: "Unknown Channel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconButton(onClick = { TrackSelectionDialogBuilder(context, "Subtitles", sharedPlayer, C.TRACK_TYPE_TEXT).setTheme(android.R.style.Theme_DeviceDefault_Dialog).setShowDisableOption(true).build().show() }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                                }
                                IconButton(onClick = { TrackSelectionDialogBuilder(context, "Audio Track", sharedPlayer, C.TRACK_TYPE_AUDIO).setTheme(android.R.style.Theme_DeviceDefault_Dialog).build().show() }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Audiotrack, contentDescription = "Audio Track", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (videoWidth > 0 && videoHeight > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(videoResolution, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("${videoWidth}x${videoHeight}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    if (videoFps > 0f) {
                                        Text("${videoFps.toInt()} FPS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEpgDialog) {
        Dialog(onDismissRequest = { showEpgDialog = false }) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E1E2A)
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Program Guide", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(currentChannel?.name ?: "Unknown Channel", color = Color(0xFFE91E63), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { showEpgDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth().height(350.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isEpgLoading) {
                            CircularProgressIndicator(color = Color(0xFFE91E63), modifier = Modifier.size(24.dp))
                        } else if (epgList.isEmpty()) {
                            Text("No EPG data currently available.", color = Color.Gray, fontSize = 10.sp)
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                items(epgList) { prog ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                        Text("${prog.start}\n${prog.end}", color = Color(0xFFFACC15), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), lineHeight = 12.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(prog.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            if (prog.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(prog.description, color = Color.Gray, fontSize = 9.sp, lineHeight = 11.sp)
                                            }
                                        }
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- DIALOG COMPONENTS ---

@Composable
fun AddPlaylistDialog(initialPlaylist: Playlist?, onDismiss: () -> Unit, onSave: (Playlist) -> Unit) {
    val initialTab = if (initialPlaylist?.type == "M3U") 1 else 0
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var name by remember { mutableStateOf(initialPlaylist?.name ?: "") }
    var url by remember { mutableStateOf(initialPlaylist?.url ?: "") }
    var username by remember { mutableStateOf(initialPlaylist?.username ?: "") }
    var password by remember { mutableStateOf(initialPlaylist?.password ?: "") }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val dialogWidth = if (isLandscape) 0.6f else 0.9f

        Card(modifier = Modifier.fillMaxWidth(dialogWidth), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330))) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(if (initialPlaylist == null) "ADD PLAYLIST" else "EDIT PLAYLIST", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TabButton("XTREAM-CODES-API", isActive = selectedTab == 0, modifier = Modifier.weight(1f)) { selectedTab = 0 }
                    TabButton("ADD M3U URL", isActive = selectedTab == 1, modifier = Modifier.weight(1f)) { selectedTab = 1 }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    if (isLandscape) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DarkTextField(value = name, label = "Playlist Name", placeholder = "name", modifier = Modifier.weight(1f), onValueChange = { name = it })
                            DarkTextField(value = url, label = "URL + Port", placeholder = "http://server:8080", modifier = Modifier.weight(1f), onValueChange = { url = it })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DarkTextField(value = username, label = "Username", placeholder = "Username", modifier = Modifier.weight(1f), onValueChange = { username = it })
                            DarkTextField(value = password, label = "Password", placeholder = "Password", modifier = Modifier.weight(1f), onValueChange = { password = it })
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            DarkTextField(value = name, label = "Playlist Name", placeholder = "name", modifier = Modifier.fillMaxWidth(), onValueChange = { name = it })
                            DarkTextField(value = url, label = "URL + Port", placeholder = "http://server:8080", modifier = Modifier.fillMaxWidth(), onValueChange = { url = it })
                            DarkTextField(value = username, label = "Username", placeholder = "Username", modifier = Modifier.fillMaxWidth(), onValueChange = { username = it })
                            DarkTextField(value = password, label = "Password", placeholder = "Password", modifier = Modifier.fillMaxWidth(), onValueChange = { password = it })
                        }
                    }
                } else {
                    if (isLandscape) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DarkTextField(value = name, label = "Playlist Name", placeholder = "name", modifier = Modifier.weight(1f), onValueChange = { name = it })
                            DarkTextField(value = url, label = "Enter M3U", placeholder = "http://server/playlist.m3u", modifier = Modifier.weight(1f), onValueChange = { url = it })
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            DarkTextField(value = name, label = "Playlist Name", placeholder = "name", modifier = Modifier.fillMaxWidth(), onValueChange = { name = it })
                            DarkTextField(value = url, label = "Enter M3U", placeholder = "http://server/playlist.m3u", modifier = Modifier.fillMaxWidth(), onValueChange = { url = it })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && url.isNotBlank()) {
                                val playlistId = initialPlaylist?.id ?: UUID.randomUUID().toString()

                                if (selectedTab == 0) {
                                    onSave(Playlist(playlistId, name, "XTREAM", url, username, password))
                                } else {
                                    val smartPlaylist = attemptXtreamExtraction(name, url, playlistId)

                                    if (smartPlaylist != null) {
                                        onSave(smartPlaylist)
                                    } else {
                                        onSave(Playlist(playlistId, name, "M3U", url, "", ""))
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                    ) {
                        Text("SAVE PLAYLIST", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333A47))
                    ) {
                        Text("CANCEL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(text: String, isActive: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable { onClick() }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = text, color = if (isActive) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (isActive) Color(0xFFFACC15) else Color.Transparent))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkTextField(
    value: String,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray, fontSize = 12.sp) },
            singleLine = true,

            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF5A6270),
                unfocusedBorderColor = Color(0xFF333A47),
                focusedContainerColor = Color(0xFF161A23),
                unfocusedContainerColor = Color(0xFF161A23),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}