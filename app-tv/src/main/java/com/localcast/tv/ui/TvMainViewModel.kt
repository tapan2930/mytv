package com.localcast.tv.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localcast.core.model.DeviceSpecs
import com.localcast.core.model.InstallStatus
import com.localcast.core.model.MediaItem
import com.localcast.core.model.MediaType
import com.localcast.core.model.PlaybackCommand
import com.localcast.core.model.PlaybackState
import com.localcast.core.model.SocketMessage
import com.localcast.core.network.NetworkUtils
import com.localcast.core.network.client.ConnectionState
import com.localcast.core.network.client.LocalTvClient
import com.localcast.core.network.discovery.DiscoveredService
import com.localcast.core.network.discovery.NsdDiscoverer
import com.localcast.core.network.storage.LastPlayedMedia
import com.localcast.core.network.storage.PlayedMediaItem
import com.localcast.core.network.storage.SettingsRepository
import com.localcast.tv.installer.InstallEventsBus
import com.localcast.tv.installer.TvPackageInstaller
import com.localcast.tv.playback.TvPlayerManager
import android.view.KeyEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class ReceivedApkInfo(
    val packageName: String,
    val appName: String,
    val filePath: String,
    val sizeBytes: Long,
    val status: InstallStatus,
    val downloadProgress: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val versionCode: Long = 0L,
    val versionName: String = "",
    val installedVersionCode: Long? = null,
    val isDowngrade: Boolean = false
)

enum class TvTab {
    PLAYER,
    APKS,
    CONNECTION,
    SETTINGS
}

class TvMainViewModel(application: Application) : AndroidViewModel(application) {

    val playerManager = TvPlayerManager(application)
    val packageInstaller = TvPackageInstaller(application)
    private val settingsRepository = SettingsRepository(application)
    private val nsdDiscoverer = NsdDiscoverer(application)
    private val subnetFastProbe = com.localcast.core.network.discovery.SubnetFastProbe()
    private val httpClient by lazy { HttpClient(CIO) }
    private val apkDownloadManager by lazy { com.localcast.tv.installer.TvApkDownloadManager(application, httpClient) }

    private val _selectedTab = MutableStateFlow(TvTab.PLAYER)
    val selectedTab: StateFlow<TvTab> = _selectedTab.asStateFlow()

    private val _currentMedia = MutableStateFlow<MediaItem?>(null)
    val currentMedia: StateFlow<MediaItem?> = _currentMedia.asStateFlow()

    private val _photoUrl = MutableStateFlow<String?>(null)
    val photoUrl: StateFlow<String?> = _photoUrl.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val audioTracks: StateFlow<List<com.localcast.tv.playback.AudioTrackInfo>> = playerManager.audioTracks

    fun cycleNextAudioTrack() {
        playerManager.cycleNextAudioTrack()
    }

    fun selectAudioTrack(track: com.localcast.tv.playback.AudioTrackInfo) {
        playerManager.selectAudioTrack(track)
    }

    fun getThumbnailUrl(mediaId: String, timeMs: Long): String {
        val quantizedTimeMs = (timeMs.coerceAtLeast(0L) / 5000L) * 5000L
        val streamUrl = playerManager.currentStreamUrl
        val baseUrl = if (streamUrl.contains("/api/media/stream/")) {
            streamUrl.substringBefore("/api/media/stream/")
        } else {
            val host = connectedHost ?: "127.0.0.1"
            "http://$host:$connectedPort"
        }
        return "$baseUrl/api/media/thumbnail/$mediaId?timeMs=$quantizedTimeMs"
    }

    val discoveredPhones: StateFlow<List<DiscoveredService>> = nsdDiscoverer.services

    private val _isProbingSubnet = MutableStateFlow(false)
    val isProbingSubnet: StateFlow<Boolean> = _isProbingSubnet.asStateFlow()

    val autoFullscreen = settingsRepository.autoFullscreen
    val preferredPlayer = settingsRepository.preferredPlayer
    val lastPlayedMedia = settingsRepository.lastPlayedMedia

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _resumeErrorMessage = MutableStateFlow<String?>(null)
    val resumeErrorMessage: StateFlow<String?> = _resumeErrorMessage.asStateFlow()

    private val _isResuming = MutableStateFlow(false)
    val isResuming: StateFlow<Boolean> = _isResuming.asStateFlow()

