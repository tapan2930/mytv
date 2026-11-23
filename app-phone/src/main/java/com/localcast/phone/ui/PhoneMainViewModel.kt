package com.localcast.phone.ui

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localcast.core.model.DeviceSpecs
import com.localcast.core.model.InstallStatus
import com.localcast.core.model.MediaItem
import com.localcast.core.model.PlaybackCommand
import com.localcast.core.model.PlaybackState
import com.localcast.core.model.SocketMessage
import com.localcast.core.network.NetworkUtils
import com.localcast.core.network.storage.SettingsRepository
import com.localcast.phone.data.MediaRepository
import com.localcast.phone.service.LocalServerForegroundService
import com.localcast.phone.service.ServerStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.security.MessageDigest

enum class PhoneTab {
    MEDIA,
    REMOTE,
    APKS,
    SETTINGS
}

enum class MediaSubTab {
    ALL,
    VIDEOS,
    AUDIOS,
    PHOTOS,
    DOCUMENTS
}

class PhoneMainViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaRepository(application)
    private val settingsRepository = SettingsRepository(application)

    val isServerRunning: StateFlow<Boolean> = ServerStateHolder.isServerRunning
    val serverIp: StateFlow<String?> = ServerStateHolder.serverIp
    val serverPort: StateFlow<Int> = ServerStateHolder.serverPort
    val connectedTv: StateFlow<DeviceSpecs?> = ServerStateHolder.connectedTvSpecs
    val nowPlaying: StateFlow<MediaItem?> = ServerStateHolder.nowPlayingMedia
    val playbackState: StateFlow<PlaybackState> = ServerStateHolder.playbackState
    val installStatus: StateFlow<SocketMessage.InstallStatusUpdate?> = ServerStateHolder.installStatus

    private val _selectedTab = MutableStateFlow(PhoneTab.MEDIA)
    val selectedTab: StateFlow<PhoneTab> = _selectedTab.asStateFlow()

    private val _mediaSubTab = MutableStateFlow(MediaSubTab.ALL)
    val mediaSubTab: StateFlow<MediaSubTab> = _mediaSubTab.asStateFlow()

    private val _videos = MutableStateFlow<List<MediaItem>>(emptyList())
    val videos: StateFlow<List<MediaItem>> = _videos.asStateFlow()

    private val _audios = MutableStateFlow<List<MediaItem>>(emptyList())
    val audios: StateFlow<List<MediaItem>> = _audios.asStateFlow()

    private val _photos = MutableStateFlow<List<MediaItem>>(emptyList())
    val photos: StateFlow<List<MediaItem>> = _photos.asStateFlow()

    private val _documents = MutableStateFlow<List<MediaItem>>(emptyList())
    val documents: StateFlow<List<MediaItem>> = _documents.asStateFlow()

    val storageApks: StateFlow<List<MediaItem>> = ServerStateHolder.storageApks

    val mediaScanDirPath = settingsRepository.mediaScanDirPath
    val mediaScanDirName = settingsRepository.mediaScanDirName
    val hasConfiguredDir = settingsRepository.hasConfiguredDir
    val scanVideos = settingsRepository.scanVideos
    val scanAudios = settingsRepository.scanAudios
    val scanPhotos = settingsRepository.scanPhotos
    val scanDocs = settingsRepository.scanDocs

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showDirectoryConfigDialog = MutableStateFlow(false)
    val showDirectoryConfigDialog: StateFlow<Boolean> = _showDirectoryConfigDialog.asStateFlow()

    private val _isWifiConnected = MutableStateFlow(false)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private val _serverPassword = MutableStateFlow("")
    val serverPassword: StateFlow<String> = _serverPassword.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val playlist: StateFlow<List<MediaItem>> = ServerStateHolder.playlist
    val currentPlaylistIndex: StateFlow<Int> = ServerStateHolder.currentPlaylistIndex

    private var installStatusDismissJob: Job? = null

    init {
        checkWifi()
        loadSettings()
        loadMedia()
        observeInstallStatus()
        checkFirstTimeDirectoryConfig()
    }

    private fun checkFirstTimeDirectoryConfig() {
        viewModelScope.launch {
            val configured = settingsRepository.hasConfiguredDir.first()
            if (!configured) {
                _showDirectoryConfigDialog.value = true
            }
        }
    }

    private fun observeInstallStatus() {
        viewModelScope.launch {
            installStatus.collect { status ->
                installStatusDismissJob?.cancel()
                if (status != null && (status.status == InstallStatus.INSTALLED || status.status == InstallStatus.FAILED)) {
                    installStatusDismissJob = launch {
                        delay(6000)
                        ServerStateHolder.updateInstallStatus(null)
                    }
                }
            }
        }
    }

    fun dismissInstallStatus() {
        installStatusDismissJob?.cancel()
        ServerStateHolder.updateInstallStatus(null)
    }

    fun selectTab(tab: PhoneTab) {
        _selectedTab.value = tab
    }

    fun selectMediaSubTab(subTab: MediaSubTab) {
        _mediaSubTab.value = subTab
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun checkWifi() {
        _isWifiConnected.value = NetworkUtils.isWifiConnected(getApplication())
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _serverPassword.value = settingsRepository.getServerPassword()
        }
    }

    fun loadMedia() {
        viewModelScope.launch {
            val dirPath = settingsRepository.mediaScanDirPath.first()
            val doVideos = settingsRepository.scanVideos.first()
            val doAudios = settingsRepository.scanAudios.first()
            val doPhotos = settingsRepository.scanPhotos.first()
            val doDocs = settingsRepository.scanDocs.first()

            val vids = if (doVideos) mediaRepository.getVideos(dirPath) else emptyList()
            val auds = if (doAudios) mediaRepository.getAudios(dirPath) else emptyList()
            val pics = if (doPhotos) mediaRepository.getPhotos(dirPath) else emptyList()
            val docs = if (doDocs) mediaRepository.getDocuments(dirPath) else emptyList()

            _videos.value = vids
            _audios.value = auds
            _photos.value = pics
            _documents.value = docs

            val apks = mediaRepository.getStorageApkFiles()
            ServerStateHolder.updateStorageApks(apks)

            ServerStateHolder.updateMediaList(vids + auds + pics + docs)
        }
    }

    fun openDirectoryConfigDialog() {
        _showDirectoryConfigDialog.value = true
    }

    fun closeDirectoryConfigDialog() {
        _showDirectoryConfigDialog.value = false
    }

    fun setMediaScanDirectory(path: String, name: String, uri: String = "") {
        viewModelScope.launch {
            settingsRepository.saveMediaScanDir(path, name, uri)
            _showDirectoryConfigDialog.value = false
            loadMedia()
            _toastMessage.value = "Scanning folder: $name"
        }
    }

    fun setMediaScanDirectoryFromUri(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}
        val (path, name) = mediaRepository.resolveTreeUriToPath(uri)
        setMediaScanDirectory(path, name, uri.toString())
    }

    fun setScanPreferences(videos: Boolean, audios: Boolean, photos: Boolean, docs: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveScanPreferences(videos, audios, photos, docs)
            loadMedia()
            _toastMessage.value = "Updated media scan preferences"
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addCustomApkUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = mediaRepository.getApkMediaItemFromUri(uri)
            if (item != null) {
                val current = ServerStateHolder.storageApks.value
                val updated = listOf(item) + current.filter { it.id != item.id }
                ServerStateHolder.updateStorageApks(updated)
                withContext(Dispatchers.Main) {
                    _toastMessage.value = "Added APK: ${item.title}"
                }
            } else {
                withContext(Dispatchers.Main) {
                    _toastMessage.value = "Could not parse selected APK file."
                }
            }
        }
    }

    fun toggleServer() {
        checkWifi()
        if (!_isWifiConnected.value && !isServerRunning.value) {
            _toastMessage.value = "Cannot start server: Not connected to Wi-Fi!"
            return
        }

        val context = getApplication<Application>()
        if (isServerRunning.value) {
            LocalServerForegroundService.stopService(context)
        } else {
            LocalServerForegroundService.startService(context)
        }
    }

    fun addToPlaylist(item: MediaItem) {
        ServerStateHolder.addToPlaylist(item)
        _toastMessage.value = "Added to Queue: ${item.title}"
    }

    fun castMedia(media: MediaItem) {
        if (!isServerRunning.value) {
            _toastMessage.value = "Start the server first to cast!"
            return
        }
        if (connectedTv.value == null) {
            _toastMessage.value = "Waiting for Android TV to connect..."
            return
        }
        ServerStateHolder.castMedia(media)
        _toastMessage.value = "Casting: ${media.title}"
        _selectedTab.value = PhoneTab.REMOTE
    }

    fun playPlaylistItem(index: Int) {
        ServerStateHolder.playPlaylistItem(index)
    }

    fun playNext() {
        ServerStateHolder.playNext()
    }

    fun playPrevious() {
        ServerStateHolder.playPrevious()
    }

    fun seekRelative(deltaMs: Long) {
        if (deltaMs > 0) {
            sendControlCommand(PlaybackCommand.SEEK_FORWARD)
        } else {
            sendControlCommand(PlaybackCommand.SEEK_BACKWARD)
        }
    }

    fun removeFromPlaylist(index: Int) {
        ServerStateHolder.removeFromPlaylist(index)
    }

    fun clearPlaylist() {
        ServerStateHolder.clearPlaylist()
        _toastMessage.value = "Playlist cleared"
    }

    fun sendControlCommand(command: PlaybackCommand, value: Long = 0) {
        ServerStateHolder.broadcast(SocketMessage.ControlCommand(command = command, value = value))
    }

    fun sendApk(item: MediaItem) {
        val tv = connectedTv.value
        if (tv == null) {
            _toastMessage.value = "Android TV is not connected!"
            return
        }

        val apkFile = mediaRepository.getFileFromUri(item.uri)
        if (apkFile == null || !apkFile.exists()) {
            _toastMessage.value = "Could not read APK file."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _toastMessage.value = "Preparing APK ${item.title} for TV..."

            val pm = getApplication<Application>().packageManager
            val pkgInfo = try {
                pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            } catch (_: Exception) { null }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo?.longVersionCode ?: 1L
            } else {
                @Suppress("DEPRECATION")
                pkgInfo?.versionCode?.toLong() ?: 1L
            }
            val versionName = pkgInfo?.versionName ?: ""
            val packageName = pkgInfo?.packageName ?: item.album ?: item.id

            // Store active transfer item so the HTTP server serves this exact file
            ServerStateHolder.activeApkTransferItem = item

            // Compute SHA-256
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(apkFile).use { fis ->
                var read = fis.read(buffer)
                while (read != -1) {
                    digest.update(buffer, 0, read)
                    read = fis.read(buffer)
                }
            }
            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }

            // Offer transfer to TV
            ServerStateHolder.broadcast(
                SocketMessage.ApkTransferOffer(
                    packageName = packageName,
                    appName = item.title,
                    versionCode = versionCode,
                    sizeBytes = apkFile.length(),
                    sha256 = sha256,
                    supportedAbis = tv.supportedAbis
                )
            )

            withContext(Dispatchers.Main) {
                val versionLabel = if (versionName.isNotBlank()) "v$versionName (build $versionCode)" else "build $versionCode"
                _toastMessage.value = "Sending ${item.title} $versionLabel to TV..."
            }
        }
    }

    fun saveServerSettings(port: Int, password: String) {
        viewModelScope.launch {
            settingsRepository.saveServerPort(port)
            settingsRepository.saveServerPassword(password)
            _serverPassword.value = password
            _toastMessage.value = "Settings saved! Restart server to apply."
        }
    }
}
