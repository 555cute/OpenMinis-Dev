package com.openminis.app.ui.quant

import android.content.Context
import com.openminis.app.util.EncryptedPrefsFactory
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Persisted strategy configuration. Execution is signal-only until approved. */
enum class BinanceStrategyKind(val label: String) {
    GRID_SPOT("现货网格"),
    GRID_FUTURES("合约网格"),
    DCA("DCA 定投"),
    REBALANCE("再平衡"),
}

data class BinanceStrategy(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val product: BinanceProduct,
    val mode: TradingMode,
    val symbol: String,
    val kind: BinanceStrategyKind,
    val investmentUsdt: Double,
    val lowerPrice: Double? = null,
    val upperPrice: Double? = null,
    val gridCount: Int? = null,
    val intervalMinutes: Int = 15,
    val enabled: Boolean = false,
    val signalOnly: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null,
    val lastPrice: Double? = null,
    val lastSignal: String? = null,
    val lastNotifiedSignal: String? = null,
    val signalCount: Int = 0,
    val totalRealizedPnlUsdt: Double = 0.0,
)

object BinanceStrategyStore {
    private const val FILE = "binance_quant_strategies"
    private const val KEY = "strategies_json"

    private fun prefs(context: Context) =
        EncryptedPrefsFactory.safeCreate(context.applicationContext, FILE)

    fun list(context: Context): List<BinanceStrategy> {
        val raw = prefs(context).getString(KEY, "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { runCatching { fromJson(it) }.getOrNull()?.let(::add) }
            }
        }.sortedByDescending { it.createdAt }
    }

    fun save(context: Context, strategy: BinanceStrategy) {
        val rows = list(context).filterNot { it.id == strategy.id } + strategy
        write(context, rows)
    }

    fun delete(context: Context, id: String) {
        write(context, list(context).filterNot { it.id == id })
    }

    fun setEnabled(context: Context, id: String, enabled: Boolean): BinanceStrategy? {
        val found = list(context).firstOrNull { it.id == id } ?: return null
        val updated = found.copy(enabled = enabled)
        save(context, updated)
        return updated
    }

    fun updateSignal(
        context: Context,
        id: String,
        price: Double?,
        signal: String,
    ): BinanceStrategy? {
        val found = list(context).firstOrNull { it.id == id } ?: return null
        val isNewSignal = signal != "WAIT" && signal != found.lastNotifiedSignal
        val updated = found.copy(
            lastRunAt = System.currentTimeMillis(),
            lastPrice = price,
            lastSignal = signal,
            lastNotifiedSignal = if (isNewSignal) signal else found.lastNotifiedSignal,
            signalCount = found.signalCount + if (isNewSignal) 1 else 0,
        )
        save(context, updated)
        return updated
    }

    fun applyExecution(
        context: Context,
        strategyId: String,
        side: String,
        quantity: Double,
        price: Double?,
        realizedPnlUsdt: Double?,
    ): BinanceStrategy? {
        val found = list(context).firstOrNull { it.id == strategyId } ?: return null
        val updated = found.copy(totalRealizedPnlUsdt = found.totalRealizedPnlUsdt + (realizedPnlUsdt ?: 0.0))
        save(context, updated)
        return updated
    }

    private fun write(context: Context, rows: List<BinanceStrategy>) {
        val array = JSONArray()
        rows.forEach { array.put(toJson(it)) }
        prefs(context).edit().putString(KEY, array.toString()).apply()
    }

    private fun toJson(strategy: BinanceStrategy) = JSONObject().apply {
        put("id", strategy.id)
        put("name", strategy.name)
        put("product", strategy.product.name)
        put("mode", strategy.mode.name)
        put("symbol", strategy.symbol)
        put("kind", strategy.kind.name)
        put("investmentUsdt", strategy.investmentUsdt)
        putNullable("lowerPrice", strategy.lowerPrice)
        putNullable("upperPrice", strategy.upperPrice)
        putNullable("gridCount", strategy.gridCount)
        put("intervalMinutes", strategy.intervalMinutes)
        put("enabled", strategy.enabled)
        put("signalOnly", strategy.signalOnly)
        put("createdAt", strategy.createdAt)
        putNullable("lastRunAt", strategy.lastRunAt)
        putNullable("lastPrice", strategy.lastPrice)
        putNullable("lastSignal", strategy.lastSignal)
        putNullable("lastNotifiedSignal", strategy.lastNotifiedSignal)
        put("signalCount", strategy.signalCount)
        put("totalRealizedPnlUsdt", strategy.totalRealizedPnlUsdt)
    }

    private fun fromJson(json: JSONObject) = BinanceStrategy(
        id = json.getString("id"),
        name = json.getString("name"),
        product = BinanceProduct.valueOf(json.getString("product")),
        mode = TradingMode.valueOf(json.getString("mode")),
        symbol = json.getString("symbol"),
        kind = BinanceStrategyKind.valueOf(json.getString("kind")),
        investmentUsdt = json.getDouble("investmentUsdt"),
        lowerPrice = json.optDoubleOrNull("lowerPrice"),
        upperPrice = json.optDoubleOrNull("upperPrice"),
        gridCount = json.optIntOrNull("gridCount"),
        intervalMinutes = json.optInt("intervalMinutes", 15).coerceIn(5, 1440),
        enabled = json.optBoolean("enabled", false),
        signalOnly = json.optBoolean("signalOnly", true),
        createdAt = json.optLong("createdAt", System.currentTimeMillis()),
        lastRunAt = json.optLongOrNull("lastRunAt"),
        lastPrice = json.optDoubleOrNull("lastPrice"),
        lastSignal = json.optString("lastSignal").takeIf { it.isNotBlank() },
        lastNotifiedSignal = json.optString("lastNotifiedSignal").takeIf { it.isNotBlank() },
        signalCount = json.optInt("signalCount", 0),
        totalRealizedPnlUsdt = json.optDouble("totalRealizedPnlUsdt", 0.0),
    )

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)
}
