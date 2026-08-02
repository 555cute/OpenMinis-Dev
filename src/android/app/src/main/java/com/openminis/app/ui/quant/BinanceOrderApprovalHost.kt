package com.openminis.app.ui.quant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ApprovalYellow = Color(0xFFF0B90B)
private val ApprovalRed = Color(0xFFF6465D)
private val ApprovalInk = Color(0xFF181A20)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinanceOrderApprovalHost() {
    val pending by BinanceApprovalStore.pending.collectAsState()
    val request = pending ?: return
    ModalBottomSheet(
        onDismissRequest = { BinanceApprovalStore.reject(request.id) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = ApprovalRed)
                Spacer(Modifier.width(10.dp))
                Text("确认提交 Binance 订单", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { BinanceApprovalStore.reject(request.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
            Spacer(Modifier.size(10.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApprovalLine("产品", request.product.label)
                    ApprovalLine("环境", if (request.mode == TradingMode.LIVE) "LIVE 正式盘" else "DEMO 模拟盘")
                    ApprovalLine("交易对", request.order.symbol)
                    ApprovalLine("方向", request.order.side)
                    ApprovalLine("类型", request.order.type)
                    ApprovalLine("数量", request.order.quantity)
                    request.order.price?.let { ApprovalLine("价格", it) }
                }
            }
            Spacer(Modifier.size(10.dp))
            if (request.mode == TradingMode.LIVE) {
                Text("这是正式盘订单，可能导致真实资金变动。请确认交易对、方向、数量和价格。", color = ApprovalRed, fontSize = 12.sp, lineHeight = 18.sp)
            } else {
                Text("Demo 订单只会发送到 Binance Demo Trading 环境。", color = Color(0xFF707A8A), fontSize = 12.sp)
            }
            Spacer(Modifier.size(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { BinanceApprovalStore.reject(request.id) }, modifier = Modifier.weight(1f)) {
                    Text("拒绝")
                }
                Button(
                    onClick = { BinanceApprovalStore.approve(request.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (request.mode == TradingMode.LIVE) ApprovalRed else ApprovalYellow, contentColor = if (request.mode == TradingMode.LIVE) Color.White else ApprovalInk),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("确认发送", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.size(10.dp))
        }
    }
}

@Composable
private fun ApprovalLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF707A8A), fontSize = 13.sp)
        Text(value, color = Color(0xFF181A20), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
