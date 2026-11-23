package com.localcast.core.network.server

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import android.util.LruCache
import com.localcast.core.model.PlaybackState
import com.localcast.core.model.SocketMessage
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.server.engine.EmbeddedServer
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.time.Duration.Companion.seconds

interface MediaProvider {
    fun getMediaFile(id: String): File?
    fun getMimeType(id: String): String?
    fun getApkFile(packageName: String): File? = null
}

class LocalHttpServer(
    private val expectedPasswordProvider: () -> String,
    private val expectedTokenProvider: () -> String?,
    private val mediaProvider: MediaProvider,
    private val apkStorageDir: File,
    private val serverDeviceNameProvider: (() -> String)? = null
) {
    private var server: EmbeddedServer<*, *>? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val thumbnailCache = LruCache<String, ByteArray>(64)

    private val connectedSessions = mutableSetOf<io.ktor.websocket.WebSocketSession>()

    private val _incomingMessages = MutableSharedFlow<SocketMessage>()
    val incomingMessages: SharedFlow<SocketMessage> = _incomingMessages.asSharedFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    companion object {
        private const val TAG = "LocalHttpServer"
    }

    fun start(host: String, port: Int) {
        if (_isServerRunning.value) return

        try {
            server = embeddedServer(CIO, port = port, host = host) {
                configurePlugins()
                configureRouting()
            }.start(wait = false)

            _isServerRunning.value = true
            Log.d(TAG, "LocalHttpServer started on $host:$port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocalHttpServer", e)
            _isServerRunning.value = false
        }
    }

    fun stop() {
        try {
            val sessionsToClose = synchronized(connectedSessions) {
                val list = connectedSessions.toList()
                connectedSessions.clear()
                list
            }
            scope.launch {
                sessionsToClose.forEach { session ->
                    try {
                        session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server stopped"))
                    } catch (_: Exception) {}
                }
            }
            server?.stop(500, 1000)
            server = null
            _isServerRunning.value = false
            _connectedClientsCount.value = 0
            Log.d(TAG, "LocalHttpServer stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping server", e)
        }
    }

    private fun Application.configurePlugins() {
        install(PartialContent) {
            maxRangeCount = 10
        }
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.Range)
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                Log.e(TAG, "Unhandled server error", cause)
                call.respondText(
                    text = "Internal Server Error: ${cause.localizedMessage}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
        install(WebSockets) {
            pingPeriod = 3.seconds
            timeout = 4.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
    }

    private fun Application.configureRouting() {
        routing {
            get("/api/status") {
                val name = serverDeviceNameProvider?.invoke() ?: "LocalCast Server"
                call.respond(mapOf(
                    "status" to "online",
                    "service" to "LocalCastPhoneServer",
                    "deviceName" to name
                ))
            }

            // Download APK
            get("/api/apk/download/{packageName}") {
                val pkgName = call.parameters["packageName"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val file = mediaProvider.getApkFile(pkgName)
                if (file == null || !file.exists()) {
                    call.respond(HttpStatusCode.NotFound, "APK file not found")
                    return@get
                }
                call.response.header(HttpHeaders.ContentType, "application/vnd.android.package-archive")
                call.respondFile(file)
            }

            // Stream Media with HTTP 206 Range support for ExoPlayer
            get("/api/media/stream/{id}") {
                val mediaId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val file = mediaProvider.getMediaFile(mediaId)
                if (file == null || !file.exists()) {
                    call.respond(HttpStatusCode.NotFound, "Media file not found")
                    return@get
                }

                val mime = mediaProvider.getMimeType(mediaId) ?: "application/octet-stream"
                call.response.header(HttpHeaders.AcceptRanges, "bytes")
                call.response.header(HttpHeaders.ContentType, mime)
                call.respondFile(file)
            }

            // Thumbnail extraction route for video/image previews (supports ?timeMs=...)
            get("/api/media/thumbnail/{id}") {
                val mediaId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val file = mediaProvider.getMediaFile(mediaId)
                if (file == null || !file.exists()) {
                    call.respond(HttpStatusCode.NotFound, "Media not found")
                    return@get
                }

                val mime = mediaProvider.getMimeType(mediaId) ?: ""
                val ext = file.extension.lowercase()
                val isVideo = mime.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "flv")
                val isImage = mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

                if (isVideo) {
                    val rawTimeMs = call.request.queryParameters["timeMs"]?.toLongOrNull() ?: 2000L
                    val timeMs = rawTimeMs.coerceAtLeast(0L)
                    val cacheKey = "${mediaId}_${timeMs}"

                    val cachedBytes = synchronized(thumbnailCache) {
                        thumbnailCache.get(cacheKey)
                    }
                    if (cachedBytes != null) {
                        call.response.header(HttpHeaders.CacheControl, "public, max-age=86400")
                        call.respondBytes(cachedBytes, ContentType.Image.JPEG)
                        return@get
                    }

                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.absolutePath)
                        val timeUs = timeMs * 1000L
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 180)
                                ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: retriever.frameAtTime
                        } else {
                            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: retriever.frameAtTime
                        }

                        if (bitmap != null) {
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                            val bytes = stream.toByteArray()
                            bitmap.recycle()

                            synchronized(thumbnailCache) {
                                thumbnailCache.put(cacheKey, bytes)
                            }

                            call.response.header(HttpHeaders.CacheControl, "public, max-age=86400")
                            call.respondBytes(bytes, ContentType.Image.JPEG)
                            return@get
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Thumbnail extraction error for $mediaId at timeMs=$timeMs", e)
                    } finally {
                        try {
                            retriever.release()
                        } catch (_: Throwable) {}
                    }
                } else if (isImage) {
                    call.response.header(HttpHeaders.CacheControl, "public, max-age=86400")
                    call.respondFile(file)
                    return@get
                }

                call.respond(HttpStatusCode.NotFound, "Thumbnail not available")
            }

            // Upload APK
            post("/api/apk/upload") {
                val packageName = call.request.headers["X-Package-Name"] ?: "app_${System.currentTimeMillis()}"
                val expectedSha256 = call.request.headers["X-File-SHA256"]

                if (!apkStorageDir.exists()) apkStorageDir.mkdirs()
                val targetFile = File(apkStorageDir, "$packageName.apk")

                val channel = call.receiveChannel()
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)

                channel.toInputStream().use { inputStream ->
                    FileOutputStream(targetFile).use { fos ->
                        var read = inputStream.read(buffer)
                        while (read != -1) {
                            fos.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            read = inputStream.read(buffer)
                        }
                        fos.flush()
                    }
                }

                val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                if (expectedSha256 != null && !expectedSha256.equals(actualSha256, ignoreCase = true)) {
                    targetFile.delete()
                    call.respond(HttpStatusCode.BadRequest, "Checksum mismatch")
                    return@post
                }

                call.respond(
                    HttpStatusCode.OK,
                    mapOf("status" to "success", "filePath" to targetFile.absolutePath, "sha256" to actualSha256)
                )
            }

            // WebSocket Control Channel
            webSocket("/ws") {
                var isAuthenticated = false
                var clientName = "Unknown"

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            handleIncomingFrameText(text, this, onAuthSuccess = { name ->
                                isAuthenticated = true
                                clientName = name
                                synchronized(connectedSessions) {
                                    connectedSessions.add(this)
                                    _connectedClientsCount.value = connectedSessions.size
                                }
                            })
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    Log.d(TAG, "WebSocket client disconnected: $clientName")
                } catch (e: Throwable) {
                    Log.e(TAG, "WebSocket error for $clientName", e)
                } finally {
                    synchronized(connectedSessions) {
                        connectedSessions.remove(this)
                        _connectedClientsCount.value = connectedSessions.size
                    }
                }
            }
        }
    }

    private suspend fun handleIncomingFrameText(
        text: String,
        session: io.ktor.websocket.WebSocketSession,
        onAuthSuccess: (String) -> Unit
    ) {
        try {
            val message = json.decodeFromString<SocketMessage>(text)
            when (message) {
                is SocketMessage.AuthRequest -> {
                    val expectedPass = expectedPasswordProvider()
                    val expectedToken = expectedTokenProvider()

                    val isValid = message.token == expectedPass || (expectedToken != null && message.token == expectedToken)
                    if (isValid) {
                        val sessionToken = expectedToken ?: expectedPass
                        val serverName = serverDeviceNameProvider?.invoke()
                        session.send(
                            Frame.Text(
                                json.encodeToString<SocketMessage>(
                                    SocketMessage.AuthResponse(
                                        success = true,
                                        message = "Authenticated",
                                        sessionToken = sessionToken,
                                        serverDeviceName = serverName
                                    )
                                )
                            )
                        )
                        onAuthSuccess(message.clientName)
                    } else {
                        session.send(
                            Frame.Text(
                                json.encodeToString<SocketMessage>(
                                    SocketMessage.AuthResponse(success = false, message = "Invalid password or token")
                                )
                            )
                        )
                    }
                }
                is SocketMessage.Ping -> {
                    session.send(Frame.Text(json.encodeToString<SocketMessage>(SocketMessage.Pong())))
                }
                else -> {
                    _incomingMessages.emit(message)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse incoming socket text: $text", e)
        }
    }

    fun broadcast(message: SocketMessage) {
        scope.launch {
            val payload = json.encodeToString(message)
            val sessionsToNotify = synchronized(connectedSessions) { connectedSessions.toList() }
            for (s in sessionsToNotify) {
                try {
                    s.send(Frame.Text(payload))
                } catch (e: Exception) {
                    Log.w(TAG, "Error sending frame to client", e)
                }
            }
        }
    }
}
