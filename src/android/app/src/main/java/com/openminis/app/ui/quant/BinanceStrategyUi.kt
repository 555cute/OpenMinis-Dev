package com.openminis.app.ui.quant

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StrategyCard(
    colors: QuantColors,
    strategy: BinanceStrategy,
    onToggle: (BinanceStrategy, Boolean) -> Unit,
    onDelete: (BinanceStrategy) -> Unit,
) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = colors.card),
        shape = RoundedCornerShape(17.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(strategy.name, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${strategy.symbol} · ${strategy.kind.label} · ${strategy.mode.label}", color = colors.muted, fontSize = 12.sp)
                }
                androidx.compose.material3.Switch(checked = strategy.enabled, onCheckedChange = { onToggle(strategy, it) })
                IconButton(onClick = { onDelete(strategy) }) {
                    Icon(Icons.Default.Delete, "删除策略", tint = colors.muted)
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("投入 ${formatPrice(strategy.investmentUsdt)} USDT", color = colors.muted, fontSize = 12.sp)
                Text(if (strategy.signalOnly) "信号模式" else "需审批", color = Color(0xFF0ECB81), fontSize = 12.sp)
            }
            strategy.lastSignal?.let {
                Spacer(Modifier.height(6.dp))
                Text("最近信号：$it · ${strategy.lastPrice?.let(::formatPrice) ?: "--"}", color = colors.text, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinanceStrategySheet(
    context: Context,
    product: BinanceProduct,
    mode: TradingMode,
    symbols: List<String>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var name by remember { mutableStateOf("我的量化策略") }
    var symbol by remember { mutableStateOf(symbols.firstOrNull() ?: "BTCUSDT") }
    var kindName by remember { mutableStateOf(BinanceStrategyKind.GRID_SPOT.name) }
    var investment by remember { mutableStateOf("100") }
    var lower by remember { mutableStateOf("") }
    var upper by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("15") }
    var expandedSymbol by remember { mutableStateOf(false) }
    var expandedKind by remember { mutableStateOf(false) }
    val kind = BinanceStrategyKind.valueOf(kindName)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("新建量化策略", fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") }
            }
            Text("${product.label} · ${mode.label} · 后台信号监控", color = Color(0xFF707A8A), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("策略名称") }, singleLine = true)
            Spacer(Modifier.height(9.dp))
            androidx.compose.foundation.layout.Box {
                OutlinedTextField(symbol, {}, Modifier.fillMaxWidth(), label = { Text("交易对") }, readOnly = true)
                androidx.compose.foundation.layout.Box(Modifier.matchParentSize().clickable { expandedSymbol = true })
                DropdownMenu(expanded = expandedSymbol, onDismissRequest = { expandedSymbol = false }) {
                    symbols.distinct().forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { symbol = item; expandedSymbol = false }) }
                }
            }
            Spacer(Modifier.height(9.dp))
            androidx.compose.foundation.layout.Box {
                OutlinedTextField(kind.label, {}, Modifier.fillMaxWidth(), label = { Text("策略类型") }, readOnly = true)
                androidx.compose.foundation.layout.Box(Modifier.matchParentSize().clickable { expandedKind = true })
                DropdownMenu(expanded = expandedKind, onDismissRequest = { expandedKind = false }) {
                    BinanceStrategyKind.values().filter { product == BinanceProduct.USD_M_FUTURES || it != BinanceStrategyKind.GRID_FUTURES }.forEach { item ->
                        DropdownMenuItem(text = { Text(item.label) }, onClick = { kindName = item.name; expandedKind = false })
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(investment, { investment = it.filter { ch -> ch.isDigit() || ch == '.' } }, Modifier.fillMaxWidth(), label = { Text("投入 USDT") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            if (kind == BinanceStrategyKind.GRID_SPOT || kind == BinanceStrategyKind.GRID_FUTURES) {
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(lower, { lower = it.filter { ch -> ch.isDigit() || ch == '.' } }, Modifier.weight(1f), label = { Text("下限") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(upper, { upper = it.filter { ch -> ch.isDigit() || ch == '.' } }, Modifier.weight(1f), label = { Text("上限") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
            }
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(interval, { interval = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("检查周期（分钟，最少 5）") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            Spacer(Modifier.height(10.dp))
            Text("后台只生成信号通知，不会绕过人工审批自动下单。", color = Color(0xFF707A8A), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val row = BinanceStrategy(
                        name = name.ifBlank { "我的量化策略" },
                        product = product,
                        mode = mode,
                        symbol = symbol,
                        kind = kind,
                        investmentUsdt = investment.toDoubleOrNull() ?: 0.0,
                        lowerPrice = lower.toDoubleOrNull(),
                        upperPrice = upper.toDoubleOrNull(),
                        intervalMinutes = (interval.toIntOrNull() ?: 15).coerceIn(5, 1440),
                        enabled = true,
                        signalOnly = true,
                    )
                    if (row.investmentUsdt <= 0.0) return@Button
                    BinanceStrategyStore.save(context, row)
                    BinanceStrategyAlarmManager(context).schedule(row)
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0B90B), contentColor = Color(0xFF181A20)),
                shape = RoundedCornerShape(12.dp),
            ) { Text("保存并开始监控", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
        }
    }
}
