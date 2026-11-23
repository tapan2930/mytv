package com.localcast.core.network.client

import android.util.Log
import com.localcast.core.model.DeviceSpecs
import com.localcast.core.model.PlaybackCommand
import com.localcast.core.model.PlaybackState
import com.localcast.core.model.SocketMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

import kotlin.time.Duration.Companion.seconds

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Authenticating(val host: String, val port: Int) : ConnectionState()
    data class Connected(val host: String, val port: Int, val deviceName: String? = null) : ConnectionState()
    data class AuthFailed(val reason: String) : ConnectionState()
    data class Error(val error: Throwable) : ConnectionState()
}

class LocalTvClient(
    private val clientDeviceSpecsProvider: () -> DeviceSpecs
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient = HttpClient(CIO) {
        install(WebSockets)
        install(ContentNegotiation) {
            json(json)
        }
    }

    private var connectionJob: Job? = null
    private var activeSession: DefaultClientWebSocketSession? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<SocketMessage>()
    val incomingMessages: SharedFlow<SocketMessage> = _incomingMessages.asSharedFlow()

    companion object {
        private const val TAG = "LocalTvClient"
    }

    fun connect(host: String, port: Int, authPasswordOrToken: String, onNewTokenReceived: (suspend (String) -> Unit)? = null) {
        disconnect()

        connectionJob = scope.launch {
            _connectionState.value = ConnectionState.Connecting
            try {
                httpClient.webSocket(
                    method = HttpMethod.Get,
                    host = host,
                    port = port,
                    path = "/ws"
                ) {
                    activeSession = this
                    pingIntervalMillis = 3000L
                    timeoutMillis = 4000L
                    _connectionState.value = ConnectionState.Authenticating(host, port)

                    val specs = clientDeviceSpecsProvider()
                    val authReq = SocketMessage.AuthRequest(
                        token = authPasswordOrToken,
                        clientName = specs.deviceName,
                        clientRole = "AndroidTV"
                    )
                    send(Frame.Text(json.encodeToString<SocketMessage>(authReq)))

                    // Listen to incoming messages
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            handleMessage(text, onNewTokenReceived, specs, host, port)
                        }
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                Log.d(TAG, "Connection closed cleanly")
                _connectionState.value = ConnectionState.Disconnected
            } catch (e: CancellationException) {
                _connectionState.value = ConnectionState.Disconnected
            } catch (e: Throwable) {
                Log.e(TAG, "Connection error", e)
                _connectionState.value = ConnectionState.Disconnected
            } finally {
                activeSession = null
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.handleMessage(
        text: String,
        onNewTokenReceived: (suspend (String) -> Unit)?,
        specs: DeviceSpecs,
        host: String,
        port: Int
    ) {
        try {
            val message = json.decodeFromString<SocketMessage>(text)
            when (message) {
                is SocketMessage.AuthResponse -> {
                    if (message.success) {
                        _connectionState.value = ConnectionState.Connected(host, port, message.serverDeviceName)
                        message.sessionToken?.let { token ->
                            onNewTokenReceived?.invoke(token)
                        }
                        // Send device specs to Phone
                        send(Frame.Text(json.encodeToString<SocketMessage>(SocketMessage.DeviceInfo(specs))))
                    } else {
                        _connectionState.value = ConnectionState.AuthFailed(message.message)
                    }
                }
                else -> {
                    _incomingMessages.emit(message)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode message: $text", e)
        }
    }

    fun send(message: SocketMessage) {
        scope.launch {
            val session = activeSession
            if (session != null && session.isActive) {
                try {
                    session.send(Frame.Text(json.encodeToString(message)))
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending socket message", e)
                }
            }
        }
    }

    fun updatePlaybackState(state: PlaybackState) {
        send(SocketMessage.StateUpdate(state))
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        scope.launch {
            try {
                activeSession?.close()
            } catch (_: Exception) {}
            activeSession = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun close() {
        disconnect()
        try {
            httpClient.close()
        } catch (_: Exception) {}
    }
}
