package com.openminis.app.ui.quant

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class PendingBinanceOrder(
    val id: String,
    val product: BinanceProduct,
    val mode: TradingMode,
    val order: BinanceOrderRequest,
    val action: BinanceWriteAction = BinanceWriteAction.PLACE_ORDER,
    val orderId: String? = null,
)

enum class BinanceWriteAction(val label: String) {
    PLACE_ORDER("提交订单"),
    CANCEL_ORDER("撤销订单"),
}

object BinanceApprovalStore {
    private val _pending = MutableStateFlow<PendingBinanceOrder?>(null)
    val pending: StateFlow<PendingBinanceOrder?> = _pending.asStateFlow()
    private var waiter: CompletableDeferred<Boolean>? = null

    suspend fun awaitOrderApproval(product: BinanceProduct, mode: TradingMode, order: BinanceOrderRequest): Boolean =
        awaitApproval(PendingBinanceOrder(UUID.randomUUID().toString(), product, mode, order))

    suspend fun awaitCancelApproval(product: BinanceProduct, mode: TradingMode, symbol: String, orderId: String): Boolean =
        awaitApproval(
            PendingBinanceOrder(
                id = UUID.randomUUID().toString(), product = product, mode = mode,
                order = BinanceOrderRequest(symbol, "", "CANCEL", "0"),
                action = BinanceWriteAction.CANCEL_ORDER, orderId = orderId,
            ),
        )

    private suspend fun awaitApproval(request: PendingBinanceOrder): Boolean {
        val deferred = synchronized(this) {
            if (_pending.value != null || waiter != null) return false
            CompletableDeferred<Boolean>().also { waiter = it; _pending.value = request }
        }
        return try { withTimeoutOrNull(120_000L) { deferred.await() } ?: false }
        finally {
            synchronized(this) { if (waiter === deferred) { waiter = null; _pending.value = null } }
        }
    }

    fun approve(id: String) = resolve(id, true)
    fun reject(id: String) = resolve(id, false)
    private fun resolve(id: String, approved: Boolean) {
        synchronized(this) { if (_pending.value?.id == id) waiter?.complete(approved) }
    }
}

data class BinanceMarketTick(
    val symbol: String,
    val price: Double,
    val changePercent: Double,
    val quoteVolume: Double,
    val eventTime: Long,
)

object BinanceQuantEvents {
    private val _marketTicks = MutableSharedFlow<BinanceMarketTick>(extraBufferCapacity = 128)
    val marketTicks: SharedFlow<BinanceMarketTick> = _marketTicks.asSharedFlow()
    fun emitMarketTick(tick: BinanceMarketTick) { _marketTicks.tryEmit(tick) }

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val events: SharedFlow<String> = _events.asSharedFlow()
    fun emit(event: String) { _events.tryEmit(event) }
}
