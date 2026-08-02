package com.openminis.app.ui.quant

import android.content.Context
import com.openminis.app.util.EncryptedPrefsFactory
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Immutable execution audit entry; no API credentials are ever stored here. */
data class BinanceOrderRecord(
    val id: String = UUID.randomUUID().toString(),
    val product: BinanceProduct,
    val mode: TradingMode,
    val symbol: String,
    val orderId: String? = null,
    val clientOrderId: String? = null,
    val side: String,
    val type: String,
    val status: String,
    val quantity: Double,
    val executedQuantity: Double = 0.0,
    val price: Double? = null,
    val avgPrice: Double? = null,
    val realizedPnlUsdt: Double? = null,
    val source: String = "ui",
    val strategyId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

data class BinanceTradeFill(
    val id: String,
    val orderId: String,
    val symbol: String,
    val price: Double,
    val quantity: Double,
    val quoteQuantity: Double,
    val commission: Double,
    val commissionAsset: String,
    val time: Long,
    val isBuyer: Boolean,
    val isMaker: Boolean,
)

object BinanceOrderStore {
    private const val FILE = "binance_quant_order_history"
    private const val ORDERS_KEY = "orders_json"
    private const val FILLS_KEY = "fills_json"
    private const val MAX_ROWS = 500

    private fun prefs(context: Context) =
        EncryptedPrefsFactory.safeCreate(context.applicationContext, FILE)

    fun orders(context: Context): List<BinanceOrderRecord> = readOrders(context)
        .sortedByDescending { it.updatedAt }

    fun record(context: Context, record: BinanceOrderRecord) {
        val rows = (readOrders(context).filterNot { it.id == record.id || (record.orderId != null && it.orderId == record.orderId) } + record)
            .takeLast(MAX_ROWS)
        writeOrders(context, rows)
        record.strategyId?.let { strategyId ->
            BinanceStrategyStore.applyExecution(
                context = context,
                strategyId = strategyId,
                side = record.side,
                quantity = record.executedQuantity,
                price = record.avgPrice ?: record.price,
                realizedPnlUsdt = record.realizedPnlUsdt,
            )
        }
    }

    fun findByOrderId(context: Context, orderId: String): BinanceOrderRecord? =
        readOrders(context).firstOrNull { it.orderId == orderId }

    fun ensureObservedOrders(context: Context, rows: List<BinanceOpenOrder>, product: BinanceProduct, mode: TradingMode) {
        rows.forEach { row ->
            record(context, BinanceOrderRecord(product = product, mode = mode, symbol = row.symbol, orderId = row.orderId, side = row.side, type = row.type, status = row.status, quantity = row.originalQuantity, executedQuantity = row.executedQuantity, price = row.price, source = "rest"))
        }
    }

    fun updateStatus(context: Context, orderId: String, status: String, executedQuantity: Double? = null, avgPrice: Double? = null, realizedPnlUsdt: Double? = null) {
        val existing = findByOrderId(context, orderId) ?: return
        record(context, existing.copy(status = status, executedQuantity = executedQuantity ?: existing.executedQuantity, avgPrice = avgPrice ?: existing.avgPrice, realizedPnlUsdt = realizedPnlUsdt ?: existing.realizedPnlUsdt, updatedAt = System.currentTimeMillis()))
    }

    fun recordFill(context: Context, fill: BinanceTradeFill) {
        val preferences = prefs(context)
        val rows = readFills(context).filterNot { it.id == fill.id } + fill
        val array = JSONArray()
        rows.takeLast(MAX_ROWS).forEach { array.put(fillToJson(it)) }
        preferences.edit().putString(FILLS_KEY, array.toString()).apply()
    }

    fun fills(context: Context): List<BinanceTradeFill> = readFills(context)
        .sortedByDescending { it.time }

    fun dailyRealizedPnl(context: Context, now: Long = System.currentTimeMillis()): Double {
        val start = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return readOrders(context)
            .filter { it.updatedAt in start..now }
            .sumOf { it.realizedPnlUsdt ?: 0.0 }
    }

    fun openOrderCount(context: Context): Int = readOrders(context)
        .count { it.status in setOf("NEW", "PARTIALLY_FILLED", "PENDING_NEW") }

    private fun readOrders(context: Context): List<BinanceOrderRecord> {
        val raw = prefs(context).getString(ORDERS_KEY, "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { json ->
                    runCatching { orderFromJson(json) }.getOrNull()?.let(::add)
                }
            }
        }
    }

    private fun readFills(context: Context): List<BinanceTradeFill> {
        val raw = prefs(context).getString(FILLS_KEY, "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { json ->
                    runCatching { fillFromJson(json) }.getOrNull()?.let(::add)
                }
            }
        }
    }

    private fun writeOrders(context: Context, rows: List<BinanceOrderRecord>) {
        val array = JSONArray()
        rows.forEach { array.put(orderToJson(it)) }
        prefs(context).edit().putString(ORDERS_KEY, array.toString()).apply()
    }

    private fun orderToJson(row: BinanceOrderRecord) = JSONObject().apply {
        put("id", row.id); put("product", row.product.name); put("mode", row.mode.name)
        put("symbol", row.symbol); putNullable("orderId", row.orderId); putNullable("clientOrderId", row.clientOrderId)
        put("side", row.side); put("type", row.type); put("status", row.status)
        put("quantity", row.quantity); put("executedQuantity", row.executedQuantity)
        putNullable("price", row.price); putNullable("avgPrice", row.avgPrice); putNullable("realizedPnlUsdt", row.realizedPnlUsdt)
        put("source", row.source); putNullable("strategyId", row.strategyId); put("updatedAt", row.updatedAt)
    }

    private fun orderFromJson(json: JSONObject) = BinanceOrderRecord(
        id = json.getString("id"), product = BinanceProduct.valueOf(json.getString("product")), mode = TradingMode.valueOf(json.getString("mode")),
        symbol = json.getString("symbol"), orderId = json.optString("orderId").takeIf { it.isNotBlank() }, clientOrderId = json.optString("clientOrderId").takeIf { it.isNotBlank() },
        side = json.getString("side"), type = json.getString("type"), status = json.getString("status"), quantity = json.optDouble("quantity"), executedQuantity = json.optDouble("executedQuantity"),
        price = json.optDoubleOrNull("price"), avgPrice = json.optDoubleOrNull("avgPrice"), realizedPnlUsdt = json.optDoubleOrNull("realizedPnlUsdt"),
        source = json.optString("source", "ui"), strategyId = json.optString("strategyId").takeIf { it.isNotBlank() }, updatedAt = json.optLong("updatedAt"),
    )

    private fun fillToJson(fill: BinanceTradeFill) = JSONObject().apply {
        put("id", fill.id); put("orderId", fill.orderId); put("symbol", fill.symbol); put("price", fill.price); put("quantity", fill.quantity); put("quoteQuantity", fill.quoteQuantity)
        put("commission", fill.commission); put("commissionAsset", fill.commissionAsset); put("time", fill.time); put("isBuyer", fill.isBuyer); put("isMaker", fill.isMaker)
    }

    private fun fillFromJson(json: JSONObject) = BinanceTradeFill(
        id = json.getString("id"), orderId = json.getString("orderId"), symbol = json.getString("symbol"), price = json.optDouble("price"), quantity = json.optDouble("quantity"), quoteQuantity = json.optDouble("quoteQuantity"),
        commission = json.optDouble("commission"), commissionAsset = json.optString("commissionAsset"), time = json.optLong("time"), isBuyer = json.optBoolean("isBuyer"), isMaker = json.optBoolean("isMaker"),
    )

    private fun JSONObject.putNullable(key: String, value: Any?) { put(key, value ?: JSONObject.NULL) }
    private fun JSONObject.optDoubleOrNull(key: String): Double? = if (!has(key) || isNull(key)) null else optDouble(key)
}
