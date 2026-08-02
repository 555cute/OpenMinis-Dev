package com.openminis.app.ui.quant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
        val timestamp = try {
            serverTime(product, mode)
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
        val signedParams = params + listOf(
            "recvWindow" to "5000",
            "timestamp" to timestamp.toString(),
        )
        val query = encodeQuery(signedParams)
        val signature = hmacSha256(credentials.secretKey, query)
        val request = Request.Builder()
            .url(baseUrl(product, mode) + path + "?" + query + "&signature=" + signature)
            .header("X-MBX-APIKEY", credentials.apiKey)
            .apply { if (method == "POST") post(ByteArray(0).toRequestBody()) }
            .build()
        return JSONObject(execute(request))
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
    private fun depthPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/depth" else "/fapi/v1/depth"
    private fun klinesPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/klines" else "/fapi/v1/klines"
    private fun accountPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/account" else "/fapi/v3/account"
    private fun orderPath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/order" else "/fapi/v1/order"
    private fun timePath(product: BinanceProduct) = if (product == BinanceProduct.SPOT) "/api/v3/time" else "/fapi/v1/time"

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
