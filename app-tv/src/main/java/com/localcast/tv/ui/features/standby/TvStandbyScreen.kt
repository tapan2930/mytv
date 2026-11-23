package com.localcast.tv.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
fun TvStandbyScreen(
    viewModel: TvMainViewModel,
    playedHistory: List<PlayedMediaItem>,
    isResuming: Boolean,
    resumeError: String?,
    connectedDeviceName: String?
) {
    val connState by viewModel.connectionState.collectAsState()
    val isProbingSubnet by viewModel.isProbingSubnet.collectAsState()
    val isConnected = connState is ConnectionState.Connected

    val resumeItem = playedHistory.firstOrNull()
    val otherItems = if (playedHistory.size > 1) playedHistory.drop(1) else emptyList()

    if (playedHistory.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            resumeError?.let { err ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TvAccentRed.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(err, fontSize = 14.sp, color = TvAccentRed, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TvAccentRed)
                                    .clickable { viewModel.clearResumeError() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Dismiss", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Header Row: Section Title + Server Pill + Clear Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Continue Watching",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvTextPrimary
                        )

                        // Scoped Server Indicator Pill
                        Surface(
                            color = if (isConnected) TvAccentGreen.copy(alpha = 0.15f) else TvSurfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) TvAccentGreen else TvAccentCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = connectedDeviceName ?: "Connected Phone",
                                    color = if (isConnected) TvAccentGreen else TvTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Minimal Clear List Button
                    var isClearFocused by remember { mutableStateOf(false) }
                    val clearScale by animateFloatAsState(
                        targetValue = if (isClearFocused) 1.08f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                        label = "clear_scale"
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = clearScale
                                scaleY = clearScale
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isClearFocused) TvAccentRed.copy(alpha = 0.35f) else TvSurfaceVariant.copy(alpha = 0.6f))
                            .border(
                                width = if (isClearFocused) 2.dp else 1.dp,
                                color = if (isClearFocused) TvAccentRed else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .onFocusChanged { isClearFocused = it.isFocused }
                            .focusable()
                            .clickable { viewModel.clearHistory() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear List",
                                tint = if (isClearFocused) Color.White else TvTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clear History",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isClearFocused) Color.White else TvTextSecondary
                            )
                        }
                    }
                }
            }

            // 1. Featured Video That Resumes (Thumbnail + Resume Button)
            resumeItem?.let { item ->
                item {
                    TvResumeHeroCard(
                        item = item,
                        viewModel = viewModel,
                        isResuming = isResuming
                    )
                }
            }

            // 2. List View of Other Videos Played / Left
            if (otherItems.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "Other Videos Played (${otherItems.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TvTextSecondary,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(otherItems, key = { it.id + it.streamUrl }) { mediaItem ->
                                TvPlayedMediaCard(
                                    item = mediaItem,
                                    viewModel = viewModel,
                                    isResuming = isResuming
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Fallback Standby View when History is Empty
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            resumeError?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TvAccentRed.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(err, fontSize = 14.sp, color = TvAccentRed, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TvAccentRed)
                                .clickable { viewModel.clearResumeError() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Dismiss", fontSize = 12.sp, color = TvTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            TvDiscoveryRadar(isConnected = isConnected)

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = if (isConnected) TvAccentGreen.copy(alpha = 0.15f) else TvAccentCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) TvAccentGreen else TvAccentCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "Connected to ${connectedDeviceName ?: "Phone"} • Ready to Cast"
                               else "Listening for Phone on Wi-Fi...",
                        color = if (isConnected) TvAccentGreen else TvAccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "Ready to Cast",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open the My TV app on your phone connected to the same Wi-Fi network.",
                fontSize = 15.sp,
                color = TvTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select any Video, Music, Photo, or APK to stream or install instantly.",
                fontSize = 14.sp,
                color = TvTextSecondary
            )

            if (!isConnected) {
                Spacer(modifier = Modifier.height(20.dp))
                var isFastProbeFocused by remember { mutableStateOf(false) }

                val fastProbeScale by animateFloatAsState(
                    targetValue = if (isFastProbeFocused) 1.08f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                    label = "fast_probe_scale"
                )
                val fastProbeBorderColor by animateColorAsState(
                    targetValue = if (isFastProbeFocused) Color.White else TvAccentCyan.copy(alpha = 0.5f),
                    animationSpec = tween(180),
                    label = "fast_probe_border"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = fastProbeScale
                            scaleY = fastProbeScale
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isFastProbeFocused) Modifier.background(TvBrandGradient)
                            else Modifier.background(TvSurfaceVariant)
                        )
                        .border(
                            width = if (isFastProbeFocused) 2.dp else 1.dp,
                            color = fastProbeBorderColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .onFocusChanged { isFastProbeFocused = it.isFocused }
                        .focusable(!isProbingSubnet)
                        .clickable(enabled = !isProbingSubnet) {
                            viewModel.probeSubnet()
                            viewModel.selectTab(TvTab.CONNECTION)
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isProbingSubnet) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Probing Wi-Fi Subnet...", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = if (isFastProbeFocused) Color.White else TvAccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "1-Click Subnet Fast-Probe for Phone",
                                fontSize = 14.sp,
                                color = if (isFastProbeFocused) Color.White else TvTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvResumeHeroCard(
    item: PlayedMediaItem,
    viewModel: TvMainViewModel,
    isResuming: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
        label = "hero_scale"
    )

    val host = if (item.serverHost.isNotBlank()) item.serverHost else (viewModel.connectedHost ?: "127.0.0.1")
    val port = if (viewModel.connectedPort > 0) viewModel.connectedPort else 8844
    val thumbUrl = "http://$host:$port/api/media/thumbnail/${item.id}"

    val posMin = (item.positionMs / 1000) / 60
    val posSec = (item.positionMs / 1000) % 60
    val durMin = (item.durationMs / 1000) / 60
    val durSec = (item.durationMs / 1000) % 60
    val progress = if (item.durationMs > 0) (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val timeLeftSec = ((item.durationMs - item.positionMs).coerceAtLeast(0L)) / 1000
    val timeLeftText = if (timeLeftSec >= 60) "${timeLeftSec / 60}m left" else "${timeLeftSec}s left"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(TvSurfaceVariant.copy(alpha = 0.95f))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(!isResuming)
            .clickable(enabled = !isResuming) { viewModel.playHistoryItem(item) }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 16:9 Thumbnail
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F111A)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlaid Duration Badge on bottom-right
                if (item.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "%02d:%02d".format(durMin, durSec),
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Progress Bar at bottom of thumbnail
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(TvAccentCyan)
                        )
                    }
                }

                // Play icon overlay when card focused
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Resume",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvAccentCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "RESUME",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvAccentCyan
                        )
                    }

                    if (item.durationMs > 0 && progress > 0.95f) {
                        Text(
                            text = "• Completed",
                            fontSize = 13.sp,
                            color = TvAccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (item.durationMs > 0) {
                        Text(
                            text = "• $timeLeftText",
                            fontSize = 13.sp,
                            color = TvTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TvTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (item.durationMs > 0) {
                        "Paused at %02d:%02d of %02d:%02d".format(posMin, posSec, durMin, durSec)
                    } else {
                        "Paused at %02d:%02d".format(posMin, posSec)
                    },
                    fontSize = 14.sp,
                    color = TvTextSecondary
                )
            }

            // Right side action indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isFocused) Color.White else TvBrandIndigo)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isResuming) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            color = if (isFocused) TvBackground else Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Resuming...",
                            fontSize = 14.sp,
                            color = if (isFocused) TvBackground else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (isFocused) TvBackground else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Resume",
                            fontSize = 14.sp,
                            color = if (isFocused) TvBackground else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvPlayedMediaCard(
    item: PlayedMediaItem,
    viewModel: TvMainViewModel,
    isResuming: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
        label = "item_scale"
    )

    val host = if (item.serverHost.isNotBlank()) item.serverHost else (viewModel.connectedHost ?: "127.0.0.1")
    val port = if (viewModel.connectedPort > 0) viewModel.connectedPort else 8844
    val thumbUrl = "http://$host:$port/api/media/thumbnail/${item.id}"

    val durMin = (item.durationMs / 1000) / 60
    val durSec = (item.durationMs / 1000) % 60
    val progress = if (item.durationMs > 0) (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val timeLeftSec = ((item.durationMs - item.positionMs).coerceAtLeast(0L)) / 1000
    val timeLeftText = if (timeLeftSec >= 60) "${timeLeftSec / 60}m left" else "${timeLeftSec}s left"

    Column(
        modifier = Modifier
            .width(175.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(TvSurfaceVariant.copy(alpha = 0.85f))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(!isResuming)
            .clickable(enabled = !isResuming) { viewModel.playHistoryItem(item) }
            .padding(10.dp)
    ) {
        // 16:9 Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F111A)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = thumbUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Duration Pill
            if (item.durationMs > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "%02d:%02d".format(durMin, durSec),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Progress Bar at bottom of thumbnail
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(if (progress > 0.95f) TvAccentGreen else TvAccentCyan)
                    )
                }
            }

            if (isFocused) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = item.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isFocused) Color.White else TvTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Remaining or Completed status
        Text(
            text = if (item.durationMs > 0 && progress > 0.95f) "Completed"
                   else if (item.durationMs > 0) timeLeftText
                   else "Played",
            fontSize = 12.sp,
            color = if (item.durationMs > 0 && progress > 0.95f) TvAccentGreen else TvTextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

