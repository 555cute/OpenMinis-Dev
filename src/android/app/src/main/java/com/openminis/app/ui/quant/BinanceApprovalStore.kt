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

/** A write action that must be approved by the human in the Android UI. */
data class PendingBinanceOrder(
    val id: String,
    val product: BinanceProduct,
    val mode: TradingMode,
    val order: BinanceOrderRequest,
)

/**
 * App-level approval gate for all Binance writes.
 *
 * The agent tool suspends here; only the UI can resolve the request. This is
 * deliberately separate from model text such as `confirm=true`, so a model
 * cannot bypass the human approval step by fabricating an argument.
 */
object BinanceApprovalStore {
    private val _pending = MutableStateFlow<PendingBinanceOrder?>(null)
    val pending: StateFlow<PendingBinanceOrder?> = _pending.asStateFlow()

    private var waiter: CompletableDeferred<Boolean>? = null

    suspend fun awaitOrderApproval(
        product: BinanceProduct,
        mode: TradingMode,
        order: BinanceOrderRequest,
    ): Boolean {
        val request = PendingBinanceOrder(UUID.randomUUID().toString(), product, mode, order)
        val deferred = synchronized(this) {
            if (_pending.value != null || waiter != null) return false
            CompletableDeferred<Boolean>().also {
                waiter = it
                _pending.value = request
            }
        }
        return try {
            withTimeoutOrNull(120_000L) { deferred.await() } ?: false
        } finally {
            synchronized(this) {
                if (waiter === deferred) {
                    waiter = null
                    _pending.value = null
                }
            }
        }
    }

    fun approve(id: String) = resolve(id, true)
    fun reject(id: String) = resolve(id, false)

    private fun resolve(id: String, approved: Boolean) {
        synchronized(this) {
            if (_pending.value?.id != id) return
            waiter?.complete(approved)
        }
    }
}

/** Signals the quant dashboard to reload after agent-side state changes. */
object BinanceQuantEvents {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun emit(event: String) {
        _events.tryEmit(event)
    }
}