    private val _receivedApks = MutableStateFlow<List<ReceivedApkInfo>>(emptyList())
    val receivedApks: StateFlow<List<ReceivedApkInfo>> = _receivedApks.asStateFlow()

    private val _isWifiConnected = MutableStateFlow(false)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private val _localIp = MutableStateFlow<String?>(null)
    val localIp: StateFlow<String?> = _localIp.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    var connectedHost: String? = null
        private set
    var connectedPort: Int = 8844
        private set

    val playedMediaHistory: StateFlow<List<PlayedMediaItem>> = combine(
        settingsRepository.allPlayedMediaHistory,
        _connectedDeviceName,
        settingsRepository.savedServerHost,
        settingsRepository.savedServerName
    ) { allHistory, connDevName, savedHost, savedName ->
        val currentHost = connectedHost ?: savedHost
        val currentName = connDevName ?: savedName

        if (currentHost.isNullOrBlank() && currentName.isNullOrBlank()) {
            allHistory
        } else {
            allHistory.filter { item ->
                (!currentHost.isNullOrBlank() && item.serverHost.equals(currentHost, ignoreCase = true)) ||
                (!currentName.isNullOrBlank() && !item.serverName.isNullOrBlank() && item.serverName.equals(currentName, ignoreCase = true))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val clientDeviceSpecs by lazy {
        DeviceSpecs(
            deviceId = "tv_${Build.MODEL.replace(" ", "_")}",
            deviceName = "${Build.MANUFACTURER} TV (${Build.MODEL})",
            model = Build.MODEL,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            freeStorageBytes = try {
                val stat = StatFs(Environment.getDataDirectory().path)
                stat.availableBlocksLong * stat.blockSizeLong
            } catch (_: Exception) { 1024L * 1024L * 1024L },
            appVersionCode = 2
        )
    }

    private val tvClient = LocalTvClient { clientDeviceSpecs }
    val connectionState: StateFlow<ConnectionState> = tvClient.connectionState

    private val _dpadKeyEvents = MutableSharedFlow<Int>(extraBufferCapacity = 32)
    val dpadKeyEvents: SharedFlow<Int> = _dpadKeyEvents.asSharedFlow()

    companion object {
        private const val TAG = "TvMainViewModel"
    }

    init {
        loadSavedApks()
        viewModelScope.launch(Dispatchers.IO) {
            checkWifi()
            startDiscovery()
            tryAutoConnect()
        }
        observeIncomingMessages()
        observePlaybackChanges()
        observeInstallEvents()
        observeConnectionState()

        playerManager.onProgressCallback = { id, title, url, pos, dur, mime ->
            val now = System.currentTimeMillis()
            val isDifferentMedia = id != lastHistorySaveMediaId
            val isTimeElapsed = (now - lastHistorySaveTime) >= 10_000L
            val isBigSeek = kotlin.math.abs(pos - lastHistorySavePos) >= 10_000L

            if (isDifferentMedia || isTimeElapsed || isBigSeek) {
                lastHistorySaveTime = now
                lastHistorySavePos = pos
                lastHistorySaveMediaId = id
                val host = connectedHost ?: ""
                val name = _connectedDeviceName.value ?: ""
                viewModelScope.launch {
                    settingsRepository.savePlaybackProgress(
                        PlayedMediaItem(
                            id = id,
                            title = title,
                            streamUrl = url,
                            positionMs = pos,
                            durationMs = dur,
                            mimeType = mime,
                            serverHost = host,
                            serverName = name
                        )
                    )
                }
            }
        }

        playerManager.onPlaybackError = { errorMsg ->
            flushPlaybackProgress()
            _resumeErrorMessage.value = errorMsg
        }
    }

    private var lastHistorySaveTime = 0L
    private var lastHistorySavePos = -1L
    private var lastHistorySaveMediaId: String? = null

    private fun flushPlaybackProgress() {
        val media = _currentMedia.value
        val state = playerManager.playbackState.value
        if (media != null && state.positionMs > 0) {
            val host = connectedHost ?: ""
            val name = _connectedDeviceName.value ?: ""
            lastHistorySaveTime = System.currentTimeMillis()
            lastHistorySavePos = state.positionMs
            lastHistorySaveMediaId = media.id
            viewModelScope.launch {
                settingsRepository.savePlaybackProgress(
                    PlayedMediaItem(
                        id = media.id,
                        title = media.title,
                        streamUrl = media.uri,
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        mimeType = media.mimeType,
                        serverHost = host,
                        serverName = name
                    )
                )
            }
        }
    }

    fun selectTab(tab: TvTab) {
        _selectedTab.value = tab
    }

    fun checkWifi() {
        val app = getApplication<Application>()
        _isWifiConnected.value = NetworkUtils.isWifiConnected(app)
        _localIp.value = NetworkUtils.getWifiIpAddress(app)
    }

    fun startDiscovery() {
        nsdDiscoverer.startDiscovery()
    }

    fun probeSubnet() {
        val ip = _localIp.value ?: NetworkUtils.getWifiIpAddress(getApplication())
        if (ip.isNullOrBlank()) {
            checkWifi()
            return
        }
        if (_isProbingSubnet.value) return

        viewModelScope.launch {
            _isProbingSubnet.value = true
            try {
                subnetFastProbe.probeSubnet(
                    localIp = ip,
                    port = 8844,
                    onDeviceFound = { discovered ->
                        nsdDiscoverer.addExternalService(discovered)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Subnet probe error", e)
            } finally {
                _isProbingSubnet.value = false
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        connectedHost = state.host
                        connectedPort = state.port
                        if (!state.deviceName.isNullOrBlank()) {
                            _connectedDeviceName.value = state.deviceName
                            settingsRepository.savePairedServer(state.host, state.port, state.deviceName)
                        }
                    }
                    is ConnectionState.Disconnected,
                    is ConnectionState.Error -> {
                        connectedHost = null
                        _connectedDeviceName.value = null
                    }
                    else -> {}
                }
            }
        }
    }

    fun connectToPhone(host: String, port: Int, passwordOrToken: String, deviceName: String? = null) {
        connectedHost = host
        connectedPort = port
        if (!deviceName.isNullOrBlank()) {
            _connectedDeviceName.value = deviceName
        }

        viewModelScope.launch {
            settingsRepository.savePairedServer(host, port, deviceName)
        }

        tvClient.connect(
            host = host,
            port = port,
            authPasswordOrToken = passwordOrToken,
            onNewTokenReceived = { token ->
                settingsRepository.saveAuthToken(token)
            }
        )
    }

    fun reconnect() {
        viewModelScope.launch {
            val savedHost = settingsRepository.savedServerHost.first()
            val savedPort = settingsRepository.savedServerPort.first()
            val savedToken = settingsRepository.getAuthToken()
            val savedName = settingsRepository.savedServerName.first()

            if (!savedName.isNullOrBlank()) {
                _connectedDeviceName.value = savedName
            }

            if (!savedHost.isNullOrBlank() && !savedToken.isNullOrBlank()) {
                Log.d(TAG, "Reconnecting to previous server $savedHost:$savedPort with saved token")
                connectToPhone(savedHost, savedPort, savedToken, savedName)
            }
        }
    }

    private fun tryAutoConnect() {
        reconnect()
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            tvClient.incomingMessages.collect { msg ->
                when (msg) {
                    is SocketMessage.DeviceInfo -> {
                        _connectedDeviceName.value = msg.specs.deviceName
                        connectedHost?.let { host ->
                            settingsRepository.savePairedServer(host, connectedPort, msg.specs.deviceName)
                        }
                    }
                    is SocketMessage.PlayMedia -> {
                        handlePlayMedia(msg.media, msg.streamUrl, msg.startPositionMs)
                    }
                    is SocketMessage.ControlCommand -> {
                        handleControlCommand(msg.command, msg.value)
                    }
                    is SocketMessage.ApkTransferOffer -> {
                        handleApkOffer(msg)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handlePlayMedia(media: MediaItem, customStreamUrl: String, startPosMs: Long) {
        val host = connectedHost ?: "127.0.0.1"
        val streamUrl = if (customStreamUrl.isNotBlank()) {
            customStreamUrl
        } else {
            "http://$host:$connectedPort/api/media/stream/${media.id}"
        }
        _currentMedia.value = media

        viewModelScope.launch {
            val prefPlayer = settingsRepository.preferredPlayer.first()
            if (prefPlayer == SettingsRepository.PLAYER_EXTERNAL && media.mediaType != MediaType.IMAGE) {
                playInExternalPlayer(streamUrl, media.mimeType, media.title)
            } else if (media.mediaType == MediaType.IMAGE) {
                _photoUrl.value = streamUrl
                playerManager.stop()
                _selectedTab.value = TvTab.PLAYER
                _isFullscreen.value = true
            } else {
                _photoUrl.value = null
                playerManager.playMediaStream(streamUrl, media.title, media.id, media.mimeType, startPosMs)
                _selectedTab.value = TvTab.PLAYER
                _isFullscreen.value = true
            }
        }
    }

    private fun handleControlCommand(command: PlaybackCommand, value: Long) {
        when (command) {
            PlaybackCommand.PLAY -> playerManager.play()
            PlaybackCommand.PAUSE -> {
                flushPlaybackProgress()
                playerManager.pause()
            }
            PlaybackCommand.SEEK -> playerManager.seekTo(value)
            PlaybackCommand.STOP -> stopPlayback()
            PlaybackCommand.VOLUME_UP -> playerManager.setVolume(playerManager.playbackState.value.volume + 0.1f)
            PlaybackCommand.VOLUME_DOWN -> playerManager.setVolume(playerManager.playbackState.value.volume - 0.1f)
            PlaybackCommand.SET_VOLUME -> playerManager.setVolume(value / 100f)
            PlaybackCommand.NEXT -> playerManager.seekBy(10000)
            PlaybackCommand.PREVIOUS -> playerManager.seekBy(-10000)
            PlaybackCommand.SEEK_FORWARD -> playerManager.seekBy(10000)
            PlaybackCommand.SEEK_BACKWARD -> playerManager.seekBy(-10000)
            PlaybackCommand.DPAD_UP -> _dpadKeyEvents.tryEmit(KeyEvent.KEYCODE_DPAD_UP)
            PlaybackCommand.DPAD_DOWN -> _dpadKeyEvents.tryEmit(KeyEvent.KEYCODE_DPAD_DOWN)
            PlaybackCommand.DPAD_LEFT -> _dpadKeyEvents.tryEmit(KeyEvent.KEYCODE_DPAD_LEFT)
            PlaybackCommand.DPAD_RIGHT -> _dpadKeyEvents.tryEmit(KeyEvent.KEYCODE_DPAD_RIGHT)
            PlaybackCommand.DPAD_CENTER -> _dpadKeyEvents.tryEmit(KeyEvent.KEYCODE_DPAD_CENTER)
            PlaybackCommand.BACK -> _dpadKeyEvents.tryEmit(KeyEvent.KEYCODE_BACK)
        }
    }

    private fun handleApkOffer(offer: SocketMessage.ApkTransferOffer) {
        val host = connectedHost ?: return
        val port = connectedPort

        viewModelScope.launch {
            val installedCode = packageInstaller.getInstalledVersionCode(offer.packageName)
            val isDowngrade = installedCode != null && offer.versionCode > 0 && offer.versionCode < installedCode

            updateApkStatus(
                packageName = offer.packageName,
                appName = offer.appName,
                path = "",
                size = offer.sizeBytes,
                status = InstallStatus.DOWNLOADING,
                downloadProgress = 0.05f,
                versionCode = offer.versionCode,
                installedVersionCode = installedCode,
                isDowngrade = isDowngrade
            )
            tvClient.send(SocketMessage.InstallStatusUpdate(offer.packageName, InstallStatus.DOWNLOADING, "Downloading APK to TV storage..."))

            val result = apkDownloadManager.downloadAndVerify(host, port, offer) { progress ->
                updateApkStatus(
                    packageName = offer.packageName,
                    appName = offer.appName,
                    path = "",
                    size = offer.sizeBytes,
                    status = InstallStatus.DOWNLOADING,
                    downloadProgress = progress,
                    versionCode = offer.versionCode,
                    installedVersionCode = installedCode,
                    isDowngrade = isDowngrade
                )
            }

            result.onSuccess { finalFile ->
                val apkVer = packageInstaller.getApkVersionInfo(finalFile)
                val realCode = apkVer?.first ?: offer.versionCode
                val realName = apkVer?.second ?: ""
                val actualDowngrade = installedCode != null && realCode > 0 && realCode < installedCode

                if (actualDowngrade) {
                    // Block automatic downgrade prompt to prevent overwriting newer app version
                    val warnMsg = "Downgrade blocked: Incoming APK is Build $realCode (v$realName), but installed version is Build $installedCode."
                    Log.w(TAG, warnMsg)
                    updateApkStatus(
                        packageName = offer.packageName,
                        appName = offer.appName,
                        path = finalFile.absolutePath,
                        size = finalFile.length(),
                        status = InstallStatus.FAILED,
                        downloadProgress = 1.0f,
                        versionCode = realCode,
                        versionName = realName,
                        installedVersionCode = installedCode,
                        isDowngrade = true
                    )
                    tvClient.send(SocketMessage.InstallStatusUpdate(offer.packageName, InstallStatus.FAILED, warnMsg))
                } else {
                    updateApkStatus(
                        packageName = offer.packageName,
                        appName = offer.appName,
                        path = finalFile.absolutePath,
                        size = finalFile.length(),
                        status = InstallStatus.PROMPTING_USER,
                        downloadProgress = 1.0f,
                        versionCode = realCode,
                        versionName = realName,
                        installedVersionCode = installedCode,
                        isDowngrade = false
                    )
                    packageInstaller.installApk(finalFile, offer.packageName)
                }
            }.onFailure { error ->
                updateApkStatus(
                    packageName = offer.packageName,
                    appName = offer.appName,
                    path = "",
                    size = 0,
                    status = InstallStatus.FAILED,
                    downloadProgress = 0f,
                    versionCode = offer.versionCode,
                    installedVersionCode = installedCode,
                    isDowngrade = isDowngrade
                )
                tvClient.send(SocketMessage.InstallStatusUpdate(offer.packageName, InstallStatus.FAILED, error.localizedMessage ?: "Download failed"))
            }
        }
    }

    private fun updateApkStatus(
        packageName: String,
        appName: String,
        path: String,
        size: Long,
        status: InstallStatus,
        downloadProgress: Float = 1.0f,
        versionCode: Long = 0L,
        versionName: String = "",
        installedVersionCode: Long? = null,
        isDowngrade: Boolean = false
    ) {
        val current = _receivedApks.value.toMutableList()
        val index = current.indexOfFirst {
            (path.isNotBlank() && it.filePath == path) || (it.packageName == packageName && (it.filePath.isBlank() || it.filePath == path))
        }
        val item = ReceivedApkInfo(
            packageName = packageName,
            appName = appName,
            filePath = path,
            sizeBytes = size,
            status = status,
            downloadProgress = downloadProgress,
            versionCode = versionCode,
            versionName = versionName,
            installedVersionCode = installedVersionCode,
            isDowngrade = isDowngrade
        )
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        _receivedApks.value = current
    }

    fun loadSavedApks() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<ReceivedApkInfo>()
            val app = getApplication<Application>()
            val searchDirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: app.filesDir, "downloaded_apks")
            )
            val seenPaths = mutableSetOf<String>()
            val pm = app.packageManager

            for (dir in searchDirs) {
                if (!dir.exists() || !dir.isDirectory) continue
                val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".apk", ignoreCase = true) } ?: continue
                for (file in files) {
                    if (!seenPaths.add(file.absolutePath)) continue
                    try {
                        val pi = pm.getPackageArchiveInfo(file.absolutePath, 0)
                        val pkgName = pi?.packageName ?: file.nameWithoutExtension
                        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            pi?.longVersionCode ?: 0L
                        } else {
                            @Suppress("DEPRECATION")
                            pi?.versionCode?.toLong() ?: 0L
                        }
                        val name = pi?.versionName ?: ""
                        val appInfo = pi?.applicationInfo
                        if (appInfo != null) {
                            appInfo.sourceDir = file.absolutePath
                            appInfo.publicSourceDir = file.absolutePath
                        }
                        val appTitle = appInfo?.loadLabel(pm)?.toString() ?: file.nameWithoutExtension
                        val installedCode = packageInstaller.getInstalledVersionCode(pkgName)
                        val isDowngrade = installedCode != null && code > 0 && code < installedCode
                        val isInstalled = installedCode != null

                        list.add(
                            ReceivedApkInfo(
                                packageName = pkgName,
                                appName = appTitle,
                                filePath = file.absolutePath,
                                sizeBytes = file.length(),
                                status = if (isInstalled) InstallStatus.INSTALLED else InstallStatus.OFFERED,
                                downloadProgress = 1.0f,
                                timestamp = file.lastModified(),
                                versionCode = code,
                                versionName = name,
                                installedVersionCode = installedCode,
                                isDowngrade = isDowngrade
                            )
                        )
                    } catch (_: Exception) {}
                }
            }

            list.sortByDescending { it.timestamp }

            // Deduplicate items with identical package, version, and file size to keep the UI clean
            val deduplicated = mutableListOf<ReceivedApkInfo>()
            val seenKeys = mutableSetOf<String>()
            for (item in list) {
                val key = "${item.packageName}_${item.versionCode}_${item.sizeBytes}"
                if (seenKeys.add(key)) {
                    deduplicated.add(item)
                }
            }
            _receivedApks.value = deduplicated
        }
    }

