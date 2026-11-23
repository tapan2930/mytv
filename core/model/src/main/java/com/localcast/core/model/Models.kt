package com.localcast.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType {
    VIDEO,
    AUDIO,
    IMAGE,
    APK,
    DOCUMENT
}

@Serializable
data class MediaItem(
    val id: String,
    val title: String,
    val uri: String,
    val mimeType: String,
    val mediaType: MediaType,
    val sizeBytes: Long = 0,
    val durationMs: Long = 0,
    val artist: String? = null,
    val album: String? = null,
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class DeviceSpecs(
    val deviceId: String,
    val deviceName: String,
    val model: String,
    val supportedAbis: List<String>,
    val freeStorageBytes: Long,
    val appVersionCode: Long
)

@Serializable
enum class PlaybackCommand {
    PLAY,
    PAUSE,
    SEEK,
    STOP,
    VOLUME_UP,
    VOLUME_DOWN,
    SET_VOLUME,
    NEXT,
    PREVIOUS,
    SEEK_FORWARD,
    SEEK_BACKWARD,
    DPAD_UP,
    DPAD_DOWN,
    DPAD_LEFT,
    DPAD_RIGHT,
    DPAD_CENTER,
    BACK
}

@Serializable
data class PlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val currentMediaId: String? = null,
    val title: String? = null,
    val volume: Float = 1.0f,
    val isEnded: Boolean = false
)

@Serializable
enum class InstallStatus {
    OFFERED,
    DOWNLOADING,
    VERIFYING,
    PROMPTING_USER,
    INSTALLED,
    FAILED
}

@Serializable
sealed class SocketMessage {
    @Serializable
    data class AuthRequest(val token: String, val clientName: String, val clientRole: String) : SocketMessage()

    @Serializable
    data class AuthResponse(
        val success: Boolean,
        val message: String,
        val sessionToken: String? = null,
        val serverDeviceName: String? = null
    ) : SocketMessage()

    @Serializable
    data class DeviceInfo(val specs: DeviceSpecs) : SocketMessage()

    @Serializable
    data class PlayMedia(
        val media: MediaItem,
        val streamUrl: String = "",
        val startPositionMs: Long = 0
    ) : SocketMessage()

    @Serializable
    data class ControlCommand(val command: PlaybackCommand, val value: Long = 0) : SocketMessage()

    @Serializable
    data class StateUpdate(val state: PlaybackState) : SocketMessage()

    @Serializable
    data class ApkTransferOffer(
        val packageName: String,
        val appName: String,
        val versionCode: Long,
        val sizeBytes: Long,
        val sha256: String,
        val supportedAbis: List<String> = emptyList()
    ) : SocketMessage()

    @Serializable
    data class ApkTransferAccept(val accepted: Boolean, val reason: String? = null) : SocketMessage()

    @Serializable
    data class InstallStatusUpdate(
        val packageName: String,
        val status: InstallStatus,
        val message: String? = null
    ) : SocketMessage()

    @Serializable
    data class Ping(val timestamp: Long = System.currentTimeMillis()) : SocketMessage()

    @Serializable
    data class Pong(val timestamp: Long = System.currentTimeMillis()) : SocketMessage()
}
