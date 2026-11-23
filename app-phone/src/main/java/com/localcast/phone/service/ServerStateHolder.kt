package com.localcast.phone.service

import com.localcast.core.model.DeviceSpecs
import com.localcast.core.model.MediaItem
import com.localcast.core.model.PlaybackState
import com.localcast.core.model.SocketMessage
import com.localcast.core.network.server.LocalHttpServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServerStateHolder {
    var serverInstance: LocalHttpServer? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverIp = MutableStateFlow<String?>(null)
    val serverIp: StateFlow<String?> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow(8844)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _connectedTvSpecs = MutableStateFlow<DeviceSpecs?>(null)
    val connectedTvSpecs: StateFlow<DeviceSpecs?> = _connectedTvSpecs.asStateFlow()

    private val _nowPlayingMedia = MutableStateFlow<MediaItem?>(null)
    val nowPlayingMedia: StateFlow<MediaItem?> = _nowPlayingMedia.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _installStatus = MutableStateFlow<SocketMessage.InstallStatusUpdate?>(null)
    val installStatus: StateFlow<SocketMessage.InstallStatusUpdate?> = _installStatus.asStateFlow()

    private val _storageApks = MutableStateFlow<List<MediaItem>>(emptyList())
    val storageApks: StateFlow<List<MediaItem>> = _storageApks.asStateFlow()

    @Volatile
    var activeApkTransferItem: MediaItem? = null

    fun findApk(idOrPackage: String): MediaItem? {
        val active = activeApkTransferItem
        if (active != null && (active.id == idOrPackage || active.album == idOrPackage || active.id.startsWith(idOrPackage))) {
            return active
        }
        val list = _storageApks.value
        return list.find { it.id == idOrPackage }
            ?: list.find { it.album == idOrPackage }
            ?: list.find { it.id.startsWith(idOrPackage) }
    }

    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlist: StateFlow<List<MediaItem>> = _playlist.asStateFlow()

    private val _currentPlaylistIndex = MutableStateFlow(-1)
    val currentPlaylistIndex: StateFlow<Int> = _currentPlaylistIndex.asStateFlow()

    private const val MAX_MEDIA_CACHE = 5000
    private val _allMediaMap = object : LinkedHashMap<String, MediaItem>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MediaItem>?): Boolean {
            return size > MAX_MEDIA_CACHE
        }
    }

    fun updateMediaList(items: List<MediaItem>) {
        synchronized(_allMediaMap) {
            for (item in items) {
                _allMediaMap[item.id] = item
            }
        }
    }

    fun findMedia(id: String): MediaItem? {
        return synchronized(_allMediaMap) {
            _allMediaMap[id] ?: _nowPlayingMedia.value?.takeIf { it.id == id }
        }
    }

    fun updateStorageApks(list: List<MediaItem>) {
        _storageApks.value = list
    }

    fun updatePlaylist(list: List<MediaItem>, index: Int = _currentPlaylistIndex.value) {
        _playlist.value = list
        _currentPlaylistIndex.value = index
    }

    fun addToPlaylist(item: MediaItem) {
        val current = _playlist.value.toMutableList()
        current.add(item)
        _playlist.value = current
        if (_currentPlaylistIndex.value == -1) {
            playPlaylistItem(0)
        }
    }

    fun castMedia(media: MediaItem) {
        val current = _playlist.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == media.id }
        if (existingIndex >= 0) {
            _currentPlaylistIndex.value = existingIndex
        } else {
            current.add(media)
            _playlist.value = current
            _currentPlaylistIndex.value = current.lastIndex
        }

        val ip = serverIp.value ?: "127.0.0.1"
        val port = serverPort.value
        val streamUrl = "http://$ip:$port/api/media/stream/${media.id}"
        updateNowPlaying(media)
        broadcast(SocketMessage.PlayMedia(media, streamUrl, 0))
    }

    fun playPlaylistItem(index: Int) {
        val list = _playlist.value
        if (index in list.indices) {
            _currentPlaylistIndex.value = index
            val media = list[index]
            val ip = serverIp.value ?: "127.0.0.1"
            val port = serverPort.value
            val streamUrl = "http://$ip:$port/api/media/stream/${media.id}"
            updateNowPlaying(media)
            broadcast(SocketMessage.PlayMedia(media, streamUrl, 0))
        }
    }

    fun playNext() {
        val list = _playlist.value
        val nextIdx = _currentPlaylistIndex.value + 1
        if (nextIdx in list.indices) {
            playPlaylistItem(nextIdx)
        }
    }

    fun playPrevious() {
        val list = _playlist.value
        val prevIdx = _currentPlaylistIndex.value - 1
        if (prevIdx in list.indices) {
            playPlaylistItem(prevIdx)
        }
    }

    fun removeFromPlaylist(index: Int) {
        val current = _playlist.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _playlist.value = current
            if (current.isEmpty()) {
                _currentPlaylistIndex.value = -1
            } else if (_currentPlaylistIndex.value >= current.size) {
                _currentPlaylistIndex.value = current.lastIndex
            }
        }
    }

    fun clearPlaylist() {
        _playlist.value = emptyList()
        _currentPlaylistIndex.value = -1
    }

    fun updateServerRunning(running: Boolean, ip: String? = null, port: Int = 8844) {
        _isServerRunning.value = running
        _serverIp.value = ip
        _serverPort.value = port
    }

    fun updateConnectedTv(specs: DeviceSpecs?) {
        _connectedTvSpecs.value = specs
    }

    fun updateNowPlaying(media: MediaItem?) {
        _nowPlayingMedia.value = media
    }

    fun updatePlaybackState(state: PlaybackState) {
        _playbackState.value = state
        if (state.isEnded) {
            playNext()
        }
    }

    fun updateInstallStatus(update: SocketMessage.InstallStatusUpdate?) {
        _installStatus.value = update
    }

    fun broadcast(message: SocketMessage) {
        serverInstance?.broadcast(message)
    }
}
