package com.localcast.tv.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.localcast.tv.playback.AudioTrackInfo
import com.localcast.tv.ui.theme.*

// ──────────────────────────────────────────────────────────────
// Component 1: PlayerActionButton (Reusable)
// ──────────────────────────────────────────────────────────────

@Composable
fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    accentColor: Color = TvBrandIndigo,
    iconSize: Dp = 17.dp,
    isFocusable: Boolean = true,
    onDpadUp: (() -> Unit)? = null,
    onDpadDown: (() -> Unit)? = null,
    onDpadLeft: (() -> Unit)? = null,
    onDpadRight: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
        label = "action_btn_scale"
    )

    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) accentColor else TvSurfaceVariant.copy(alpha = 0.85f))
            .border(2.dp, if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(isFocusable)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                            if (onDpadUp != null) {
                                onDpadUp()
                                true
                            } else false
                        }

                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (onDpadDown != null) {
                                onDpadDown()
                                true
                            } else false
                        }

                        android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                        android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                            if (onDpadLeft != null) {
                                onDpadLeft()
                                true
                            } else false
                        }

                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                        android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                            if (onDpadRight != null) {
                                onDpadRight()
                                true
                            } else false
                        }

                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                        android.view.KeyEvent.KEYCODE_BUTTON_A,
                        android.view.KeyEvent.KEYCODE_BUTTON_SELECT -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                enabled = isFocusable,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Component 2: PlayerTopBar
// ──────────────────────────────────────────────────────────────

@Composable
fun PlayerTopBar(
    title: String,
    mimeType: String,
    width: Int,
    height: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF50A0A0A),
                        Color(0xCC0A0A0A),
                        Color(0x660A0A0A),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Wi-Fi Streaming", fontSize = 12.sp, color = TvTextSecondary)
                    Text("•", fontSize = 12.sp, color = TvTextSecondary)
                    Text(
                        text = mimeType.substringAfter('/').uppercase(),
                        fontSize = 12.sp,
                        color = TvTextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TvAccentCyan.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = when {
                            width >= 3840 || height >= 2160 -> "4K UHD"
                            width >= 1920 || height >= 1080 -> "1080p FHD"
                            else -> "HD"
                        },
                        color = TvAccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TvBrandIndigo.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "5.1 AUDIO",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Component 3: PlayerCenterControls
// ──────────────────────────────────────────────────────────────

@Composable
fun PlayerCenterControls(
    isPlaying: Boolean,
    playFocusRequester: FocusRequester,
    onTogglePlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onDpadUp: () -> Unit,
    onDpadDown: () -> Unit,
    modifier: Modifier = Modifier,
    isFocusable: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rewind -10s
        var isRewFocused by remember { mutableStateOf(false) }
        val rewScale by animateFloatAsState(
            targetValue = if (isRewFocused) 1.10f else 1.0f,
            animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
            label = "center_rew_scale"
        )
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = rewScale; scaleY = rewScale }
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isRewFocused) TvSurfaceVariant else Color.White.copy(alpha = 0.1f))
                .border(2.dp, if (isRewFocused) Color.White else Color.Transparent, CircleShape)
                .onFocusChanged { isRewFocused = it.isFocused }
                .focusable(isFocusable)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> { onDpadUp(); true }
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> { onDpadDown(); true }
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER,
                            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                onRewind()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                .clickable { onRewind() },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                Icons.Filled.Replay10,
                contentDescription = "Rewind 10s",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Hero Play/Pause
        var isPlayFocused by remember { mutableStateOf(false) }
        val playScale by animateFloatAsState(
            targetValue = if (isPlayFocused) 1.12f else 1.0f,
            animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
            label = "center_play_scale"
        )
        Box(
            modifier = Modifier
                .focusRequester(playFocusRequester)
                .graphicsLayer { scaleX = playScale; scaleY = playScale }
                .size(72.dp)
                .clip(CircleShape)
                .then(
                    if (isPlayFocused) Modifier.background(TvBrandGradient)
                    else Modifier.background(Color.White.copy(alpha = 0.15f))
                )
                .border(2.5.dp, if (isPlayFocused) Color.White else Color.White.copy(alpha = 0.3f), CircleShape)
                .onFocusChanged { isPlayFocused = it.isFocused }
                .focusable(isFocusable)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> { onDpadUp(); true }
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> { onDpadDown(); true }
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER,
                            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                onTogglePlayPause()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                .clickable { onTogglePlayPause() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (scaleIn(initialScale = 0.8f, animationSpec = tween(150)) + fadeIn(tween(150)))
                        .togetherWith(scaleOut(targetScale = 0.8f, animationSpec = tween(150)) + fadeOut(tween(150)))
                },
                label = "play_pause_icon"
            ) { playing ->
                androidx.compose.material3.Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Forward +10s
        var isFfFocused by remember { mutableStateOf(false) }
        val ffScale by animateFloatAsState(
            targetValue = if (isFfFocused) 1.10f else 1.0f,
            animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
            label = "center_ff_scale"
        )
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = ffScale; scaleY = ffScale }
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isFfFocused) TvSurfaceVariant else Color.White.copy(alpha = 0.1f))
                .border(2.dp, if (isFfFocused) Color.White else Color.Transparent, CircleShape)
                .onFocusChanged { isFfFocused = it.isFocused }
                .focusable(isFocusable)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> { onDpadUp(); true }
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> { onDpadDown(); true }
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER,
                            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                onForward()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                .clickable { onForward() },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                Icons.Filled.Forward10,
                contentDescription = "Forward 10s",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Component 4a: ScrubPreviewCard (Netflix & YouTube Style Preview)
