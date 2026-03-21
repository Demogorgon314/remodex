package com.emanueledipietro.remodex.data.connection

import kotlinx.coroutines.channels.Channel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

sealed interface RelayWireEvent {
    data object Opened : RelayWireEvent
    data class Message(val text: String) : RelayWireEvent
    data class Failure(val throwable: Throwable) : RelayWireEvent
    data class Closed(val code: Int, val reason: String) : RelayWireEvent
}

interface RelayWebSocket {
    fun send(text: String): Boolean
    fun close(code: Int, reason: String): Boolean
}

interface RelayWebSocketFactory {
    fun open(
        url: String,
        headers: Map<String, String>,
        events: Channel<RelayWireEvent>,
    ): RelayWebSocket
}

class OkHttpRelayWebSocketFactory(
    private val client: OkHttpClient,
) : RelayWebSocketFactory {
    override fun open(
        url: String,
        headers: Map<String, String>,
        events: Channel<RelayWireEvent>,
    ): RelayWebSocket {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        val socket = client.newWebSocket(
            requestBuilder.build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    events.trySend(RelayWireEvent.Opened)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    events.trySend(RelayWireEvent.Message(text))
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    events.trySend(RelayWireEvent.Closed(code, reason))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    events.trySend(RelayWireEvent.Closed(code, reason))
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    events.trySend(RelayWireEvent.Failure(t))
                }
            },
        )

        return object : RelayWebSocket {
            override fun send(text: String): Boolean = socket.send(text)

            override fun close(code: Int, reason: String): Boolean = socket.close(code, reason)
        }
    }
}
