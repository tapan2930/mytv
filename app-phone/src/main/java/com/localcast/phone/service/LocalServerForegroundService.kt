package com.localcast.phone.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.localcast.core.model.DeviceSpecs
import com.localcast.core.model.MediaItem
import com.localcast.core.model.PlaybackCommand
import com.localcast.core.model.PlaybackState
import com.localcast.core.model.SocketMessage
import com.localcast.core.network.NetworkUtils
import com.localcast.core.network.discovery.NsdAdvertiser
import com.localcast.core.network.server.LocalHttpServer
import com.localcast.core.network.server.MediaProvider
import com.localcast.core.network.storage.SettingsRepository
import com.localcast.phone.MainActivity
import com.localcast.phone.data.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class LocalServerForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var mediaRepository: MediaRepository
    private lateinit var nsdAdvertiser: NsdAdvertiser
    private var httpServer: LocalHttpServer? = null
    private var mediaSession: MediaSessionCompat? = null

    companion object {
        const val CHANNEL_ID = "my_tv_server_channel_v3"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "ACTION_PREV"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_REWIND = "ACTION_REWIND"
        const val ACTION_FAST_FORWARD = "ACTION_FAST_FORWARD"
        private const val TAG = "ServerService"

        fun startService(context: Context) {
            val intent = Intent(context, LocalServerForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocalServerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        mediaRepository = MediaRepository(this)
        nsdAdvertiser = NsdAdvertiser(this)

        initMediaSession()
        createNotificationChannel()
        acquireLocks()
        observePlaybackForNotification()
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "MyTvMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    ServerStateHolder.broadcast(SocketMessage.ControlCommand(PlaybackCommand.PLAY))
                }

                override fun onPause() {
                    ServerStateHolder.broadcast(SocketMessage.ControlCommand(PlaybackCommand.PAUSE))
                }

                override fun onSkipToNext() {
                    ServerStateHolder.playNext()
                }

                override fun onSkipToPrevious() {
                    ServerStateHolder.playPrevious()
                }

                override fun onRewind() {
                    ServerStateHolder.broadcast(SocketMessage.ControlCommand(PlaybackCommand.SEEK_BACKWARD))
                }

                override fun onFastForward() {
                    ServerStateHolder.broadcast(SocketMessage.ControlCommand(PlaybackCommand.SEEK_FORWARD))
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundServer()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PLAY_PAUSE -> {
                if (ServerStateHolder.playbackState.value.isPlaying) {
                    ServerStateHolder.broadcast(SocketMessage.ControlCommand(PlaybackCommand.PAUSE))
                } else {
                    ServerStateHolder.broadcast(SocketMessage.ControlCommand(PlaybackCommand.PLAY))
                }
            }
            ACTION_PREV -> {
                ServerStateHolder.playPrevious()
            }
            ACTION_NEXT -> {
                ServerStateHolder.playNext()
            }
            ACTION_REWIND -> {
                ServerStateHolder.broadcast(SocketMessage.ControlCommand(PlaybackCommand.SEEK_BACKWARD))
            }
            ACTION_FAST_FORWARD -> {
                ServerStateHolder.broadcast(SocketMessage.ControlCommand(PlaybackCommand.SEEK_FORWARD))
            }
            ACTION_START, null -> {
                startForegroundServer()
            }
        }
        return START_STICKY
    }

    private fun observePlaybackForNotification() {
        scope.launch {
            combine(
                ServerStateHolder.isServerRunning,
                ServerStateHolder.nowPlayingMedia,
                ServerStateHolder.playbackState,
                ServerStateHolder.connectedTvSpecs
            ) { isRunning, media, state, tv ->
                if (isRunning) {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, buildNotification(media, state, tv, null))
                }
            }.collect {}
        }
    }

    private fun startForegroundServer() {
        val notification = createNotification("Starting server...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        scope.launch {
            if (!NetworkUtils.isWifiConnected(this@LocalServerForegroundService)) {
                updateNotification("Wi-Fi not connected! Server waiting...")
                ServerStateHolder.updateServerRunning(false)
                return@launch
            }

            val wifiIp = NetworkUtils.getWifiIpAddress(this@LocalServerForegroundService) ?: "127.0.0.1"
            val port = settingsRepository.getServerPort()
            val password = settingsRepository.getServerPassword()
            val token = settingsRepository.getAuthToken()
            val deviceId = settingsRepository.getOrCreateDeviceId()
            val deviceName = settingsRepository.getDeviceName(Build.MODEL)

            val apkDir = File(cacheDir, "received_apks")

            httpServer = LocalHttpServer(
                expectedPasswordProvider = { password },
                expectedTokenProvider = { token },
                mediaProvider = object : MediaProvider {
                    override fun getMediaFile(id: String): File? {
                        val item = ServerStateHolder.findMedia(id)
                        if (item != null) {
                            val f = mediaRepository.getFileFromUri(item.uri)
                            if (f != null && f.exists() && f.canRead()) return f
                        }
                        return mediaRepository.getFileFromUri("content://media/external/video/media/$id")
                            ?: mediaRepository.getFileFromUri("content://media/external/audio/media/$id")
                            ?: mediaRepository.getFileFromUri("content://media/external/images/media/$id")
                            ?: mediaRepository.getFileFromUri("content://media/external/file/$id")
                    }

                    override fun getMimeType(id: String): String? {
                        val item = ServerStateHolder.findMedia(id)
                        if (item != null) return item.mimeType
                        return null
                    }

                    override fun getApkFile(packageName: String): File? {
                        val found = ServerStateHolder.findApk(packageName)
                        if (found != null) {
                            val f = mediaRepository.getFileFromUri(found.uri)
                            if (f != null && f.exists() && f.canRead()) return f
                        }
                        return null
                    }
                },
                apkStorageDir = apkDir,
                serverDeviceNameProvider = { deviceName }
            )

            ServerStateHolder.serverInstance = httpServer
            httpServer?.start("0.0.0.0", port)
            ServerStateHolder.updateServerRunning(true, wifiIp, port)

            // Start NSD advertiser and UDP beacon so Android TV can auto-discover
            nsdAdvertiser.startAdvertising(
                serviceName = "LocalCast-${deviceName.take(8)}",
                port = port,
                deviceName = deviceName,
                deviceId = deviceId,
                hostIp = wifiIp
            )

            updateNotification("Online: $wifiIp:$port")

            // Listen to messages from TV
            httpServer?.incomingMessages?.collect { msg ->
                when (msg) {
                    is SocketMessage.DeviceInfo -> {
                        ServerStateHolder.updateConnectedTv(msg.specs)
                        updateNotification("Connected to: ${msg.specs.deviceName}")
                    }
                    is SocketMessage.StateUpdate -> {
                        ServerStateHolder.updatePlaybackState(msg.state)
                    }
                    is SocketMessage.InstallStatusUpdate -> {
                        ServerStateHolder.updateInstallStatus(msg)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun stopForegroundServer() {
        httpServer?.stop()
        httpServer = null
        ServerStateHolder.serverInstance = null
        ServerStateHolder.updateServerRunning(false)
        ServerStateHolder.updateConnectedTv(null)
        nsdAdvertiser.stopAdvertising()
        releaseLocks()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

    private fun acquireLocks() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocalCast::ServerWakeLock").apply {
            setReferenceCounted(false)
            acquire(24 * 60 * 60 * 1000L) // 24 hours max safeguard
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = wifiManager.createWifiLock(wifiMode, "LocalCast::ServerWifiLock").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing locks", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            try {
                manager.deleteNotificationChannel("local_cast_server_channel")
            } catch (_: Exception) {}

            val channel = NotificationChannel(
                CHANNEL_ID,
                "My TV Server Status",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows persistent status and controls while My TV server is running on local Wi-Fi"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        return buildNotification(
            media = ServerStateHolder.nowPlayingMedia.value,
            state = ServerStateHolder.playbackState.value,
            tv = ServerStateHolder.connectedTvSpecs.value,
            customStatus = content
        )
    }

    private fun buildNotification(
        media: MediaItem?,
        state: PlaybackState,
        tv: DeviceSpecs?,
        customStatus: String? = null
    ): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pMainIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
            val intent = Intent(this, LocalServerForegroundService::class.java).apply {
                this.action = action
            }
            return PendingIntent.getService(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val pStop = servicePendingIntent(ACTION_STOP, 1)
        val ip = ServerStateHolder.serverIp.value ?: NetworkUtils.getWifiIpAddress(this) ?: "Wi-Fi"
        val port = ServerStateHolder.serverPort.value

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.localcast.phone.R.drawable.ic_qs_mytv_server)
            .setContentIntent(pMainIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        if (media != null) {
            // Media actively streaming to TV
            val title = media.title
            val subtitle = if (tv != null) "Playing on ${tv.deviceName}" else (customStatus ?: "Streaming • $ip:$port")

            mediaSession?.let { session ->
                val metaBuilder = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_ARTIST,
                        if (tv != null) "Casting to ${tv.deviceName}" else "Streaming • $ip:$port"
                    )
                if (state.durationMs > 0) {
                    metaBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, state.durationMs)
                }
                session.setMetadata(metaBuilder.build())

                val pbState = PlaybackStateCompat.Builder()
                    .setState(
                        if (state.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                        state.positionMs,
                        1.0f
                    )
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_REWIND or
                        PlaybackStateCompat.ACTION_FAST_FORWARD
                    )
                    .build()
                session.setPlaybackState(pbState)
            }

            builder.setContentTitle(title)
                .setContentText(subtitle)
                .setSubText("My TV • $ip:$port")

            val pPrev = servicePendingIntent(ACTION_PREV, 2)
            val pRewind = servicePendingIntent(ACTION_REWIND, 3)
            val pPlayPause = servicePendingIntent(ACTION_PLAY_PAUSE, 4)
            val pForward = servicePendingIntent(ACTION_FAST_FORWARD, 5)
            val pNext = servicePendingIntent(ACTION_NEXT, 6)

            builder.addAction(android.R.drawable.ic_media_previous, "Prev", pPrev)
            builder.addAction(android.R.drawable.ic_media_rew, "-10s", pRewind)
            if (state.isPlaying) {
                builder.addAction(android.R.drawable.ic_media_pause, "Pause", pPlayPause)
            } else {
                builder.addAction(android.R.drawable.ic_media_play, "Play", pPlayPause)
            }
            builder.addAction(android.R.drawable.ic_media_ff, "+10s", pForward)
            builder.addAction(android.R.drawable.ic_media_next, "Next", pNext)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Server", pStop)

            val style = MediaStyle()
                .setMediaSession(mediaSession?.sessionToken)
                .setShowActionsInCompactView(1, 2, 3)

            builder.setStyle(style)
        } else {
            // Persistent notification when Server is running without active media playback
            val title = "My TV Server is Running"
            val subtitle = if (customStatus != null) {
                customStatus
            } else if (tv != null) {
                "Connected: ${tv.deviceName} • $ip:$port"
            } else {
                "Online: $ip:$port • Ready for TV connection"
            }

            builder.setContentTitle(title)
                .setContentText(subtitle)
                .setSubText("Local Wi-Fi Server")
                .addAction(android.R.drawable.ic_menu_view, "Open App", pMainIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Server", pStop)
        }

        return builder.build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(content))
    }

    override fun onDestroy() {
        stopForegroundServer()
        mediaSession?.release()
        mediaSession = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
