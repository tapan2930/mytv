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
fun TvAudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_transition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_phase"
    )

    Canvas(
        modifier = modifier
            .height(44.dp)
            .width(170.dp)
    ) {
        val barCount = 18
        val barWidth = 4.dp.toPx()
        val spacing = (size.width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)
        val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        val brush = Brush.verticalGradient(
            listOf(
                TvAccentCyan,
                TvBrandPurple,
                TvBrandIndigo
            )
        )

        for (i in 0 until barCount) {
            val fraction = if (isPlaying) {
                (Math.sin((phase + i * 0.42).toDouble()).toFloat() * 0.4f +
                 Math.cos((phase * 1.6 + i * 0.65).toDouble()).toFloat() * 0.35f +
                 0.65f).coerceIn(0.18f, 1.0f)
            } else {
                0.12f
            }

            val barHeight = size.height * fraction
            val left = i * (barWidth + spacing)
            val top = size.height - barHeight

            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}

@Composable
fun TvVinylRecordView(
    isPlaying: Boolean,
    title: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier.size(230.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TvBrandPurple.copy(alpha = 0.4f), Color.Transparent),
                    radius = size.width / 2f
                )
            )
        }

        Box(
            modifier = Modifier
                .size(210.dp)
                .graphicsLayer { rotationZ = currentRotation }
                .clip(CircleShape)
                .background(Color(0xFF13141F))
                .border(width = 3.dp, color = Color(0xFF26283B), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.width / 2f
                listOf(0.92f, 0.82f, 0.72f, 0.62f, 0.52f).forEach { rFraction ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = maxR * rFraction,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(TvBrandGradient)
                    .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Audiotrack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(TvBackground)
                )
            }
        }
    }
}

