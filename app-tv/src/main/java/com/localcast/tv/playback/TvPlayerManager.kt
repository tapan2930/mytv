package com.localcast.tv.playback

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.localcast.core.model.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
class TvPlayerManager(private val context: Context) {

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer
        get() {
            if (_exoPlayer == null) {
                initPlayer()
            }
            return _exoPlayer!!
        }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressPollJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<AudioTrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrackInfo>> = _audioTracks.asStateFlow()

    private var currentTitle: String = ""
    private var currentMediaId: String? = null
    var currentStreamUrl: String = ""
        private set
    var currentMimeType: String = "video/mp4"
        private set

    var onProgressCallback: ((id: String, title: String, url: String, pos: Long, dur: Long, mime: String) -> Unit)? = null
    var onPlaybackError: ((errorMsg: String) -> Unit)? = null

    private fun initPlayer() {
        val player = ExoPlayer.Builder(context).build()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateState()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("TvPlayerManager", "ExoPlayer playback error: ${error.message} (code ${error.errorCode})", error)
                onPlaybackError?.invoke(error.localizedMessage ?: "Playback error: ${error.errorCodeName}")
                updateState()
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateAudioTracks(tracks)
            }
        })
        _exoPlayer = player
        startProgressPolling()
    }

    private fun updateAudioTracks(tracks: Tracks) {
        val list = mutableListOf<AudioTrackInfo>()
        var audioGroupIdx = 0
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val lang = format.language?.uppercase()
                    val label = format.label ?: if (!lang.isNullOrBlank()) "Audio ($lang)" else "Audio Track ${list.size + 1}"
                    list.add(
                        AudioTrackInfo(
                            groupIndex = audioGroupIdx,
                            trackIndex = i,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
                audioGroupIdx++
            }
        }
        _audioTracks.value = list
    }

    fun selectAudioTrack(track: AudioTrackInfo) {
        val audioGroups = exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (track.groupIndex in audioGroups.indices) {
            val targetGroup = audioGroups[track.groupIndex]
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(
                    TrackSelectionOverride(
                        targetGroup.mediaTrackGroup,
                        track.trackIndex
                    )
                )
                .build()
            updateAudioTracks(exoPlayer.currentTracks)
        }
    }

    fun cycleNextAudioTrack() {
        val tracks = _audioTracks.value
        if (tracks.size <= 1) return
        val currentIdx = tracks.indexOfFirst { it.isSelected }.let { if (it >= 0) it else 0 }
        val nextIdx = (currentIdx + 1) % tracks.size
        selectAudioTrack(tracks[nextIdx])
    }

    private fun startProgressPolling() {
        progressPollJob?.cancel()
        progressPollJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    updateState()
                }
                delay(1000)
            }
        }
    }

    fun playMediaStream(streamUrl: String, title: String, id: String, mimeType: String = "video/mp4", startPositionMs: Long = 0) {
        currentTitle = title
        currentMediaId = id
        currentStreamUrl = streamUrl
        currentMimeType = mimeType

        val mediaItem = MediaItem.fromUri(streamUrl)
        exoPlayer.setMediaItem(mediaItem, startPositionMs)
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
        exoPlayer.play()
        updateState()
    }

    fun play() {
        exoPlayer.play()
        updateState()
    }

    fun pause() {
        _exoPlayer?.pause()
        updateState()
    }

    fun togglePlayPause() {
        val player = _exoPlayer ?: return
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        _exoPlayer?.seekTo(positionMs)
        updateState()
    }

    fun seekBy(deltaMs: Long) {
        val player = _exoPlayer ?: return
        val newPos = (player.currentPosition + deltaMs).coerceAtLeast(0)
        player.seekTo(newPos)
        updateState()
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        exoPlayer.setPlaybackSpeed(clamped)
    }

    fun setVolume(volume: Float) {
        _exoPlayer?.let { player ->
            player.volume = volume.coerceIn(0f, 1f)
            updateState()
        }
    }

    fun stop() {
        _exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
        }
        updateState()
    }

    private fun updateState() {
        val player = _exoPlayer ?: return
        val isEnded = player.playbackState == Player.STATE_ENDED
        val pos = player.currentPosition
        val dur = if (player.duration > 0) player.duration else 0

        _playbackState.value = PlaybackState(
            isPlaying = player.isPlaying,
            positionMs = pos,
            durationMs = dur,
            currentMediaId = currentMediaId,
            title = currentTitle,
            volume = player.volume,
            isEnded = isEnded
        )

        val id = currentMediaId
        if (player.isPlaying && id != null && currentStreamUrl.isNotBlank()) {
            onProgressCallback?.invoke(id, currentTitle, currentStreamUrl, pos, dur, currentMimeType)
        }
    }

    fun release() {
        progressPollJob?.cancel()
        _exoPlayer?.release()
        _exoPlayer = null
    }
}
