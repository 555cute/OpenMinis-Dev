package com.openminis.app.ui.quant

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

/** Market/user stream controller owned by the quant dashboard lifecycle. */
class BinanceQuantStreamController(
    private val context: Context,
    private val client: BinanceApiClient = BinanceApiClient(),
) {
    private var socket: WebSocket? = null

    suspend fun startMarket(symbol: String, product: BinanceProduct, mode: TradingMode) = withContext(Dispatchers.IO) {
        stop()
        val stream = symbol.lowercase() + "@ticker"
        val host = when (product) {
            BinanceProduct.SPOT -> if (mode == TradingMode.DEMO) "wss://demo-stream.binance.com/ws/" else "wss://stream.binance.com/ws/"
            BinanceProduct.USD_M_FUTURES -> if (mode == TradingMode.DEMO) "wss://demo-fstream.binance.com/ws/" else "wss://fstream.binance.com/ws/"
        }
        socket = client.webSocket(host + stream, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val event = runCatching { JSONObject(text) }.getOrNull() ?: return
                if (event.optString("e") == "24hrTicker") BinanceQuantEvents.emit("market_tick")
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) = onMessage(webSocket, bytes.utf8())
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) { socket = null }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { socket = null }
        })
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        socket?.close(1000, "stop")
        socket = null
    }
}