@Composable
fun TvPhotoPlayerView(
    photoUrl: String,
    title: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Ambient backdrop: fills the entire 16:9 TV screen eliminating pillarbox voids
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.28f }
        )

        // Radial shadow overlay focusing eyes on center
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        radius = 1200f
                    )
                )
        )

        // Sharp centered photo with subtle border
        AsyncImage(
            model = photoUrl,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize(0.92f)
                .clip(RoundedCornerShape(8.dp))
        )

        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, TvAccentCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(32.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Photo, contentDescription = null, tint = TvAccentCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Casting Photo: $title",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        var isCloseFocused by remember { mutableStateOf(false) }
        val closeScale by animateFloatAsState(
            targetValue = if (isCloseFocused) 1.08f else 1.0f,
            animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
            label = "photo_close_scale"
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = closeScale
                    scaleY = closeScale
                }
                .clip(CircleShape)
                .background(if (isCloseFocused) TvAccentRed else Color.Black.copy(alpha = 0.65f))
                .border(2.dp, if (isCloseFocused) Color.White else Color.Transparent, CircleShape)
                .onFocusChanged { isCloseFocused = it.isFocused }
                .focusable()
                .clickable { onClose() }
                .padding(10.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun TvAudioPlayerView(
    media: com.localcast.core.model.MediaItem,
    playbackState: com.localcast.core.model.PlaybackState,
    isFullscreen: Boolean,
    viewModel: TvMainViewModel
) {
    val pos = playbackState.positionMs
    val dur = if (playbackState.durationMs > 0) playbackState.durationMs else media.durationMs
    val posMin = (pos / 1000) / 60
    val posSec = (pos / 1000) % 60
    val durMin = (dur / 1000) / 60
    val durSec = (dur / 1000) % 60
    val progressFraction = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
    val isPlaying = playbackState.isPlaying

    val playFocusRequester = remember { FocusRequester() }

    LaunchedEffect(media.id) {
        kotlinx.coroutines.delay(100)
        try {
            playFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(TvSurfaceVariant, TvBackground),
                    radius = 1200f
                )
            )
            .padding(horizontal = 48.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvVinylRecordView(
                isPlaying = isPlaying,
                title = media.title
            )

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = TvAccentCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "AUDIO STREAM • ${media.mimeType.substringAfter('/').uppercase()}",
                        color = TvAccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = media.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TvTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val artistText = listOfNotNull(media.artist, media.album).filter { it.isNotBlank() }.joinToString(" • ")
                if (artistText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = artistText,
                        fontSize = 16.sp,
                        color = TvTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                TvAudioVisualizer(
                    isPlaying = isPlaying
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Interactive Audio Scrubber with D-pad navigation
        var isScrubberFocused by remember { mutableStateOf(false) }
        val scrubberScale by animateFloatAsState(
            targetValue = if (isScrubberFocused) 1.02f else 1.0f,
            animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
            label = "audio_scrubber_scale"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .graphicsLayer {
                    scaleX = scrubberScale
                    scaleY = scrubberScale
                }
                .clip(RoundedCornerShape(10.dp))
                .background(if (isScrubberFocused) TvSurfaceVariant.copy(alpha = 0.6f) else Color.Transparent)
                .border(
                    width = if (isScrubberFocused) 1.5.dp else 0.dp,
                    color = if (isScrubberFocused) TvAccentCyan else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                )
                .onFocusChanged { isScrubberFocused = it.isFocused }
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                viewModel.playerManager.seekBy(-10000)
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                viewModel.playerManager.seekBy(10000)
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                .focusable()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val fullWidth = maxWidth
                // Background Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(TvSurfaceVariant)
                )

                // Filled Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .then(
                            if (isScrubberFocused) Modifier.background(TvBrandGradient)
                            else Modifier.background(TvAccentCyan)
                        )
                )

                // Interactive Scrubber Thumb
                val thumbOffset = fullWidth * progressFraction - 7.dp
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset.coerceAtLeast(0.dp))
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isScrubberFocused) Color.White else TvAccentCyan)
                        .border(
                            2.dp,
                            if (isScrubberFocused) TvAccentCyan else Color.White,
                            CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "%02d:%02d".format(posMin, posSec),
                    fontSize = 13.sp,
                    fontWeight = if (isScrubberFocused) FontWeight.Bold else FontWeight.Normal,
                    color = if (isScrubberFocused) TvAccentCyan else TvTextSecondary
                )
                if (isScrubberFocused) {
                    Text(
                        text = "◄ Left / Right to scrub ►",
                        fontSize = 12.sp,
                        color = TvAccentCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "%02d:%02d".format(durMin, durSec),
                    fontSize = 13.sp,
                    color = TvTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var isRewFocused by remember { mutableStateOf(false) }
            val rewScale by animateFloatAsState(
                targetValue = if (isRewFocused) 1.08f else 1.0f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                label = "rew_scale"
            )
            val rewBorderColor by animateColorAsState(
                targetValue = if (isRewFocused) Color.White else Color.Transparent,
                animationSpec = tween(110),
                label = "rew_border"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = rewScale
                        scaleY = rewScale
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isRewFocused) TvBrandIndigo else TvSurfaceVariant)
                    .border(2.dp, rewBorderColor, RoundedCornerShape(10.dp))
                    .onFocusChanged { isRewFocused = it.isFocused }
                    .focusable()
                    .clickable { viewModel.playerManager.seekBy(-10000) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FastRewind, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("-10s", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            var isPlayFocused by remember { mutableStateOf(false) }
            val playScale by animateFloatAsState(
                targetValue = if (isPlayFocused) 1.10f else 1.0f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                label = "play_scale"
            )
            val playBorderColor by animateColorAsState(
                targetValue = if (isPlayFocused) Color.White else Color.Transparent,
                animationSpec = tween(110),
                label = "play_border"
            )

            Box(
                modifier = Modifier
                    .focusRequester(playFocusRequester)
                    .graphicsLayer {
                        scaleX = playScale
                        scaleY = playScale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isPlayFocused) Modifier.background(TvBrandGradient)
                        else Modifier.background(TvAccentCyan)
                    )
                    .border(2.5.dp, playBorderColor, RoundedCornerShape(12.dp))
                    .onFocusChanged { isPlayFocused = it.isFocused }
                    .focusable()
                    .clickable { viewModel.playerManager.togglePlayPause() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = TvBackground,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying) "Pause" else "Play",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvBackground
                    )
                }
            }

            var isFfFocused by remember { mutableStateOf(false) }
            val ffScale by animateFloatAsState(
                targetValue = if (isFfFocused) 1.08f else 1.0f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                label = "ff_scale"
            )
            val ffBorderColor by animateColorAsState(
                targetValue = if (isFfFocused) Color.White else Color.Transparent,
                animationSpec = tween(110),
                label = "ff_border"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = ffScale
                        scaleY = ffScale
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isFfFocused) TvBrandIndigo else TvSurfaceVariant)
                    .border(2.dp, ffBorderColor, RoundedCornerShape(10.dp))
                    .onFocusChanged { isFfFocused = it.isFocused }
                    .focusable()
                    .clickable { viewModel.playerManager.seekBy(10000) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FastForward, contentDescription = "Fast Forward", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+10s", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            var isStopFocused by remember { mutableStateOf(false) }
            val stopScale by animateFloatAsState(
                targetValue = if (isStopFocused) 1.08f else 1.0f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                label = "stop_scale"
            )
            val stopBorderColor by animateColorAsState(
                targetValue = if (isStopFocused) Color.White else Color.Transparent,
                animationSpec = tween(110),
                label = "stop_border"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = stopScale
                        scaleY = stopScale
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isStopFocused) TvAccentRed else TvSurfaceVariant)
                    .border(2.dp, stopBorderColor, RoundedCornerShape(10.dp))
                    .onFocusChanged { isStopFocused = it.isFocused }
                    .focusable()
                    .clickable { viewModel.stopPlayback() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Close, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

