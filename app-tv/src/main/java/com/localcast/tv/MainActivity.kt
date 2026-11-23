package com.localcast.tv

import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import com.localcast.tv.ui.TvMainScreen
import com.localcast.tv.ui.TvMainViewModel
import com.localcast.tv.ui.theme.LocalCastTvTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TvMainViewModel by viewModels()

    var activePlayerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep TV screen awake during video/casting sessions
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Forward virtual D-pad and remote navigation commands received from phone
        lifecycleScope.launch {
            viewModel.dpadKeyEvents.collect { keyCode ->
                val now = SystemClock.uptimeMillis()
                val downEvent = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
                val upEvent = KeyEvent(now, now + 30, KeyEvent.ACTION_UP, keyCode, 0)
                dispatchKeyEvent(downEvent)
                dispatchKeyEvent(upEvent)
            }
        }

        setContent {
            LocalCastTvTheme {
                TvMainScreen(
                    viewModel = viewModel,
                    onPlayerViewCreated = { playerView ->
                        activePlayerView = playerView
                    }
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Dedicated Hardware Media Keys (Play/Pause, Fast-Forward, Rewind, Stop)
        // These keys should directly control playback without consuming UI focus navigation.
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    viewModel.playerManager.togglePlayPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    viewModel.playerManager.play()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    viewModel.playerManager.pause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    viewModel.playerManager.seekBy(10000)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    viewModel.playerManager.seekBy(-10000)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    viewModel.stopPlayback()
                    return true
                }
            }
        }

        // All D-pad navigation (UP, DOWN, LEFT, RIGHT), DPAD_CENTER, ENTER, BACK, etc.
        // are delegated directly to Compose so focus moves smoothly between the playback
        // controls (Rewind, Play/Pause, Fast-Forward, Stop, Fullscreen, etc.).
        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopPlayback()
        viewModel.playerManager.release()
    }
}