// ──────────────────────────────────────────────────────────────

@Composable
fun ScrubPreviewCard(
    scrubPositionMs: Long,
    scrubAnchorMs: Long,
    trackWidth: Dp,
    progressFraction: Float,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
) {
    val cardWidth = 168.dp
    val cardHeight = 94.5.dp
    val thumbSize = 18.dp

    // 1. Calculate Thumb Center
    val thumbCenter = (trackWidth - thumbSize) * progressFraction + (thumbSize / 2)

    // 2. Clamp Preview Card X Position within Track Bounds
    val maxCardOffset = (trackWidth - cardWidth).coerceAtLeast(0.dp)
    val idealCardX = thumbCenter - (cardWidth / 2)
    val clampedCardX = idealCardX.coerceIn(0.dp, maxCardOffset)

    // 3. Smooth Horizontal Translation Animation
    val animatedCardX by animateDpAsState(
        targetValue = clampedCardX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "preview_card_glide"
    )

    // 4. Calculate Anchor Pointer Arrow Position Relative to Card
    val arrowXWithinCard = (thumbCenter - animatedCardX).coerceIn(12.dp, cardWidth - 12.dp)

    // 5. Time & Delta Formatting
    val curMin = (scrubPositionMs / 1000) / 60
    val curSec = (scrubPositionMs / 1000) % 60
    val deltaMs = scrubPositionMs - scrubAnchorMs
    val deltaSec = deltaMs / 1000
    val deltaSign = if (deltaSec >= 0) "+" else "-"
    val absSec = kotlin.math.abs(deltaSec)
    val deltaText = "$deltaSign%02d:%02d".format(absSec / 60, absSec % 60)

    // Entrance Animation
    var hasAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasAppeared = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = tween(120),
        label = "preview_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0.85f,
        animationSpec = tween(120),
        label = "preview_scale"
    )

    Column(
        modifier = modifier
            .offset(x = animatedCardX, y = (-cardHeight - 14.dp))
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.Start
    ) {
        // Thumbnail Card Container
        Box(
            modifier = Modifier
                .size(cardWidth, cardHeight)
                .shadow(10.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF141722))
                .border(1.5.dp, TvAccentCyan, RoundedCornerShape(10.dp))
        ) {
            // Coil Video Frame
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl)
                        .crossfade(150)
                        .build(),
                    contentDescription = "Seek preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Scrim & Time Badge at bottom of Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xB3000000),
                                Color(0xF00A0A0A)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "%02d:%02d".format(curMin, curSec),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "[$deltaText]",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (deltaMs >= 0) TvAccentCyan else TvAccentRed
                        )
                    }
                    Text(
                        text = "Press OK to jump",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFDDDDDD)
                    )
                }
            }
        }

        // Downward Anchor Pointer (Arrow Triangle)
        Canvas(
            modifier = Modifier
                .offset(x = arrowXWithinCard - 6.dp)
                .size(12.dp, 6.dp)
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, color = TvAccentCyan)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Component 4: PlayerSeekBar
// ──────────────────────────────────────────────────────────────

