package com.openminis.app.ui.quant

import android.content.Context
import com.openminis.app.util.EncryptedPrefsFactory
import org.json.JSONObject

data class BinanceRiskPolicy(
    val maxOrderUsdt: Double = 100.0,
    val maxDailyLossUsdt: Double = 50.0,
    val maxOpenOrders: Int = 10,
    val maxLeverage: Int = 3,
    val requireLiveSecondApproval: Boolean = true,
    val blockLiveByDefault: Boolean = false,
)

class BinanceRiskException(message: String) : IllegalStateException(message)

object BinanceRiskPolicyStore {
    private const val FILE = "binance_quant_risk_policy"
    private const val KEY = "risk_policy"

    private fun prefs(context: Context) =
        EncryptedPrefsFactory.safeCreate(context.applicationContext, FILE)

    fun load(context: Context): BinanceRiskPolicy {
        val json = runCatching { JSONObject(prefs(context).getString(KEY, "{}")) }
            .getOrElse { JSONObject() }
        return BinanceRiskPolicy(
            maxOrderUsdt = json.optDouble("maxOrderUsdt", 100.0).coerceAtLeast(0.0),
            maxDailyLossUsdt = json.optDouble("maxDailyLossUsdt", 50.0).coerceAtLeast(0.0),
            maxOpenOrders = json.optInt("maxOpenOrders", 10).coerceIn(0, 1000),
            maxLeverage = json.optInt("maxLeverage", 3).coerceIn(1, 125),
            requireLiveSecondApproval = json.optBoolean("requireLiveSecondApproval", true),
            blockLiveByDefault = json.optBoolean("blockLiveByDefault", false),
        )
    }

    fun save(context: Context, policy: BinanceRiskPolicy) {
        val json = JSONObject().apply {
            put("maxOrderUsdt", policy.maxOrderUsdt)
            put("maxDailyLossUsdt", policy.maxDailyLossUsdt)
            put("maxOpenOrders", policy.maxOpenOrders)
            put("maxLeverage", policy.maxLeverage)
            put("requireLiveSecondApproval", policy.requireLiveSecondApproval)
            put("blockLiveByDefault", policy.blockLiveByDefault)
        }
        prefs(context).edit().putString(KEY, json.toString()).apply()
    }
}

object BinanceRiskGuard {
    fun validateOrder(
        context: Context,
        product: BinanceProduct,
        mode: TradingMode,
        order: BinanceOrderRequest,
        referencePrice: Double?,
    ) {
        val policy = BinanceRiskPolicyStore.load(context)
        if (mode == TradingMode.LIVE && policy.blockLiveByDefault) {
            throw BinanceRiskException("LIVE trading is blocked by the local risk policy")
        }
        val quantity = order.quantity.toDoubleOrNull()
            ?: throw BinanceRiskException("quantity must be numeric")
        if (quantity <= 0.0) throw BinanceRiskException("quantity must be greater than zero")
        val price = order.price?.toDoubleOrNull() ?: referencePrice
        if (price != null && quantity * price > policy.maxOrderUsdt) {
            throw BinanceRiskException("order notional exceeds local max ${policy.maxOrderUsdt} USDT")
        }
        if (BinanceOrderStore.openOrderCount(context) >= policy.maxOpenOrders) {
            throw BinanceRiskException("open-order limit ${policy.maxOpenOrders} reached")
        }
        if (BinanceOrderStore.dailyRealizedPnl(context) <= -policy.maxDailyLossUsdt) {
            throw BinanceRiskException("daily loss limit ${policy.maxDailyLossUsdt} USDT reached")
        }
        if (product == BinanceProduct.USD_M_FUTURES && mode == TradingMode.LIVE && policy.maxLeverage < 1) {
            throw BinanceRiskException("invalid local futures leverage policy")
        }
    }
}