    private fun observePlaybackChanges() {
        viewModelScope.launch {
            playerManager.playbackState.collectLatest { state ->
                tvClient.updatePlaybackState(state)
            }
        }
    }

    private fun observeInstallEvents() {
        viewModelScope.launch {
            InstallEventsBus.events.collect { event ->
                tvClient.send(SocketMessage.InstallStatusUpdate(event.packageName, event.status, event.message))
                _receivedApks.value = _receivedApks.value.map {
                    if (it.packageName == event.packageName) it.copy(status = event.status) else it
                }
            }
        }
    }

    fun reinstallApk(apk: ReceivedApkInfo) {
        val file = File(apk.filePath)
        if (file.exists()) {
            packageInstaller.installApk(file, apk.packageName)
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val app = getApplication<Application>()
            val intent = app.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app $packageName", e)
            false
        }
    }

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            val app = getApplication<Application>()
            app.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun updateAutoFullscreen(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.saveAutoFullscreen(enabled) }
    }

    fun updatePreferredPlayer(player: String) {
        viewModelScope.launch { settingsRepository.savePreferredPlayer(player) }
    }

    fun stopPlayback() {
        flushPlaybackProgress()
        playerManager.stop()
        _currentMedia.value = null
        _photoUrl.value = null
        _isFullscreen.value = false
        _selectedTab.value = TvTab.PLAYER
    }

    fun playInExternalPlayer(streamUrl: String? = null, mimeType: String? = null, title: String? = null) {
        val url = streamUrl ?: playerManager.currentStreamUrl
        val mime = mimeType ?: playerManager.currentMimeType
        val t = title ?: currentMedia.value?.title ?: "Video"
        if (url.isBlank()) return

        try {
            playerManager.pause()
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), mime)
                putExtra("title", t)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, "Play in VLC / External Player").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start external player", e)
        }
    }

    fun resumeLastPlayed(lastPlayed: LastPlayedMedia) {
        viewModelScope.launch {
            _resumeErrorMessage.value = null
            _isResuming.value = true
            try {
                // 1. Dynamically resolve host and port
                val savedHost = settingsRepository.savedServerHost.first()
                val savedPort = settingsRepository.savedServerPort.first() ?: 8844
                val activeHost = connectedHost ?: savedHost
                val activePort = if (connectedPort > 0) connectedPort else savedPort

                if (activeHost.isNullOrBlank()) {
                    _resumeErrorMessage.value = "Phone is not connected. Please connect your phone first."
                    _isResuming.value = false
                    return@launch
                }

                val streamUrl = if (lastPlayed.id.isNotBlank()) {
                    "http://$activeHost:$activePort/api/media/stream/${lastPlayed.id}"
                } else {
                    lastPlayed.streamUrl
                }

                // If not currently connected, attempt reconnect in background so phone remote works
                if (connectionState.value !is ConnectionState.Connected) {
                    reconnect()
                }

                // 2. Lightly probe stream availability using Range: bytes=0-0 with a 3-second timeout
                // prepareGet with Range request downloads at most 1 byte and does NOT buffer the video into memory
                val isAvailable = try {
                    withTimeout(3000L) {
                        httpClient.prepareGet(streamUrl) {
                            header(HttpHeaders.Range, "bytes=0-0")
                        }.execute { resp ->
                            resp.status.value in 200..299
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Stream probe failed: ${e.message}")
                    false
                }

                if (!isAvailable) {
                    _resumeErrorMessage.value = "Cannot reach '${lastPlayed.title}' on phone. Make sure phone server is active."
                    _isResuming.value = false
                    return@launch
                }

                // 3. Media is available! Prepare ExoPlayer and transition to player screen
                val safePosition = if (lastPlayed.durationMs > 0 && lastPlayed.positionMs >= lastPlayed.durationMs - 2000L) {
                    0L
                } else {
                    lastPlayed.positionMs.coerceAtLeast(0L)
                }

                _currentMedia.value = MediaItem(
                    id = lastPlayed.id,
                    title = lastPlayed.title,
                    uri = streamUrl,
                    mimeType = lastPlayed.mimeType,
                    mediaType = MediaType.VIDEO,
                    durationMs = lastPlayed.durationMs
                )
                _photoUrl.value = null
                playerManager.playMediaStream(streamUrl, lastPlayed.title, lastPlayed.id, lastPlayed.mimeType, safePosition)
                _selectedTab.value = TvTab.PLAYER
                _isFullscreen.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume media", e)
                _resumeErrorMessage.value = "Cannot reach phone to resume '${lastPlayed.title}'."
            } finally {
                _isResuming.value = false
            }
        }
    }

    fun playHistoryItem(item: PlayedMediaItem) {
        viewModelScope.launch {
            _resumeErrorMessage.value = null
            _isResuming.value = true
            try {
                val savedHost = settingsRepository.savedServerHost.first()
                val savedPort = settingsRepository.savedServerPort.first()
                val activeHost = connectedHost ?: if (item.serverHost.isNotBlank()) item.serverHost else savedHost
                val activePort = if (connectedPort > 0) connectedPort else savedPort

                if (activeHost.isNullOrBlank()) {
                    _resumeErrorMessage.value = "Phone is not connected. Please connect your phone first."
                    _isResuming.value = false
                    return@launch
                }

                val streamUrl = if (item.id.isNotBlank()) {
                    "http://$activeHost:$activePort/api/media/stream/${item.id}"
                } else {
                    item.streamUrl
                }

                if (connectionState.value !is ConnectionState.Connected) {
                    reconnect()
                }

                val isAvailable = try {
                    withTimeout(3000L) {
                        httpClient.prepareGet(streamUrl) {
                            header(HttpHeaders.Range, "bytes=0-0")
                        }.execute { resp ->
                            resp.status.value in 200..299
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Stream probe failed: ${e.message}")
                    false
                }

                if (!isAvailable) {
                    _resumeErrorMessage.value = "Cannot reach '${item.title}' on phone. Make sure phone server is active."
                    _isResuming.value = false
                    return@launch
                }

                val safePosition = if (item.durationMs > 0 && item.positionMs >= item.durationMs - 2000L) {
                    0L
                } else {
                    item.positionMs.coerceAtLeast(0L)
                }

                val isAudio = item.mimeType.startsWith("audio/")
                _currentMedia.value = MediaItem(
                    id = item.id,
                    title = item.title,
                    uri = streamUrl,
                    mimeType = item.mimeType,
                    mediaType = if (isAudio) MediaType.AUDIO else MediaType.VIDEO,
                    durationMs = item.durationMs
                )
                _photoUrl.value = null
                playerManager.playMediaStream(streamUrl, item.title, item.id, item.mimeType, safePosition)
                _selectedTab.value = TvTab.PLAYER
                _isFullscreen.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play history item", e)
                _resumeErrorMessage.value = "Cannot reach phone to play '${item.title}'."
            } finally {
                _isResuming.value = false
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val host = connectedHost
            val name = _connectedDeviceName.value
            settingsRepository.clearPlayedMediaHistory(host, name)
            settingsRepository.clearLastPlayed()
        }
    }

    fun dismissResumePrompt() {
        viewModelScope.launch { settingsRepository.clearLastPlayed() }
    }

    fun clearResumeError() {
        _resumeErrorMessage.value = null
    }

    fun unpairAndDisconnect() {
        viewModelScope.launch {
            settingsRepository.clearAuthToken()
            tvClient.disconnect()
        }
    }

    override fun onCleared() {
        flushPlaybackProgress()
        try {
            httpClient.close()
        } catch (_: Exception) {}
        playerManager.release()
        nsdDiscoverer.stopDiscovery()
        tvClient.close()
        super.onCleared()
    }
}
