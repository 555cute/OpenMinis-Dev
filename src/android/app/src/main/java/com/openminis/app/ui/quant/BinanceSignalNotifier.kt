package com.openminis.app.ui.quant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicInteger

object BinanceSignalNotifier {
    private const val CHANNEL_ID = "binance_quant_signals"
    private const val CHANNEL_NAME = "Binance Quant Signals"
    private val ids = AtomicInteger(17000)

    fun notify(context: Context, strategy: BinanceStrategy, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.openminis.app.R.drawable.ic_launcher_monochrome)
            .setContentTitle("Binance 策略信号 · ${strategy.name}")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(ids.incrementAndGet(), notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Signals from Binance Quant strategy monitors."
            },
        )
    }
}
