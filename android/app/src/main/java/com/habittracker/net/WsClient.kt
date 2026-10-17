package com.habittracker.net

import com.habittracker.data.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ConnState { OFFLINE, CONNECTING, ONLINE }

class WsClient(private val store: TokenStore) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var attempt = 0

    val incoming = MutableSharedFlow<Envelope>(extraBufferCapacity = 128)
    val connectionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private val _state = MutableStateFlow(ConnState.OFFLINE)
    val state: StateFlow<ConnState> = _state

    fun connect() {
        if (_state.value == ConnState.CONNECTING || _state.value == ConnState.ONLINE) return
        open()
    }

    private fun open() {
        _state.value = ConnState.CONNECTING
        val request = Request.Builder().url(store.serverUrl).build()
        socket = client.newWebSocket(request, listener)
    }

    fun send(type: String, id: String?, data: JSONObject = JSONObject()): Boolean =
        socket?.send(encode(type, id, data)) ?: false

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        val delayMs = (1000L shl attempt.coerceAtMost(5)).coerceAtMost(30_000L)
        attempt++
        reconnectJob = scope.launch {
            delay(delayMs)
            if (_state.value == ConnState.OFFLINE) open()
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            attempt = 0
            val data = JSONObject()
                .put("token", store.token)
                .put("deviceId", store.deviceId)
                .put("deviceName", android.os.Build.MODEL ?: "android")
            if (store.token.isBlank()) webSocket.send(encode(Msg.AUTH_REGISTER, "auth", data))
            else webSocket.send(encode(Msg.AUTH, "auth", data))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val env = runCatching { decode(text) }.getOrNull() ?: return
            if (env.type == Msg.AUTH_OK) {
                env.data.optString("token").takeIf { it.isNotBlank() }?.let { store.token = it }
                env.data.optString("deviceId").takeIf { it.isNotBlank() }?.let { store.deviceId = it }
                _state.value = ConnState.ONLINE
                connectionEvents.tryEmit(Unit)
            }
            if (env.type == Msg.AUTH_ERR) {
                store.token = ""
                _state.value = ConnState.OFFLINE
                webSocket.close(1000, "reauth")
                scheduleReconnect()
            }
            incoming.tryEmit(env)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _state.value = ConnState.OFFLINE
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _state.value = ConnState.OFFLINE
            scheduleReconnect()
        }
    }
}
