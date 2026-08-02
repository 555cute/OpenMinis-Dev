package com.openminis.app.ui.quant

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinanceOrderHistorySheet(context: Context, onDismiss: () -> Unit) {
    val orders = BinanceOrderStore.orders(context)
    val fills = BinanceOrderStore.fills(context)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("订单与成交", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") }
            }
            Text("仅显示应用观察到的真实 Binance 返回和 WebSocket 事件", color = Color(0xFF707A8A), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (orders.isEmpty() && fills.isEmpty()) {
                    item { Text("暂无订单或成交记录", color = Color(0xFF707A8A), modifier = Modifier.padding(vertical = 30.dp)) }
                }
                items(orders) { order ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                        Column(Modifier.padding(13.dp)) {
                            Text("${order.symbol} · ${order.side.ifBlank { order.type }}", fontWeight = FontWeight.Bold)
                            Text("${order.status} · ${order.executedQuantity}/${order.quantity} · ${order.avgPrice ?: order.price ?: "市价"}", fontSize = 12.sp, color = Color(0xFF707A8A))
                            Text("${order.product.label} · ${order.mode.label} · ${order.orderId ?: order.id}", fontSize = 11.sp, color = Color(0xFF9AA4B2))
                        }
                    }
                }
                items(fills) { fill ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                        Column(Modifier.padding(13.dp)) {
                            Text("成交 ${fill.symbol}", fontWeight = FontWeight.Bold)
                            Text("price=${fill.price} · qty=${fill.quantity} · commission=${fill.commission} ${fill.commissionAsset}", fontSize = 12.sp, color = Color(0xFF707A8A))
                        }
                    }
                }
            }
        }
    }
}
