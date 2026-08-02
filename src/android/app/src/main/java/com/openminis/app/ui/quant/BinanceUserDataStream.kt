package com.openminis.app.ui.quant

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

/**
 * Best-effort account/order event stream. REST remains the source of truth;
 * the socket only accelerates local status refresh and is recreated on close.
 */
class BinanceUserDataStream(
    private val context: Context,
    private val product: BinanceProduct,
    private val mode: TradingMode,
    private val credentials: BinanceCredentials,
    private val client: BinanceApiClient = BinanceApiClient(),
) {
    private var socket: WebSocket? = null
    private var listenKey: String? = null

    suspend fun start() = withContext(Dispatchers.IO) {
        stop()
        val key = client.createListenKey(product, mode, credentials)
        listenKey = key
        val url = client.userStreamUrl(product, mode, key)
        socket = client.webSocket(url, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleEvent(text)
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleEvent(bytes.utf8())
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                socket = null
            }
        })
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        socket?.close(1000, "stop")
        socket = null
        listenKey?.let { runCatching { client.closeListenKey(product, mode, credentials, it) } }
        listenKey = null
    }

    private fun handleEvent(text: String) {
        val event = runCatching { JSONObject(text) }.getOrNull() ?: return
        val type = event.optString("e")
        if (type == "executionReport") {
            val orderId = event.optString("i")
            val status = event.optString("X")
            BinanceOrderStore.updateStatus(
                context, orderId, status,
                executedQuantity = event.optString("z").toDoubleOrNull(),
                avgPrice = event.optString("L").toDoubleOrNull()?.takeIf { it > 0 },
            )
            val fillQty = event.optString("l").toDoubleOrNull() ?: 0.0
            if (fillQty > 0.0) {
                BinanceOrderStore.recordFill(
                    context,
                    BinanceTradeFill(
                        id = event.optString("t").ifBlank { "$orderId-${event.optLong("T")}" }, orderId = orderId,
                        symbol = event.optString("s"), price = event.optString("L").toDoubleOrNull() ?: 0.0,
                        quantity = fillQty, quoteQuantity = event.optString("Y").toDoubleOrNull() ?: 0.0,
                        commission = event.optString("n").toDoubleOrNull() ?: 0.0, commissionAsset = event.optString("N"),
                        time = event.optLong("T"), isBuyer = event.optBoolean("S"), isMaker = event.optBoolean("m"),
                    ),
                )
            }
            BinanceQuantEvents.emit("order_update")
        } else if (type == "ORDER_TRADE_UPDATE") {
            val order = event.optJSONObject("o") ?: return
            val orderId = order.optString("i")
            BinanceOrderStore.updateStatus(context, orderId, order.optString("X"), order.optString("z").toDoubleOrNull(), order.optString("ap").toDoubleOrNull(), order.optString("rp").toDoubleOrNull())
            BinanceQuantEvents.emit("order_update")
        }
    }
}
