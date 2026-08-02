package com.openminis.app.ui.quant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class BinanceStrategyAlarmManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(strategy: BinanceStrategy) {
        if (!strategy.enabled) {
            cancel(strategy.id)
            return
        }
        val intent = Intent(context, BinanceStrategyAlarmReceiver::class.java).apply {
            action = BinanceStrategyScheduler.ACTION_FIRE
            putExtra(BinanceStrategyScheduler.EXTRA_STRATEGY_ID, strategy.id)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            strategy.id.hashCode() and 0x7FFFFFFF,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = System.currentTimeMillis() + strategy.intervalMinutes.coerceIn(5, 1440) * 60_000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(id: String) {
        val intent = Intent(context, BinanceStrategyAlarmReceiver::class.java).apply {
            action = BinanceStrategyScheduler.ACTION_FIRE
        }
        val pending = PendingIntent.getBroadcast(
            context,
            id.hashCode() and 0x7FFFFFFF,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pending)
        pending.cancel()
    }

    fun rescheduleAll() {
        BinanceStrategyStore.list(context).filter { it.enabled }.forEach(::schedule)
    }
}
