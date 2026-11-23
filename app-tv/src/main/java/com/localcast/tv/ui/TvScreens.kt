package com.localcast.tv.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.localcast.core.model.InstallStatus
import com.localcast.core.model.MediaType
import com.localcast.core.network.client.ConnectionState
import com.localcast.core.network.storage.LastPlayedMedia
import com.localcast.core.network.storage.PlayedMediaItem
import com.localcast.core.network.storage.SettingsRepository
import com.localcast.tv.playback.AudioTrackInfo
import com.localcast.tv.ui.theme.TvAccentBlue
import com.localcast.tv.ui.theme.TvAccentCyan
import com.localcast.tv.ui.theme.TvAccentGreen
import com.localcast.tv.ui.theme.TvAccentPrimary
import com.localcast.tv.ui.theme.TvAccentRed
import com.localcast.tv.ui.theme.TvBackground
import com.localcast.tv.ui.theme.TvBrandGradient
import com.localcast.tv.ui.theme.TvBrandIndigo
import com.localcast.tv.ui.theme.TvBrandPurple
import com.localcast.tv.ui.theme.TvCardGradient
import com.localcast.tv.ui.theme.TvStreamGradient
import com.localcast.tv.ui.theme.TvSurface
import com.localcast.tv.ui.theme.TvSurfaceVariant
import com.localcast.tv.ui.theme.TvTextPrimary
import com.localcast.tv.ui.theme.TvTextSecondary


