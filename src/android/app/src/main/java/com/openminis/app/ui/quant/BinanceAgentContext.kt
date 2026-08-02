package com.openminis.app.ui.quant

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Context shared by the dashboard and the Binance Agent chat. */
data class BinanceAgentContextSnapshot(
    val product: BinanceProduct = BinanceProduct.SPOT,
    val mode: TradingMode = TradingMode.DEMO,
    val symbol: String = "BTCUSDT",
)

object BinanceAgentContext {
    private val _current = MutableStateFlow(BinanceAgentContextSnapshot())
    val current: StateFlow<BinanceAgentContextSnapshot> = _current.asStateFlow()

    fun update(product: BinanceProduct, mode: TradingMode, symbol: String) {
        _current.value = BinanceAgentContextSnapshot(product, mode, symbol.uppercase())
    }
}
