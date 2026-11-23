package com.localcast.tv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.localcast.tv.playback.AudioTrackInfo
import com.localcast.tv.ui.TvMainViewModel
import kotlinx.coroutines.launch

@Composable
fun TvNetflixPlayerScreen(
    media: com.localcast.core.model.MediaItem,
    playbackState: com.localcast.core.model.PlaybackState,
    audioTracks: List<AudioTrackInfo>,
    viewModel: TvMainViewModel,
    onPlayerViewCreated: (PlayerView?) -> Unit
) {
    // ─── Overlay State ───
    var isControllerVisible by remember { mutableStateOf(true) }
    var isSidePanelOpen by remember { mutableStateOf(false) }
    var isSpeedSelectorOpen by remember { mutableStateOf(false) }
    var userInteractedTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // ─── Scrubbing State ───
    var isScrubbing by remember { mutableStateOf(false) }
    var wasPlayingBeforeScrub by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableStateOf(0L) }
    var scrubAnchorMs by remember { mutableStateOf(0L) }
    var consecutiveScrubCount by remember { mutableStateOf(0) }
    var lastScrubPressTime by remember { mutableStateOf(0L) }

    // ─── Quick Seek Feedback ───
    var quickSeekFeedback by remember { mutableStateOf<String?>(null) }
    var quickSeekTriggerTime by remember { mutableStateOf(0L) }

    // ─── Playback Speed ───
    var playbackSpeed by remember { mutableStateOf(1.0f) }

    // ─── Buffering ───
    val isBuffering = playbackState.isPlaying.not() &&
        playbackState.positionMs > 0 &&
        playbackState.durationMs > 0 &&
        !playbackState.isEnded

    // ─── Focus Requesters ───
    val playFocusRequester = remember { FocusRequester() }
    val scrubberFocusRequester = remember { FocusRequester() }
    val audioBtnFocusRequester = remember { FocusRequester() }
    val speedBtnFocusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }

    // ─── Derived Values ───
    val pos = playbackState.positionMs
    val dur = if (playbackState.durationMs > 0) playbackState.durationMs else media.durationMs
    val isPlaying = playbackState.isPlaying
    val bufferedFraction = if (dur > 0) {
        val exo = viewModel.playerManager.exoPlayer
        (exo.bufferedPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // ─── Scrubbing Logic ───
    fun getScrubStep(): Long {
        val now = System.currentTimeMillis()
        val diff = now - lastScrubPressTime
        if (diff < 450L) {
            consecutiveScrubCount++
        } else {
            consecutiveScrubCount = 1
        }
        lastScrubPressTime = now
        return when {
            consecutiveScrubCount >= 10 -> 60_000L
            consecutiveScrubCount >= 4 -> 30_000L
            else -> 10_000L
        }
    }

    fun onScrub(deltaSign: Int) {
        val step = getScrubStep()
        val currentDur = if (playbackState.durationMs > 0) playbackState.durationMs
            else if (media.durationMs > 0) media.durationMs
            else viewModel.playerManager.exoPlayer.duration.takeIf { it > 0 } ?: 0L
        val maxDur = if (currentDur > 0) currentDur else Long.MAX_VALUE
        if (!isScrubbing) {
            isScrubbing = true
            wasPlayingBeforeScrub = playbackState.isPlaying
            if (playbackState.isPlaying) {
                viewModel.playerManager.pause()
            }
            scrubAnchorMs = playbackState.positionMs
            scrubPositionMs = (playbackState.positionMs + (deltaSign * step)).coerceIn(0L, maxDur)
        } else {
            scrubPositionMs = (scrubPositionMs + (deltaSign * step)).coerceIn(0L, maxDur)
        }
        userInteractedTime = System.currentTimeMillis()
    }

    fun commitScrub() {
        if (isScrubbing) {
            viewModel.playerManager.seekTo(scrubPositionMs)
            if (wasPlayingBeforeScrub) {
                viewModel.playerManager.play()
            }
            isScrubbing = false
            consecutiveScrubCount = 0
            userInteractedTime = System.currentTimeMillis()
        }
    }

    fun cancelScrub() {
        if (isScrubbing) {
            if (wasPlayingBeforeScrub) {
                viewModel.playerManager.play()
            }
            isScrubbing = false
            consecutiveScrubCount = 0
            userInteractedTime = System.currentTimeMillis()
        }
    }

    fun triggerQuickSeek(deltaMs: Long, label: String) {
        viewModel.playerManager.seekBy(deltaMs)
        quickSeekFeedback = label
        quickSeekTriggerTime = System.currentTimeMillis()
        userInteractedTime = System.currentTimeMillis()
    }

    // ─── Quick Seek Dismiss ───
    LaunchedEffect(quickSeekTriggerTime) {
        if (quickSeekTriggerTime > 0L) {
            kotlinx.coroutines.delay(800)
            quickSeekFeedback = null
        }
    }

    // ─── Auto-Hide (5s) ───
    LaunchedEffect(isControllerVisible, isPlaying, userInteractedTime, isSidePanelOpen, isScrubbing, isSpeedSelectorOpen) {
        if (isControllerVisible && isPlaying && !isSidePanelOpen && !isScrubbing && !isSpeedSelectorOpen) {
            kotlinx.coroutines.delay(5000L)
            isControllerVisible = false
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val isBackgroundControlsFocusable = isControllerVisible && !isSidePanelOpen && !isSpeedSelectorOpen

    // Focus restoration & dismissal handler for audio drawer
    val dismissAudioDrawer: () -> Unit = {
        isSidePanelOpen = false
        isControllerVisible = true
        userInteractedTime = System.currentTimeMillis()
        coroutineScope.launch {
            kotlinx.coroutines.delay(80)
            try { playFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // Focus restoration & dismissal handler for playback speed drawer
    val dismissSpeedDrawer: () -> Unit = {
        isSpeedSelectorOpen = false
        isControllerVisible = true
        userInteractedTime = System.currentTimeMillis()
        coroutineScope.launch {
            kotlinx.coroutines.delay(80)
            try { playFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // ─── Focus Restoration ───
    LaunchedEffect(isControllerVisible) {
        if (isControllerVisible) {
            kotlinx.coroutines.delay(60)
            try { playFocusRequester.requestFocus() } catch (_: Exception) {}
        } else {
            try { rootFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // ─── BackHandler Priority Chain ───
    // 1. Close speed drawer (Shift focus directly to on-screen play button)
    BackHandler(enabled = isSpeedSelectorOpen) {
        dismissSpeedDrawer()
    }

    // 2. Close audio drawer (Shift focus directly to on-screen play button)
    BackHandler(enabled = !isSpeedSelectorOpen && isSidePanelOpen) {
        dismissAudioDrawer()
    }

    // 3. Cancel scrubbing
    BackHandler(enabled = !isSpeedSelectorOpen && !isSidePanelOpen && isScrubbing) {
        cancelScrub()
        try { playFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    // 4. Hide controls
    BackHandler(enabled = !isSpeedSelectorOpen && !isSidePanelOpen && !isScrubbing && isControllerVisible) {
        isControllerVisible = false
    }

    // ═══════════════════════════════════════════════
    //  ROOT LAYOUT
    // ═══════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocusRequester)
            .focusable(!isControllerVisible)
            .onKeyEvent { keyEvent ->
                if (!isControllerVisible && keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                        android.view.KeyEvent.KEYCODE_BUTTON_A,
                        android.view.KeyEvent.KEYCODE_BUTTON_SELECT,
                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            isControllerVisible = true
                            userInteractedTime = System.currentTimeMillis()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(enabled = isControllerVisible && !isSidePanelOpen && !isSpeedSelectorOpen) {
                isControllerVisible = !isControllerVisible
                userInteractedTime = System.currentTimeMillis()
            }
    ) {
        // ── Layer 1: Video Surface ──
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = viewModel.playerManager.exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    isFocusable = false
                    onPlayerViewCreated(this)
                }
            },
            update = { view ->
                if (view.player != viewModel.playerManager.exoPlayer) {
                    view.player = viewModel.playerManager.exoPlayer
                }
                onPlayerViewCreated(view)
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Layer 2: Buffering Indicator ──
        BufferingIndicator(
            isVisible = isBuffering && !isControllerVisible,
            modifier = Modifier.align(Alignment.Center)
        )

        // ── Layer 3: Quick Seek Feedback ──
        QuickSeekFeedback(
            feedback = quickSeekFeedback,
            modifier = Modifier.align(Alignment.Center)
        )

        // ── Layer 4: Top Bar ──
        AnimatedVisibility(
            visible = isControllerVisible,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PlayerTopBar(
                title = media.title,
                mimeType = media.mimeType,
                width = media.width,
                height = media.height
            )
        }

        // ── Layer 5: Center Controls ──
        AnimatedVisibility(
            visible = isControllerVisible,
            enter = scaleIn(initialScale = 0.85f, animationSpec = tween(200)) + fadeIn(tween(200)),
            exit = scaleOut(targetScale = 0.85f, animationSpec = tween(200)) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            PlayerCenterControls(
                isPlaying = isPlaying,
                playFocusRequester = playFocusRequester,
                isFocusable = isBackgroundControlsFocusable,
                onTogglePlayPause = {
                    viewModel.playerManager.togglePlayPause()
                    userInteractedTime = System.currentTimeMillis()
                },
                onRewind = { triggerQuickSeek(-10000L, "-10s") },
                onForward = { triggerQuickSeek(10000L, "+10s") },
                onDpadUp = {},
                onDpadDown = {
                    try { scrubberFocusRequester.requestFocus() } catch (_: Exception) {}
                }
            )
        }

        // ── Layer 6: Bottom Bar ──
        AnimatedVisibility(
            visible = isControllerVisible,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val activeTrack = audioTracks.firstOrNull { it.isSelected } ?: audioTracks.firstOrNull()
            val audioBadgeLabel = activeTrack?.language?.let { " ($it)" } ?: ""

            val previewThumbnailUrl = remember(media.id, scrubPositionMs, isScrubbing) {
                if (isScrubbing) {
                    viewModel.getThumbnailUrl(media.id, scrubPositionMs)
                } else null
            }

            PlayerBottomBar(
                positionMs = pos,
                durationMs = dur,
                bufferedFraction = bufferedFraction,
                isScrubbing = isScrubbing,
                scrubPositionMs = scrubPositionMs,
                scrubAnchorMs = scrubAnchorMs,
                previewThumbnailUrl = previewThumbnailUrl,
                playbackSpeed = playbackSpeed,
                activeAudioLabel = audioBadgeLabel,
                playFocusRequester = playFocusRequester,
                scrubberFocusRequester = scrubberFocusRequester,
                audioBtnFocusRequester = audioBtnFocusRequester,
                speedBtnFocusRequester = speedBtnFocusRequester,
                isFocusable = isBackgroundControlsFocusable,
                onScrubLeft = { onScrub(-1) },
                onScrubRight = { onScrub(+1) },
                onScrubCommit = { commitScrub() },
                onScrubCancel = {
                    cancelScrub()
                    try { playFocusRequester.requestFocus() } catch (_: Exception) {}
                },
                onOpenAudio = {
                    isSidePanelOpen = true
                    userInteractedTime = System.currentTimeMillis()
                },
                onOpenSpeed = {
                    isSpeedSelectorOpen = true
                    userInteractedTime = System.currentTimeMillis()
                },
                onExternalPlayer = {
                    viewModel.playInExternalPlayer()
                    userInteractedTime = System.currentTimeMillis()
                },
                onStop = { viewModel.stopPlayback() },
                onInteraction = { userInteractedTime = System.currentTimeMillis() }
            )
        }

        // ── Layer 7: Playback Speed Drawer (Right-Hand Slide-In) ──
        PlaybackSpeedDrawer(
            isOpen = isSpeedSelectorOpen,
            currentSpeed = playbackSpeed,
            onSpeedSelected = { speed ->
                playbackSpeed = speed
                viewModel.playerManager.setPlaybackSpeed(speed)
                dismissSpeedDrawer()
            },
            onDismiss = dismissSpeedDrawer,
            onInteraction = { userInteractedTime = System.currentTimeMillis() }
        )

        // ── Layer 8: Audio Track Drawer (Self-contained scrim + right-hand drawer) ──
        AudioTrackDrawer(
            isOpen = isSidePanelOpen,
            tracks = audioTracks,
            onTrackSelected = { track ->
                viewModel.selectAudioTrack(track)
                dismissAudioDrawer()
            },
            onDismiss = dismissAudioDrawer,
            onInteraction = { userInteractedTime = System.currentTimeMillis() }
        )
    }
}