@Composable
fun TvMainScreen(
    viewModel: TvMainViewModel,
    onPlayerViewCreated: (PlayerView?) -> Unit = {}
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val photoUrl by viewModel.photoUrl.collectAsState()
    val currentMedia by viewModel.currentMedia.collectAsState()
    val receivedApks by viewModel.receivedApks.collectAsState()
    val activeDownloadingApk = receivedApks.firstOrNull { 
        it.status == InstallStatus.DOWNLOADING || it.status == InstallStatus.VERIFYING 
    }

    var lastBackPressTime by remember { mutableStateOf(0L) }
    var showExitPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(lastBackPressTime) {
        if (lastBackPressTime > 0L) {
            showExitPrompt = true
            kotlinx.coroutines.delay(2000L)
            showExitPrompt = false
        }
    }

    // 1. If currently in fullscreen mode or media/photo is active, Back exits player mode to Now Playing screen
    BackHandler(enabled = isFullscreen || currentMedia != null || photoUrl != null) {
        viewModel.stopPlayback()
    }

    // 2. If on a secondary tab, Back returns to Player tab
    BackHandler(enabled = !isFullscreen && currentMedia == null && photoUrl == null && selectedTab != TvTab.PLAYER) {
        viewModel.selectTab(TvTab.PLAYER)
    }

    // 3. Root Now Playing screen: Press back again to exit
    BackHandler(enabled = !isFullscreen && currentMedia == null && photoUrl == null && selectedTab == TvTab.PLAYER) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
        }
    }

    val isPlayerActive = isFullscreen || currentMedia != null || photoUrl != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AnimatedContent(
            targetState = isPlayerActive,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = LinearOutSlowInEasing
                    )
                ) togetherWith fadeOut(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutLinearInEasing
                    )
                )
            },
            label = "tv_player_navigation_transition",
            modifier = Modifier.fillMaxSize()
        ) { inPlayer ->
            if (inPlayer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    TvPlayerScreen(viewModel, onPlayerViewCreated)
                }
            } else {
                val infiniteTransition = rememberInfiniteTransition(label = "tv_aurora")
                val auroraPulse by infiniteTransition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(6500, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "aurora_pulse"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TvBackground)
                        .drawBehind {
                            val baseRadius = size.width * 0.55f * auroraPulse
                            // Primary aurora glow in purple shade on the left top most corner
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF9333EA).copy(alpha = 0.40f),
                                        Color(0xFF7C3AED).copy(alpha = 0.22f),
                                        Color(0xFF581C87).copy(alpha = 0.08f),
                                        Color.Transparent
                                    ),
                                    center = Offset(0f, 0f),
                                    radius = baseRadius
                                )
                            )
                            // Secondary diffuse aurora band extending along top-left
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFA855F7).copy(alpha = 0.18f),
                                        Color(0xFF6D28D9).copy(alpha = 0.07f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.16f, 0f),
                                    radius = size.width * 0.42f * auroraPulse
                                )
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp, bottom = 24.dp, end = 32.dp, start = 84.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    TvTab.PLAYER -> "Now Playing"
                                    TvTab.APKS -> "Received APKs"
                                    TvTab.CONNECTION -> "Device Pairing & Wi-Fi"
                                    TvTab.SETTINGS -> "Settings & Preferences"
                                },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TvTextPrimary
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(TvSurfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "TV IP: ${localIp ?: "No Wi-Fi"}",
                                        fontSize = 13.sp,
                                        color = TvTextSecondary
                                    )
                                }

                                TvClockWidget()

                                ConnectionStatusPill(
                                    connState = connState,
                                    connectedDeviceName = connectedDeviceName,
                                    onRetryClick = { viewModel.reconnect() }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(TvSurface.copy(alpha = 0.82f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(16.dp))
                        ) {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(120)) togetherWith
                                    fadeOut(animationSpec = tween(120))
                                },
                                label = "tab_transition",
                                modifier = Modifier.fillMaxSize()
                            ) { tab ->
                                when (tab) {
                                    TvTab.PLAYER -> {
                                        val resumeError by viewModel.resumeErrorMessage.collectAsState()
                                        val isResuming by viewModel.isResuming.collectAsState()
                                        val playedHistory by viewModel.playedMediaHistory.collectAsState()
                                        val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()

                                        TvStandbyScreen(
                                            viewModel = viewModel,
                                            playedHistory = playedHistory,
                                            isResuming = isResuming,
                                            resumeError = resumeError,
                                            connectedDeviceName = connectedDeviceName
                                        )
                                    }
                                    TvTab.APKS -> TvApksScreen(viewModel)
                                    TvTab.CONNECTION -> TvConnectionScreen(viewModel)
                                    TvTab.SETTINGS -> TvSettingsScreen(viewModel)
                                }
                            }
                        }
                    }

                    // Floating Navigation Rail Overlay (Decoupled from content layout)
                    TvNavRail(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.selectTab(it) },
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            }
        }

        // Top-Right TV Notification HUD for Background APK Transfers
        AnimatedVisibility(
            visible = activeDownloadingApk != null && selectedTab != TvTab.APKS,
            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(200)) { -it },
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200)) { -it },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 28.dp, end = 36.dp)
        ) {
            activeDownloadingApk?.let { apk ->
                Surface(
                    color = Color(0xF0181A26),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, TvAccentCyan),
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { apk.downloadProgress },
                            modifier = Modifier.size(20.dp),
                            color = TvAccentCyan,
                            strokeWidth = 2.5.dp
                        )
                        Column {
                            Text(
                                text = "Transferring ${apk.appName}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (apk.status == InstallStatus.VERIFYING) "Verifying package..." 
                                       else "${(apk.downloadProgress * 100).toInt()}% • Receiving over Wi-Fi",
                                fontSize = 12.sp,
                                color = TvTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // On-screen modern TV exit HUD banner
        AnimatedVisibility(
            visible = showExitPrompt,
            enter = fadeIn(animationSpec = tween(150)) + slideInVertically(animationSpec = tween(150)) { it / 2 },
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Surface(
                color = Color(0xF0181A26),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, TvAccentCyan.copy(alpha = 0.6f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = TvAccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Press back again to exit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}



@Composable
fun TvPlayerScreen(
    viewModel: TvMainViewModel,
    onPlayerViewCreated: (PlayerView?) -> Unit = {}
) {
    val photoUrl by viewModel.photoUrl.collectAsState()
    val media by viewModel.currentMedia.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val resumeError by viewModel.resumeErrorMessage.collectAsState()
    val isResuming by viewModel.isResuming.collectAsState()
    val playedHistory by viewModel.playedMediaHistory.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val audioTracks by viewModel.audioTracks.collectAsState()

    // Latch onto last active media/photo to prevent black flashes during the 300ms exit fade
    var lastActiveMedia by remember { mutableStateOf(media) }
    LaunchedEffect(media) {
        if (media != null) lastActiveMedia = media
    }
    val activeMedia = media ?: lastActiveMedia

    var lastActivePhoto by remember { mutableStateOf(photoUrl) }
    LaunchedEffect(photoUrl) {
        if (photoUrl != null) lastActivePhoto = photoUrl
    }
    val activePhoto = photoUrl ?: lastActivePhoto

    val isAudio = activeMedia != null && (activeMedia.mediaType == MediaType.AUDIO || activeMedia.mimeType.startsWith("audio/"))

    DisposableEffect(Unit) {
        onDispose {
            onPlayerViewCreated(null)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (activePhoto != null) {
            TvPhotoPlayerView(
                photoUrl = activePhoto,
                title = activeMedia?.title ?: "Photo",
                onClose = { viewModel.stopPlayback() }
            )
        } else if (isAudio) {
            TvAudioPlayerView(
                media = activeMedia!!,
                playbackState = playbackState,
                isFullscreen = isFullscreen,
                viewModel = viewModel
            )
        } else if (activeMedia != null) {
            com.localcast.tv.ui.player.TvNetflixPlayerScreen(
                media = activeMedia,
                playbackState = playbackState,
                audioTracks = audioTracks,
                viewModel = viewModel,
                onPlayerViewCreated = onPlayerViewCreated
            )
        } else {
            TvStandbyScreen(
                viewModel = viewModel,
                playedHistory = playedHistory,
                isResuming = isResuming,
                resumeError = resumeError,
                connectedDeviceName = connectedDeviceName
            )
        }
    }
}

