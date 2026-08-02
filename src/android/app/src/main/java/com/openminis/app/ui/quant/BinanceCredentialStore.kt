package com.openminis.app.ui.quant

import android.content.Context
import com.openminis.app.util.EncryptedPrefsFactory

object BinanceCredentialStore {
    private const val FILE = "binance_quant_credentials"
    private const val API_SUFFIX = "_api_key"
    private const val SECRET_SUFFIX = "_secret_key"

    private fun prefs(context: Context) =
        EncryptedPrefsFactory.safeCreate(context.applicationContext, FILE)

    private fun prefix(product: BinanceProduct, mode: TradingMode): String =
        "${product.name.lowercase()}_${mode.name.lowercase()}"

    fun load(
        context: Context,
        product: BinanceProduct,
        mode: TradingMode,
    ): BinanceCredentials? {
        val p = prefs(context)
        val base = prefix(product, mode)
        val apiKey = p.getString(base + API_SUFFIX, "").orEmpty()
        val secretKey = p.getString(base + SECRET_SUFFIX, "").orEmpty()
        return if (apiKey.isBlank() || secretKey.isBlank()) null
        else BinanceCredentials(apiKey.trim(), secretKey.trim())
    }

    fun save(
        context: Context,
        product: BinanceProduct,
        mode: TradingMode,
        credentials: BinanceCredentials,
    ) {
        val base = prefix(product, mode)
        prefs(context).edit()
            .putString(base + API_SUFFIX, credentials.apiKey.trim())
            .putString(base + SECRET_SUFFIX, credentials.secretKey.trim())
            .apply()
    }

    fun clear(context: Context, product: BinanceProduct, mode: TradingMode) {
        val base = prefix(product, mode)
        prefs(context).edit()
            .remove(base + API_SUFFIX)
            .remove(base + SECRET_SUFFIX)
            .apply()
    }
}
