package com.openminis.app.ui.quant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class BinanceStrategyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BinanceStrategyScheduler.ACTION_FIRE) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                BinanceStrategyScheduler(context.applicationContext).runOnce()
            } finally {
                pending.finish()
            }
        }
    }
}

class BinanceStrategyScheduler(private val context: Context) {
    private val client = BinanceApiClient()

    suspend fun runOnce() {
        BinanceStrategyStore.list(context).filter { it.enabled }.forEach { strategy ->
            runStrategy(strategy)
        }
    }

    private suspend fun runStrategy(strategy: BinanceStrategy) {
        val ticker = try {
            client.load24hTickers(strategy.product, strategy.mode, listOf(strategy.symbol)).firstOrNull()
        } catch (_: Throwable) {
            null
        }
        val price = ticker?.price
        val signal = evaluateSignal(strategy, price)
        val updated = BinanceStrategyStore.updateSignal(context, strategy.id, price, signal) ?: strategy
        if (signal != "WAIT" && signal != "DATA_UNAVAILABLE" && updated.lastNotifiedSignal == signal && updated.signalCount > strategy.signalCount) {
            BinanceSignalNotifier.notify(context, updated, "${strategy.symbol} 当前价格 ${price ?: "未知"}，策略信号：$signal。应用不会在后台静默下单，请打开量化 Agent 审批。")
            BinanceQuantEvents.emit("strategy_signal")
        }
    }

    private fun evaluateSignal(strategy: BinanceStrategy, price: Double?): String {
        if (price == null) return "DATA_UNAVAILABLE"
        return when (strategy.kind) {
            BinanceStrategyKind.GRID_SPOT, BinanceStrategyKind.GRID_FUTURES -> {
                val low = strategy.lowerPrice
                val high = strategy.upperPrice
                when {
                    low != null && price <= low -> "GRID_BUY_ZONE"
                    high != null && price >= high -> "GRID_SELL_ZONE"
                    else -> "WAIT"
                }
            }
            BinanceStrategyKind.DCA -> "DCA_DUE"
            BinanceStrategyKind.REBALANCE -> "REBALANCE_REVIEW"
        }
    }

    companion object {
        const val ACTION_FIRE = "com.openminis.quant.strategy.FIRE"
        const val EXTRA_STRATEGY_ID = "strategy_id"
    }
}