@Composable
fun PlayerSeekBar(
    positionMs: Long,
    durationMs: Long,
    bufferedFraction: Float,
    isScrubbing: Boolean,
    scrubPositionMs: Long,
    scrubAnchorMs: Long,
    scrubberFocusRequester: FocusRequester,
    previewThumbnailUrl: String? = null,
    onScrubLeft: () -> Unit,
    onScrubRight: () -> Unit,
    onScrubCommit: () -> Unit,
    onScrubCancel: () -> Unit,
    onDpadUp: () -> Unit,
    onDpadDown: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    isFocusable: Boolean = true
) {
    val displayPos = if (isScrubbing) scrubPositionMs else positionMs
    val dur = durationMs.coerceAtLeast(1L)
    val curMin = (displayPos / 1000) / 60
    val curSec = (displayPos / 1000) % 60
    val remMs = (dur - displayPos).coerceAtLeast(0L)
    val remMin = (remMs / 1000) / 60
    val remSec = (remMs / 1000) % 60
    val progressFraction = (displayPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

    var isScrubberFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "%02d:%02d".format(curMin, curSec),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isScrubbing) TvAccentCyan else Color.White
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .focusRequester(scrubberFocusRequester)
                .onFocusChanged {
                    isScrubberFocused = it.isFocused
                    if (it.isFocused) onInteraction()
                    if (!it.isFocused && isScrubbing) onScrubCancel()
                }
                .focusable(isFocusable)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                            android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                                onScrubLeft()
                                onInteraction()
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                            android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                                onScrubRight()
                                onInteraction()
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER,
                            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                            android.view.KeyEvent.KEYCODE_BUTTON_A,
                            android.view.KeyEvent.KEYCODE_BUTTON_SELECT -> {
                                if (isScrubbing) onScrubCommit()
                                onInteraction()
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                if (isScrubbing) onScrubCancel()
                                onInteraction()
                                onDpadUp()
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (isScrubbing) onScrubCancel()
                                onInteraction()
                                onDpadDown()
                                true
                            }
                            android.view.KeyEvent.KEYCODE_BACK -> {
                                if (isScrubbing) {
                                    onScrubCancel()
                                    onInteraction()
                                    true
                                } else false
                            }
                            else -> false
                        }
                    } else false
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val trackWidth = maxWidth

            // Background Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isScrubberFocused) 8.dp else 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            )

            // Buffered Track
            Box(
                modifier = Modifier
                    .fillMaxWidth(bufferedFraction.coerceIn(0f, 1f))
                    .height(if (isScrubberFocused) 8.dp else 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )

            // Played Progress
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .height(if (isScrubberFocused) 8.dp else 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TvBrandGradient)
            )

            // Thumb
            val thumbSize = if (isScrubberFocused) 18.dp else 10.dp
            val thumbOffset = (trackWidth - thumbSize) * progressFraction

            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = if (isScrubberFocused) 2.5.dp else 0.dp,
                        color = if (isScrubberFocused) TvAccentCyan else Color.Transparent,
                        shape = CircleShape
                    )
            )

            // Floating Scrub Preview (Netflix & YouTube Style)
            if (isScrubbing) {
                ScrubPreviewCard(
                    scrubPositionMs = scrubPositionMs,
                    scrubAnchorMs = scrubAnchorMs,
                    trackWidth = trackWidth,
                    progressFraction = progressFraction,
                    thumbnailUrl = previewThumbnailUrl
                )
            }
        }

        Text(
            text = "-%02d:%02d".format(remMin, remSec),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TvTextSecondary
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Component 5: PlayerBottomBar
// ──────────────────────────────────────────────────────────────

@Composable
fun PlayerBottomBar(
    positionMs: Long,
    durationMs: Long,
    bufferedFraction: Float,
    isScrubbing: Boolean,
    scrubPositionMs: Long,
    scrubAnchorMs: Long,
    playbackSpeed: Float,
    activeAudioLabel: String,
    playFocusRequester: FocusRequester,
    scrubberFocusRequester: FocusRequester,
    audioBtnFocusRequester: FocusRequester,
    previewThumbnailUrl: String? = null,
    speedBtnFocusRequester: FocusRequester? = null,
    onScrubLeft: () -> Unit,
    onScrubRight: () -> Unit,
    onScrubCommit: () -> Unit,
    onScrubCancel: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSpeed: () -> Unit,
    onExternalPlayer: () -> Unit,
    onStop: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    isFocusable: Boolean = true
) {
    val actualSpeedRequester = speedBtnFocusRequester ?: remember { FocusRequester() }
    val vlcBtnFocusRequester = remember { FocusRequester() }
    val stopBtnFocusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0x33000000),
                        Color(0xCC0A0A0A),
                        Color(0xFA0A0A0A)
                    )
                )
            )
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Seekbar
            PlayerSeekBar(
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedFraction = bufferedFraction,
                isScrubbing = isScrubbing,
                scrubPositionMs = scrubPositionMs,
                scrubAnchorMs = scrubAnchorMs,
                scrubberFocusRequester = scrubberFocusRequester,
                previewThumbnailUrl = previewThumbnailUrl,
                onScrubLeft = onScrubLeft,
                onScrubRight = onScrubRight,
                onScrubCommit = onScrubCommit,
                onScrubCancel = onScrubCancel,
                onDpadUp = {
                    onInteraction()
                    try { playFocusRequester.requestFocus() } catch (_: Exception) {}
                },
                onDpadDown = {
                    onInteraction()
                    try { audioBtnFocusRequester.requestFocus() } catch (_: Exception) {}
                },
                onInteraction = onInteraction,
                isFocusable = isFocusable
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio
                PlayerActionButton(
                    icon = Icons.Filled.Audiotrack,
                    label = "Audio$activeAudioLabel",
                    focusRequester = audioBtnFocusRequester,
                    isFocusable = isFocusable,
                    onDpadUp = {
                        onInteraction()
                        try { scrubberFocusRequester.requestFocus() } catch (_: Exception) {}
                    },
                    onDpadRight = {
                        onInteraction()
                        try { actualSpeedRequester.requestFocus() } catch (_: Exception) {}
                    },
                    onClick = onOpenAudio
                )

                // Speed
                val speedLabel = if (playbackSpeed == 1.0f) "1.0×" else "${playbackSpeed}×"
                PlayerActionButton(
                    icon = Icons.Filled.Speed,
                    label = speedLabel,
                    focusRequester = actualSpeedRequester,
                    isFocusable = isFocusable,
                    onDpadUp = {
                        onInteraction()
                        try { scrubberFocusRequester.requestFocus() } catch (_: Exception) {}
                    },
                    onDpadLeft = {
                        onInteraction()
                        try { audioBtnFocusRequester.requestFocus() } catch (_: Exception) {}
                    },
                    onDpadRight = {
                        onInteraction()
                        try { vlcBtnFocusRequester.requestFocus() } catch (_: Exception) {}
                    },
                    onClick = onOpenSpeed
                )

                // External Player
                PlayerActionButton(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    label = "VLC",
                    focusRequester = vlcBtnFocusRequester,
                    isFocusable = isFocusable,
                    onDpadUp = {
                        onInteraction()
                        try { scrubberFocusRequester.requestFocus() } catch (_: Exception) {}
                    },
                    onDpadLeft = {
                        onInteraction()
                        try { actualSpeedRequester.requestFocus() } catch (_: Exception) {}
                    },
                    onDpadRight = {
                        onInteraction()
                        try { stopBtnFocusRequester.requestFocus() } catch (_: Exception) {}
                    },
                    onClick = onExternalPlayer
                )

                // Stop
                PlayerActionButton(
                    icon = Icons.Filled.Close,
                    label = "Stop",
                    accentColor = TvAccentRed,
                    focusRequester = stopBtnFocusRequester,
                    isFocusable = isFocusable,
                    onDpadUp = {
                        onInteraction()
                        try { scrubberFocusRequester.requestFocus() } catch (_: Exception) {}
                    },
                    onDpadLeft = {
                        onInteraction()
                        try { vlcBtnFocusRequester.requestFocus() } catch (_: Exception) {}
                    },
                    onClick = onStop
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Component 6: PlaybackSpeedDrawer (Right-Hand Slide-In)
// ──────────────────────────────────────────────────────────────

@Composable
fun PlaybackSpeedDrawer(
    isOpen: Boolean,
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = modifier
    ) {
        val closeBtnFocusRequester = remember { FocusRequester() }
        val speedFocusRequesters = remember { List(speeds.size) { FocusRequester() } }
        val selectedIndex = remember(currentSpeed) {
            speeds.indexOfFirst { kotlin.math.abs(it - currentSpeed) < 0.01f }.takeIf { it >= 0 } ?: 2
        }

        // Focus selected speed (or close button) when drawer opens
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(80)
            try {
                if (selectedIndex in speedFocusRequesters.indices) {
                    speedFocusRequesters[selectedIndex].requestFocus()
                } else {
                    closeBtnFocusRequester.requestFocus()
                }
            } catch (_: Exception) {}
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Full-screen Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )

            // Drawer Panel pinned strictly to CenterEnd (Right side of the screen)
            AnimatedVisibility(
                visible = isOpen,
                enter = slideInHorizontally(tween(250)) { it },
                exit = slideOutHorizontally(tween(200)) { it },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(360.dp)
                        .background(Color(0xF810121A))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)))
                        .padding(28.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Playback Speed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Select Speed Multiplier", fontSize = 12.sp, color = TvTextSecondary)
                            }

                            var isCloseFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .focusRequester(closeBtnFocusRequester)
                                    .onFocusChanged { isCloseFocused = it.isFocused }
                                    .focusable()
                                    .clip(CircleShape)
                                    .background(if (isCloseFocused) TvBrandIndigo else TvSurfaceVariant)
                                    .border(
                                        width = if (isCloseFocused) 2.dp else 1.dp,
                                        color = if (isCloseFocused) Color.White else Color.White.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .onKeyEvent { keyEvent ->
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                if (keyEvent.type == KeyEventType.KeyDown && speedFocusRequesters.isNotEmpty()) {
                                                    try { speedFocusRequesters.first().requestFocus() } catch (_: Exception) {}
                                                    true
                                                } else false
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                            android.view.KeyEvent.KEYCODE_BACK -> {
                                                if (keyEvent.type == KeyEventType.KeyDown) {
                                                    onDismiss()
                                                    true
                                                } else false
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_UP,
                                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> true
                                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                            android.view.KeyEvent.KEYCODE_ENTER,
                                            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                                            android.view.KeyEvent.KEYCODE_BUTTON_A,
                                            android.view.KeyEvent.KEYCODE_BUTTON_SELECT -> {
                                                if (keyEvent.type == KeyEventType.KeyUp) {
                                                    onDismiss()
                                                }
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    Icons.Filled.Close, contentDescription = "Close Drawer",
                                    tint = Color.White, modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(speeds) { idx, speed ->
                                val isActive = kotlin.math.abs(speed - currentSpeed) < 0.01f
                                var isItemFocused by remember { mutableStateOf(false) }
                                val itemScale by animateFloatAsState(
                                    targetValue = if (isItemFocused) 1.03f else 1.0f,
                                    animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                                    label = "speed_item_scale"
                                )
                                val isFirst = idx == 0
                                val isLast = idx == speeds.size - 1

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(speedFocusRequesters[idx])
                                        .graphicsLayer { scaleX = itemScale; scaleY = itemScale }
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isItemFocused -> TvBrandIndigo
                                                isActive -> TvBrandIndigo.copy(alpha = 0.35f)
                                                else -> TvSurfaceVariant.copy(alpha = 0.7f)
                                            }
                                        )
                                        .border(
                                            width = when {
                                                isItemFocused -> 2.dp
                                                isActive -> 1.dp
                                                else -> 0.dp
                                            },
                                            color = when {
                                                isItemFocused -> Color.White
                                                isActive -> TvAccentCyan
                                                else -> Color.Transparent
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .onFocusChanged { isItemFocused = it.isFocused }
                                        .focusable()
                                        .onKeyEvent { keyEvent ->
                                            when (keyEvent.nativeKeyEvent.keyCode) {
                                                android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                                android.view.KeyEvent.KEYCODE_BACK -> {
                                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                                        onDismiss()
                                                        true
                                                    } else false
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> true
                                                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                                        if (isFirst) {
                                                            try { closeBtnFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        } else {
                                                            try { speedFocusRequesters[idx - 1].requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                    } else false
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                                        if (isLast) {
                                                            true
                                                        } else {
                                                            try { speedFocusRequesters[idx + 1].requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                    } else false
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                                android.view.KeyEvent.KEYCODE_ENTER,
                                                android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                                                android.view.KeyEvent.KEYCODE_BUTTON_A,
                                                android.view.KeyEvent.KEYCODE_BUTTON_SELECT -> {
                                                    if (keyEvent.type == KeyEventType.KeyUp) {
                                                        onSpeedSelected(speed)
                                                        onInteraction()
                                                    }
                                                    true
                                                }
                                                else -> false
                                            }
                                        }
                                        .clickable {
                                            onSpeedSelected(speed)
                                            onInteraction()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 13.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (speed == 1.0f) "1.0× (Normal)" else "${speed}×",
                                            fontSize = 14.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(TvAccentCyan),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                androidx.compose.material3.Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = "Selected",
                                                    tint = TvBackground,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Component 7: AudioTrackDrawer (Right-Hand Slide-In)
// ──────────────────────────────────────────────────────────────

@Composable
fun AudioTrackDrawer(
    isOpen: Boolean,
    tracks: List<AudioTrackInfo>,
    onTrackSelected: (AudioTrackInfo) -> Unit,
    onDismiss: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = modifier
    ) {
        val closeBtnFocusRequester = remember { FocusRequester() }
        val trackFocusRequesters = remember(tracks.size) { List(tracks.size) { FocusRequester() } }
        val selectedIndex = remember(tracks) {
            tracks.indexOfFirst { it.isSelected }.takeIf { it >= 0 } ?: 0
        }

        // Focus selected or first track (or close button) when drawer opens
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(80)
            try {
                if (tracks.isNotEmpty() && selectedIndex in trackFocusRequesters.indices) {
                    trackFocusRequesters[selectedIndex].requestFocus()
                } else {
                    closeBtnFocusRequester.requestFocus()
                }
            } catch (_: Exception) {}
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Full-screen Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )

            // Drawer Panel pinned strictly to CenterEnd (Right side of the screen)
            AnimatedVisibility(
                visible = isOpen,
                enter = slideInHorizontally(tween(250)) { it },
                exit = slideOutHorizontally(tween(200)) { it },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(380.dp)
                        .background(Color(0xF810121A))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)))
                        .padding(28.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Audio & Languages", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Select Audio Stream", fontSize = 12.sp, color = TvTextSecondary)
                            }

                            var isCloseFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .focusRequester(closeBtnFocusRequester)
                                    .onFocusChanged { isCloseFocused = it.isFocused }
                                    .focusable()
                                    .clip(CircleShape)
                                    .background(if (isCloseFocused) TvBrandIndigo else TvSurfaceVariant)
                                    .border(
                                        width = if (isCloseFocused) 2.dp else 1.dp,
                                        color = if (isCloseFocused) Color.White else Color.White.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .onKeyEvent { keyEvent ->
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                if (keyEvent.type == KeyEventType.KeyDown && tracks.isNotEmpty() && trackFocusRequesters.isNotEmpty()) {
                                                    try { trackFocusRequesters.first().requestFocus() } catch (_: Exception) {}
                                                    true
                                                } else false
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                            android.view.KeyEvent.KEYCODE_BACK -> {
                                                if (keyEvent.type == KeyEventType.KeyDown) {
                                                    onDismiss()
                                                    true
                                                } else false
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_UP,
                                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> true
                                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                            android.view.KeyEvent.KEYCODE_ENTER,
                                            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                                            android.view.KeyEvent.KEYCODE_BUTTON_A,
                                            android.view.KeyEvent.KEYCODE_BUTTON_SELECT -> {
                                                if (keyEvent.type == KeyEventType.KeyUp) {
                                                    onDismiss()
                                                }
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    Icons.Filled.Close, contentDescription = "Close Drawer",
                                    tint = Color.White, modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (tracks.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No alternate audio tracks available", fontSize = 13.sp, color = TvTextSecondary)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(tracks) { idx, track ->
                                    var isTrackItemFocused by remember { mutableStateOf(false) }
                                    val trackScale by animateFloatAsState(
                                        targetValue = if (isTrackItemFocused) 1.03f else 1.0f,
                                        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                                        label = "drawer_track_scale"
                                    )
                                    val isFirstTrack = idx == 0
                                    val isLastTrack = idx == tracks.size - 1

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(trackFocusRequesters[idx])
                                            .graphicsLayer { scaleX = trackScale; scaleY = trackScale }
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isTrackItemFocused -> TvBrandIndigo
                                                    track.isSelected -> TvBrandIndigo.copy(alpha = 0.35f)
                                                    else -> TvSurfaceVariant.copy(alpha = 0.7f)
                                                }
                                            )
                                            .border(
                                                width = when {
                                                    isTrackItemFocused -> 2.dp
                                                    track.isSelected -> 1.dp
                                                    else -> 0.dp
                                                },
                                                color = when {
                                                    isTrackItemFocused -> Color.White
                                                    track.isSelected -> TvAccentCyan
                                                    else -> Color.Transparent
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .onFocusChanged { isTrackItemFocused = it.isFocused }
                                            .focusable()
                                            .onKeyEvent { keyEvent ->
                                                when (keyEvent.nativeKeyEvent.keyCode) {
                                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                                    android.view.KeyEvent.KEYCODE_BACK -> {
                                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                                            onDismiss()
                                                            true
                                                        } else false
                                                    }
                                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> true
                                                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                                            if (isFirstTrack) {
                                                                try { closeBtnFocusRequester.requestFocus() } catch (_: Exception) {}
                                                                true
                                                            } else {
                                                                try { trackFocusRequesters[idx - 1].requestFocus() } catch (_: Exception) {}
                                                                true
                                                            }
                                                        } else false
                                                    }
                                                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                                            if (isLastTrack) {
                                                                true
                                                            } else {
                                                                try { trackFocusRequesters[idx + 1].requestFocus() } catch (_: Exception) {}
                                                                true
                                                            }
                                                        } else false
                                                    }
                                                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                                    android.view.KeyEvent.KEYCODE_ENTER,
                                                    android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                                                    android.view.KeyEvent.KEYCODE_BUTTON_A,
                                                    android.view.KeyEvent.KEYCODE_BUTTON_SELECT -> {
                                                        if (keyEvent.type == KeyEventType.KeyUp) {
                                                            onTrackSelected(track)
                                                            onInteraction()
                                                        }
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            }
                                            .clickable {
                                                onTrackSelected(track)
                                                onInteraction()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = track.label,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = Color.White
                                                )
                                                track.language?.let { lang ->
                                                    Text(
                                                        text = "Language: $lang",
                                                        fontSize = 12.sp,
                                                        color = if (isTrackItemFocused) Color.White.copy(alpha = 0.8f) else TvTextSecondary
                                                    )
                                                }
                                            }
                                            if (track.isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(TvAccentCyan),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    androidx.compose.material3.Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = "Selected",
                                                        tint = TvBackground,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Component 8: QuickSeekFeedback
// ──────────────────────────────────────────────────────────────

@Composable
fun QuickSeekFeedback(
    feedback: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = feedback != null,
        enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(tween(220)) + scaleOut(targetScale = 1.1f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.82f))
                .border(2.dp, TvAccentCyan, CircleShape)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    if (feedback?.startsWith("-") == true) Icons.Filled.FastRewind else Icons.Filled.FastForward,
                    contentDescription = null,
                    tint = TvAccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    feedback ?: "",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Component 9: BufferingIndicator
// ──────────────────────────────────────────────────────────────

@Composable
fun BufferingIndicator(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = TvAccentCyan,
                strokeWidth = 3.dp
            )
        }
    }
}
