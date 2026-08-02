package com.openminis.app.ui.quant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/** Binance product family supported by the quant client. */
enum class BinanceProduct(val label: String) {
    SPOT("现货"),
    USD_M_FUTURES("USDⓈ-M 合约"),
}

enum class TradingMode(val label: String) {
    DEMO("模拟盘"),
    LIVE("实盘"),
}

data class BinanceCredentials(
    val apiKey: String,
    val secretKey: String,
)

data class BinanceAssetBalance(
    val asset: String,
    val free: Double,
    val locked: Double,
    val valueUsdt: Double?,
)

data class BinanceAccountSnapshot(
    val totalEquityUsdt: Double?,
    val availableUsdt: Double?,
    val unrealizedPnlUsdt: Double?,
    val canTrade: Boolean,
    val assets: List<BinanceAssetBalance>,
)

data class BinanceOrderBookLevel(
    val price: Double,
    val quantity: Double,
)

data class BinanceOrderBook(
    val bids: List<BinanceOrderBookLevel>,
    val asks: List<BinanceOrderBookLevel>,
)

data class BinanceExchangeFilters(
    val minQty: Double? = null,
    val maxQty: Double? = null,
    val stepSize: Double? = null,
    val minNotional: Double? = null,
    val maxNotional: Double? = null,
    val tickSize: Double? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
)
data class BinanceOrderRequest(
    val symbol: String,
    val side: String,
    val type: String,
    val quantity: String,
    val price: String? = null,
)

data class BinanceOrderResult(
    val orderId: String,
    val status: String,
    val executedQuantity: Double,
    val avgPrice: Double?,
)

data class BinanceOpenOrder(
    val orderId: String,
    val symbol: String,
    val side: String,
    val type: String,
    val status: String,
    val originalQuantity: Double,
    val executedQuantity: Double,
    val price: Double,
)

data class BinancePosition(
    val symbol: String,
    val amount: Double,
    val entryPrice: Double,
    val markPrice: Double,
    val unrealizedPnl: Double,
    val leverage: Int?,
)

class BinanceApiException(
    val httpCode: Int,
    val binanceCode: Int?,
    override val message: String,
) : IOException(message)

/**
 * Small REST client for the official Binance Spot and USDⓈ-M Futures APIs.
 *
 * The client deliberately has no fallback values. A failed request is surfaced
 * to the UI, so demo numbers can never be mistaken for an account or order.
 */
class BinanceApiClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun webSocket(url: String, listener: WebSocketListener): WebSocket =
        http.newWebSocket(Request.Builder().url(url).build(), listener)

    suspend fun createListenKey(product: BinanceProduct, mode: TradingMode, credentials: BinanceCredentials): String {
        val path = if (product == BinanceProduct.SPOT) "/api/v3/userDataStream" else "/fapi/v1/listenKey"
        val request = Request.Builder().url(baseUrl(product, mode) + path).header("X-MBX-APIKEY", credentials.apiKey).post(ByteArray(0).toRequestBody()).build()
        return JSONObject(execute(request)).getString("listenKey")
    }

    suspend fun closeListenKey(product: BinanceProduct, mode: TradingMode, credentials: BinanceCredentials, listenKey: String) {
        val path = if (product == BinanceProduct.SPOT) "/api/v3/userDataStream" else "/fapi/v1/listenKey"
        val request = Request.Builder().url(baseUrl(product, mode) + path + "?listenKey=" + urlEncode(listenKey)).header("X-MBX-APIKEY", credentials.apiKey).delete().build()
        execute(request)
    }

    fun userStreamUrl(product: BinanceProduct, mode: TradingMode, listenKey: String): String = when (product) {
        BinanceProduct.SPOT -> if (mode == TradingMode.DEMO) "wss://demo-stream.binance.com/ws/$listenKey" else "wss://stream.binance.com/ws/$listenKey"
        BinanceProduct.USD_M_FUTURES -> if (mode == TradingMode.DEMO) "wss://demo-fstream.binance.com/ws/$listenKey" else "wss://fstream.binance.com/ws/$listenKey"
    }

    suspend fun load24hTickers(
        product: BinanceProduct,
        mode: TradingMode,
        symbols: List<String>,
    ): List<MarketTicker> = coroutineScope {
        symbols.map { symbol ->
            async(Dispatchers.IO) {
                val ticker = getJsonObject(
                    product,
                    mode,
                    tickerPath(product),
                    listOf("symbol" to symbol),
                )
                val spark = runCatching {
                    val klines = getJsonArray(
                        product,
                        mode,
                        klinesPath(product),
                        listOf("symbol" to symbol, "interval" to "1h", "limit" to "24"),
                    )
                    buildList {
                        for (index in 0 until klines.length()) {
                            val row = klines.getJSONArray(index)
                            add(row.getString(4).toDouble())
                        }
                    }
                }.getOrDefault(emptyList())
                MarketTicker(
                    symbol = symbol,
                    name = symbolNames[symbol] ?: symbol,
                    price = ticker.getString("lastPrice").toDouble(),
                    change = ticker.getString("priceChangePercent").toDouble(),
                    quoteVolume = ticker.optString("quoteVolume", "0").toDoubleOrNull() ?: 0.0,
                    spark = normalizeSpark(spark),
                )
            }
        }.awaitAll().sortedBy { it.symbol }
    }

    suspend fun loadExchangeFilters(
        product: BinanceProduct,
        mode: TradingMode,
        symbol: String,
    ): BinanceExchangeFilters {
        val json = getJsonObject(product, mode, exchangeInfoPath(product), listOf("symbol" to symbol))
        val symbols = json.optJSONArray("symbols") ?: JSONArray()
        val item = if (symbols.length() > 0) symbols.getJSONObject(0) else throw BinanceApiException(200, null, "Symbol not found: $symbol")
        val filters = item.optJSONArray("filters") ?: JSONArray()
        var result = BinanceExchangeFilters()
        for (index in 0 until filters.length()) {
            val filter = filters.getJSONObject(index)
            result = when (filter.optString("filterType")) {
                "LOT_SIZE", "MARKET_LOT_SIZE" -> result.copy(
                    minQty = filter.optString("minQty").toDoubleOrNull() ?: result.minQty,
                    maxQty = filter.optString("maxQty").toDoubleOrNull() ?: result.maxQty,
                    stepSize = filter.optString("stepSize").toDoubleOrNull() ?: result.stepSize,
                )
                "MIN_NOTIONAL", "NOTIONAL" -> result.copy(
                    minNotional = filter.optString("minNotional").toDoubleOrNull() ?: result.minNotional,
                    maxNotional = filter.optString("maxNotional").toDoubleOrNull() ?: result.maxNotional,
                )
                "PRICE_FILTER" -> result.copy(
                    tickSize = filter.optString("tickSize").toDoubleOrNull() ?: result.tickSize,
                    minPrice = filter.optString("minPrice").toDoubleOrNull() ?: result.minPrice,
                    maxPrice = filter.optString("maxPrice").toDoubleOrNull() ?: result.maxPrice,
                )
                else -> result
            }
        }
        return result
    }

    fun validateOrderFilters(order: BinanceOrderRequest, filters: BinanceExchangeFilters, referencePrice: Double? = null) {
        val quantity = order.quantity.toDoubleOrNull() ?: throw BinanceApiException(0, null, "Quantity must be numeric")
        val price = order.price?.toDoubleOrNull() ?: referencePrice
        filters.minQty?.let { if (quantity < it) throw BinanceApiException(0, null, "Quantity $quantity is below minQty $it") }
        filters.maxQty?.let { if (quantity > it) throw BinanceApiException(0, null, "Quantity $quantity exceeds maxQty $it") }
        filters.stepSize?.takeIf { it > 0 }?.let { step ->
            val steps = quantity / step
            if (kotlin.math.abs(steps - kotlin.math.round(steps)) > 1e-8) throw BinanceApiException(0, null, "Quantity does not match stepSize $step")
        }
        if (price != null) {
            filters.minPrice?.let { if (price < it) throw BinanceApiException(0, null, "Price $price is below minPrice $it") }
            filters.maxPrice?.let { if (price > it) throw BinanceApiException(0, null, "Price $price exceeds maxPrice $it") }
            filters.tickSize?.takeIf { it > 0 }?.let { tick ->
                val steps = price / tick
                if (kotlin.math.abs(steps - kotlin.math.round(steps)) > 1e-8) throw BinanceApiException(0, null, "Price does not match tickSize $tick")
            }
            val notional = quantity * price
            filters.minNotional?.let { if (notional < it) throw BinanceApiException(0, null, "Order notional $notional is below minNotional $it") }
            filters.maxNotional?.let { if (notional > it) throw BinanceApiException(0, null, "Order notional $notional exceeds maxNotional $it") }
        }
    }

    suspend fun loadOrderBook(
        product: BinanceProduct,
        mode: TradingMode,
        symbol: String,
        limit: Int = 10,
    ): BinanceOrderBook {
        val json = getJsonObject(
            product,
            mode,
            depthPath(product),
            listOf("symbol" to symbol, "limit" to limit.toString()),
        )
        fun parse(name: String): List<BinanceOrderBookLevel> {
            val array = json.optJSONArray(name) ?: return emptyList()
            return buildList {
                for (index in 0 until array.length()) {
                    val row = array.getJSONArray(index)
                    add(
                        BinanceOrderBookLevel(
                            price = row.getString(0).toDouble(),
                            quantity = row.getString(1).toDouble(),
                        ),
                    )
                }
            }
        }
        return BinanceOrderBook(bids = parse("bids"), asks = parse("asks"))
    }

    suspend fun loadAccount(
        product: BinanceProduct,
        mode: TradingMode,
        credentials: BinanceCredentials,
        marketTickers: List<MarketTicker>,
    ): BinanceAccountSnapshot {
        val json = signedJsonObject(
            product,
            mode,
            accountPath(product),
            credentials,
            emptyList(),
        )
        return when (product) {
            BinanceProduct.SPOT -> parseSpotAccount(json, marketTickers)
            BinanceProduct.USD_M_FUTURES -> parseFuturesAccount(json)
        }
    }

    suspend fun loadOpenOrders(
        product: BinanceProduct,
        mode: TradingMode,
        credentials: BinanceCredentials,
        symbol: String? = null,
    ): List<BinanceOpenOrder> {
        val params = symbol?.takeIf { it.isNotBlank() }?.let { listOf("symbol" to it) } ?: emptyList()
        val payload = signedJsonPayload(product, mode, openOrdersPath(product), credentials, params)
        val array = JSONArray(payload)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    BinanceOpenOrder(
                        orderId = item.optString("orderId"),
                        symbol = item.optString("symbol"),
                        side = item.optString("side"),
                        type = item.optString("type"),
                        status = item.optString("status"),
                        originalQuantity = item.optString("origQty", "0").toDoubleOrNull() ?: 0.0,
                        executedQuantity = item.optString("executedQty", "0").toDoubleOrNull() ?: 0.0,
                        price = item.optString("price", "0").toDoubleOrNull() ?: 0.0,
                    ),
                )
            }
        }
    }

    suspend fun loadPositions(
        product: BinanceProduct,
        mode: TradingMode,
        credentials: BinanceCredentials,
    ): List<BinancePosition> {
        if (product != BinanceProduct.USD_M_FUTURES) return emptyList()
        val payload = signedJsonPayload(product, mode, positionRiskPath(product), credentials, emptyList())
        val positions = JSONArray(payload)
        return buildList {
            for (index in 0 until positions.length()) {
                val item = positions.getJSONObject(index)
                val amount = item.optString("positionAmt", "0").toDoubleOrNull() ?: 0.0
                if (abs(amount) < 1e-12) continue
                add(
                    BinancePosition(
                        symbol = item.optString("symbol"),
                        amount = amount,
                        entryPrice = item.optString("entryPrice", "0").toDoubleOrNull() ?: 0.0,
                        markPrice = item.optString("markPrice", "0").toDoubleOrNull() ?: 0.0,
                        unrealizedPnl = item.optString("unRealizedProfit", "0").toDoubleOrNull() ?: 0.0,
                        leverage = item.optString("leverage", "").toIntOrNull(),
                    ),
                )
            }
        }
    }

    suspend fun cancelOrder(
        product: BinanceProduct,
        mode: TradingMode,
        credentials: BinanceCredentials,
        symbol: String,
        orderId: String,
    ): BinanceOrderResult {
        val json = JSONObject(
            signedJsonPayload(
                product,
                mode,
                cancelOrderPath(product),
                credentials,
                listOf("symbol" to symbol, "orderId" to orderId),
                method = "DELETE",
            ),
        )
        return BinanceOrderResult(
            orderId = json.optString("orderId", orderId),
            status = json.optString("status", "UNKNOWN"),
            executedQuantity = json.optString("executedQty", "0").toDoubleOrNull() ?: 0.0,
            avgPrice = json.optString("price", "").toDoubleOrNull()?.takeIf { it > 0 },
        )
    }

    suspend fun placeOrder(
        product: BinanceProduct,
        mode: TradingMode,
        credentials: BinanceCredentials,
        order: BinanceOrderRequest,
    ): BinanceOrderResult {
        val params = buildList {
            add("symbol" to order.symbol)
            add("side" to order.side)
            add("type" to order.type)
            add("quantity" to order.quantity)
            if (order.type == "LIMIT") {
                add("timeInForce" to "GTC")
                add("price" to (order.price ?: error("LIMIT order requires price")))
            }
            if (product == BinanceProduct.USD_M_FUTURES) {
                add("newOrderRespType" to "RESULT")
            }
        }
        val json = signedJsonObject(
            product,
            mode,
            orderPath(product),
            credentials,
            params,
            method = "POST",
        )
        val executed = json.optString("executedQty", "0").toDoubleOrNull() ?: 0.0
        val avgPrice = json.optString("avgPrice", "")
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 }
            ?: json.optString("price", "").toDoubleOrNull()?.takeIf { it > 0.0 }
        return BinanceOrderResult(
            orderId = json.optString("orderId", ""),
            status = json.optString("status", "UNKNOWN"),
            executedQuantity = executed,
            avgPrice = avgPrice,
        )
    }

    private suspend fun signedJsonObject(
        product: BinanceProduct,
        mode: TradingMode,
        path: String,
        credentials: BinanceCredentials,
        params: List<Pair<String, String>>,
        method: String = "GET",
    ): JSONObject {
        return JSONObject(signedJsonPayload(product, mode, path, credentials, params, method))
    }

    private suspend fun signedJsonPayload(
        product: BinanceProduct,
        mode: TradingMode,
        path: String,
        credentials: BinanceCredentials,
        params: List<Pair<String, String>>,
        method: String = "GET",
    ): String {
        val timestamp = try {
            serverTime(product, mode)
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
        val signedParams = params + listOf("recvWindow" to "5000", "timestamp" to timestamp.toString())
        val query = encodeQuery(signedParams)
        val signature = hmacSha256(credentials.secretKey, query)
        val request = Request.Builder()
            .url(baseUrl(product, mode) + path + "?" + query + "&signature=" + signature)
            .header("X-MBX-APIKEY", credentials.apiKey)
            .apply {
                when (method) {
                    "POST" -> post(ByteArray(0).toRequestBody())
                    "DELETE" -> delete()
                }
            }
            .build()
        return execute(request)
    }

    private suspend fun serverTime(product: BinanceProduct, mode: TradingMode): Long {
        val json = getJsonObject(product, mode, timePath(product), emptyList())
        return json.getLong("serverTime")
    }

    private suspend fun getJsonObject(
        product: BinanceProduct,
        mode: TradingMode,
        path: String,
        params: List<Pair<String, String>>,
    ): JSONObject {
        val query = encodeQuery(params)
        val url = baseUrl(product, mode) + path + if (query.isEmpty()) "" else "?$query"
        return JSONObject(execute(Request.Builder().url(url).get().build()))
    }

    private suspend fun getJsonArray(
        product: BinanceProduct,
        mode: TradingMode,
        path: String,
        params: List<Pair<String, String>>,
    ): JSONArray {
        val query = encodeQuery(params)
        val url = baseUrl(product, mode) + path + if (query.isEmpty()) "" else "?$query"
        return JSONArray(execute(Request.Builder().url(url).get().build()))
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val error = runCatching { JSONObject(body) }.getOrNull()
                val code = error?.optInt("code")
                val message = error?.optString("msg")?.takeIf { it.isNotBlank() }
                    ?: "Binance HTTP ${response.code}"
                throw BinanceApiException(response.code, code, message)
            }
            body
        }
    }

    private fun parseSpotAccount(json: JSONObject, tickers: List<MarketTicker>): BinanceAccountSnapshot {
        val prices = tickers.associateBy { it.symbol }
        val balances = json.optJSONArray("balances") ?: JSONArray()
        val assets = buildList {
            for (index in 0 until balances.length()) {
                val item = balances.getJSONObject(index)
                val asset = item.optString("asset")
                val free = item.optString("free", "0").toDoubleOrNull() ?: 0.0
                val locked = item.optString("locked", "0").toDoubleOrNull() ?: 0.0
                if (abs(free) < 1e-12 && abs(locked) < 1e-12) continue
                val price = when {
                    asset == "USDT" || asset == "USDC" || asset == "BUSD" -> 1.0
                    else -> prices[asset + "USDT"]?.price
                }
                add(BinanceAssetBalance(asset, free, locked, price?.times(free + locked)))
            }
        }.sortedByDescending { it.valueUsdt ?: 0.0 }
        val total = assets.mapNotNull { it.valueUsdt }.sum().takeIf { assets.any { a -> a.valueUsdt != null } }
        val available = assets.mapNotNull { asset ->
            val price = asset.valueUsdt?.div(asset.free + asset.locked)
            price?.times(asset.free)
        }.sum().takeIf { total != null }
        return BinanceAccountSnapshot(
            totalEquityUsdt = total,
            availableUsdt = available,
            unrealizedPnlUsdt = null,
            canTrade = json.optBoolean("canTrade", false),
            assets = assets,
        )
    }

    private fun parseFuturesAccount(json: JSONObject): BinanceAccountSnapshot {
        val assetsJson = json.optJSONArray("assets") ?: JSONArray()
        val assets = buildList {
            for (index in 0 until assetsJson.length()) {
                val item = assetsJson.getJSONObject(index)
                val asset = item.optString("asset")
                val wallet = item.optString("walletBalance", "0").toDoubleOrNull() ?: 0.0
                val unrealized = item.optString("unrealizedProfit", "0").toDoubleOrNull() ?: 0.0
                val available = item.optString("availableBalance", "0").toDoubleOrNull() ?: 0.0
                if (abs(wallet) < 1e-12 && abs(unrealized) < 1e-12) continue
                add(BinanceAssetBalance(asset, available, wallet - available, wallet + unrealized))
            }
        }.sortedByDescending { it.valueUsdt ?: 0.0 }
        return BinanceAccountSnapshot(
            totalEquityUsdt = json.optString("totalMarginBalance", "").toDoubleOrNull(),
            availableUsdt = json.optString("availableBalance", "").toDoubleOrNull(),
            unrealizedPnlUsdt = json.optString("totalUnrealizedProfit", "").toDoubleOrNull(),
            canTrade = json.optBoolean("canTrade", false),
            assets = assets,
        )
    }

    private fun normalizeSpark(values: List<Double>): List<Float> {
        if (values.size < 2) return emptyList()
        val min = values.minOrNull() ?: return emptyList()
        val max = values.maxOrNull() ?: return emptyList()
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        return values.map { ((it - min) / range).toFloat() }
    }

    private fun baseUrl(product: BinanceProduct, mode: TradingMode): String = when (product) {
        BinanceProduct.SPOT -> if (mode == TradingMode.DEMO) "https://demo-api.binance.com" else "https://api.binance.com"
        BinanceProduct.USD_M_FUTURES -> if (mode == TradingMode.DEMO) "https://demo-fapi.binance.com" else "https://fapi.binance.com"
    }

    private fun tickerPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/ticker/24hr" else "/fapi/v1/ticker/24hr"
    private fun exchangeInfoPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/exchangeInfo" else "/fapi/v1/exchangeInfo"
    private fun depthPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/depth" else "/fapi/v1/depth"
    private fun klinesPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/klines" else "/fapi/v1/klines"
    private fun accountPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/account" else "/fapi/v3/account"
    private fun orderPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/order" else "/fapi/v1/order"
    private fun timePath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/time" else "/fapi/v1/time"
    private fun openOrdersPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/openOrders" else "/fapi/v1/openOrders"
    private fun positionRiskPath(product: BinanceProduct) = "/fapi/v2/positionRisk"
    private fun cancelOrderPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/order" else "/fapi/v1/order"

    private fun encodeQuery(params: List<Pair<String, String>>): String = params.joinToString("&") {
        urlEncode(it.first) + "=" + urlEncode(it.second)
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    private fun hmacSha256(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val symbolNames = mapOf(
            "BTCUSDT" to "Bitcoin",
            "ETHUSDT" to "Ethereum",
            "BNBUSDT" to "BNB",
            "SOLUSDT" to "Solana",
            "XRPUSDT" to "XRP",
            "DOGEUSDT" to "Dogecoin",
        )
    }
}
