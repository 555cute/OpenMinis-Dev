package com.openminis.app.ui.quant

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.Locale
import kotlin.math.max

private val BinanceYellow = Color(0xFFF0B90B)
private val BinanceGreen = Color(0xFF0ECB81)
private val BinanceRed = Color(0xFFF6465D)
private val Ink = Color(0xFF181A20)
private val MutedInk = Color(0xFF707A8A)
private val LightPage = Color(0xFFF5F5F5)
private val LightCard = Color.White
private val DarkPage = Color(0xFF0B0E11)
private val DarkCard = Color(0xFF171A1F)

private enum class QuantTab(val label: String) {
    HOME("首页"), MARKET("行情"), TRADE("交易"), BOTS("机器人"), ASSETS("资产")
}

private enum class TradingMode(val label: String) {
    DEMO("模拟盘"), LIVE("实盘")
}

data class MarketTicker(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val quoteVolume: Double,
    val spark: List<Float>,
)

data class QuantBot(
    val id: String,
    val symbol: String,
    val strategy: String,
    val leverage: String,
    val pnl: Double,
    val roi: Double,
    val running: Boolean,
    val minInvestment: Double,
    val runtime: String,
)

private data class QuantColors(
    val page: Color,
    val card: Color,
    val elevated: Color,
    val text: Color,
    val muted: Color,
    val divider: Color,
)

private fun quantColors(): QuantColors {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.35f
    return if (dark) {
        QuantColors(DarkPage, DarkCard, Color(0xFF232830), Color.White, Color(0xFF9AA4B2), Color(0xFF2B3038))
    } else {
        QuantColors(LightPage, LightCard, Color(0xFFF0F1F3), Ink, MutedInk, Color(0xFFE7E9EC))
    }
}

private val fallbackTickers = listOf(
    MarketTicker("BTCUSDT", "Bitcoin", 63_555.65, 0.80, 1_280_000_000.0, listOf(0.26f, 0.30f, 0.27f, 0.42f, 0.38f, 0.57f, 0.62f, 0.72f)),
    MarketTicker("ETHUSDT", "Ethereum", 3_482.18, -1.25, 720_000_000.0, listOf(0.76f, 0.70f, 0.72f, 0.63f, 0.67f, 0.54f, 0.56f, 0.48f)),
    MarketTicker("BNBUSDT", "BNB", 584.26, 2.31, 215_000_000.0, listOf(0.30f, 0.34f, 0.32f, 0.46f, 0.49f, 0.58f, 0.55f, 0.68f)),
    MarketTicker("SOLUSDT", "Solana", 142.80, 4.68, 385_000_000.0, listOf(0.22f, 0.30f, 0.29f, 0.37f, 0.50f, 0.46f, 0.64f, 0.78f)),
    MarketTicker("XRPUSDT", "XRP", 0.5284, -0.42, 116_000_000.0, listOf(0.64f, 0.61f, 0.68f, 0.60f, 0.58f, 0.63f, 0.53f, 0.55f)),
    MarketTicker("DOGEUSDT", "Dogecoin", 0.1182, 3.12, 96_000_000.0, listOf(0.24f, 0.28f, 0.32f, 0.27f, 0.39f, 0.43f, 0.46f, 0.59f)),
)

private val initialBots = listOf(
    QuantBot("grid-btc", "BTCUSDT", "现货网格", "1x", 124.60, 4.28, true, 466.86, "3天 12时"),
    QuantBot("dca-eth", "ETHUSDT", "DCA 定投", "1x", 48.32, 2.16, true, 200.00, "8天 04时"),
    QuantBot("grid-sol", "SOLUSDT", "合约网格", "3x", -18.40, -1.12, false, 300.00, "1天 07时"),
)

/**
 * Fresh Binance Quant surface. This intentionally does not reuse the old
 * quant implementation: it is a self-contained UI shell with public market
 * data, local demo state, and clear seams for authenticated trading later.
 */
@Composable
fun BinanceQuantScreen(
    onSettingsClick: () -> Unit,
    onAgentClick: () -> Unit,
) {
    val colors = quantColors()
    var tabName by rememberSaveable { mutableStateOf(QuantTab.HOME.name) }
    var modeName by rememberSaveable { mutableStateOf(TradingMode.DEMO.name) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    var selectedSymbol by rememberSaveable { mutableStateOf("BTCUSDT") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var tickers by remember { mutableStateOf(fallbackTickers) }
    var loading by remember { mutableStateOf(false) }
    var marketError by remember { mutableStateOf<String?>(null) }
    var bots by remember { mutableStateOf(initialBots) }
    var showCreateBot by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val selectedTab = QuantTab.valueOf(tabName)
    val mode = TradingMode.valueOf(modeName)
    val selectedTicker = tickers.firstOrNull { it.symbol == selectedSymbol } ?: tickers.first()

    LaunchedEffect(refreshKey) {
        loading = true
        try {
            val fresh = BinanceMarketClient.load24hTickers()
            if (fresh.isNotEmpty()) tickers = fresh
            marketError = null
        } catch (_: Throwable) {
            // Public Binance endpoints can be unavailable on some networks;
            // keep the product usable with the clearly marked demo snapshot.
            marketError = "实时行情暂不可用，当前显示演示快照"
        } finally {
            loading = false
        }
    }

    if (showCreateBot) {
        CreateBotSheet(
            colors = colors,
            symbols = tickers.map { it.symbol },
            onDismiss = { showCreateBot = false },
            onCreate = { symbol, strategy, investment ->
                bots = listOf(
                    QuantBot(
                        id = "bot-${System.currentTimeMillis()}",
                        symbol = symbol,
                        strategy = strategy,
                        leverage = if (strategy == "合约网格") "3x" else "1x",
                        pnl = 0.0,
                        roi = 0.0,
                        running = true,
                        minInvestment = investment,
                        runtime = "刚刚启动",
                    ),
                ) + bots
                showCreateBot = false
                scope.launch { snackbarHostState.showSnackbar("策略已启动：$symbol $strategy") }
            },
        )
    }

    Scaffold(
        containerColor = colors.page,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            QuantBottomBar(
                colors = colors,
                selectedTab = selectedTab,
                onSelect = { tabName = it.name },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.page)
                .padding(innerPadding)
                .statusBarsPadding(),
        ) {
            QuantHeader(
                colors = colors,
                mode = mode,
                onModeChange = { modeName = it.name },
                onRefresh = { refreshKey++ },
                loading = loading,
                onSettingsClick = onSettingsClick,
                onAgentClick = onAgentClick,
            )

            when (selectedTab) {
                QuantTab.HOME -> HomePanel(
                    colors = colors,
                    tickers = tickers,
                    bots = bots,
                    mode = mode,
                    hidden = hidden,
                    marketError = marketError,
                    onHiddenChange = { hidden = !hidden },
                    onTabChange = { tabName = it.name },
                    onSymbolClick = {
                        selectedSymbol = it
                        tabName = QuantTab.TRADE.name
                    },
                    onBotClick = { tabName = QuantTab.BOTS.name },
                )

                QuantTab.MARKET -> MarketPanel(
                    colors = colors,
                    tickers = tickers,
                    marketError = marketError,
                    onSymbolClick = {
                        selectedSymbol = it
                        tabName = QuantTab.TRADE.name
                    },
                    onRefresh = { refreshKey++ },
                )

                QuantTab.TRADE -> TradePanel(
                    colors = colors,
                    ticker = selectedTicker,
                    mode = mode,
                    onPairClick = { tabName = QuantTab.MARKET.name },
                    onOrderSubmitted = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                )

                QuantTab.BOTS -> BotsPanel(
                    colors = colors,
                    bots = bots,
                    onCreateBot = { showCreateBot = true },
                    onToggleBot = { id ->
                        bots = bots.map { if (it.id == id) it.copy(running = !it.running) else it }
                    },
                    onDeleteBot = { id -> bots = bots.filterNot { it.id == id } },
                )

                QuantTab.ASSETS -> AssetsPanel(
                    colors = colors,
                    hidden = hidden,
                    onHiddenChange = { hidden = !hidden },
                    onTrade = { tabName = QuantTab.TRADE.name },
                )
            }
        }
    }
}

@Composable
private fun QuantHeader(
    colors: QuantColors,
    mode: TradingMode,
    onModeChange: (TradingMode) -> Unit,
    onRefresh: () -> Unit,
    loading: Boolean,
    onSettingsClick: () -> Unit,
    onAgentClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(BinanceYellow),
            contentAlignment = Alignment.Center,
        ) {
            Text("Q", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("BINANCE QUANT", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (mode == TradingMode.DEMO) BinanceYellow else BinanceGreen))
                Spacer(Modifier.width(5.dp))
                Text(mode.label, color = colors.muted, fontSize = 12.sp)
            }
        }
        FilterChip(
            selected = mode == TradingMode.LIVE,
            onClick = { onModeChange(if (mode == TradingMode.LIVE) TradingMode.DEMO else TradingMode.LIVE) },
            label = { Text(if (mode == TradingMode.LIVE) "LIVE" else "DEMO", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            leadingIcon = { Box(Modifier.size(6.dp).clip(CircleShape).background(if (mode == TradingMode.LIVE) BinanceGreen else BinanceYellow)) },
            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                containerColor = colors.card,
                labelColor = colors.muted,
                selectedContainerColor = BinanceYellow.copy(alpha = 0.18f),
                selectedLabelColor = colors.text,
            ),
            border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = mode == TradingMode.LIVE,
                borderColor = colors.divider,
                selectedBorderColor = BinanceYellow.copy(alpha = 0.65f),
            ),
        )
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = if (loading) BinanceYellow else colors.muted)
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "更多", tint = colors.text)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("AI 助手") },
                    leadingIcon = { Icon(Icons.Default.AutoGraph, null) },
                    onClick = { menuExpanded = false; onAgentClick() },
                )
                DropdownMenuItem(
                    text = { Text("应用设置") },
                    leadingIcon = { Icon(Icons.Default.Settings, null) },
                    onClick = { menuExpanded = false; onSettingsClick() },
                )
            }
        }
    }
}

@Composable
private fun QuantBottomBar(
    colors: QuantColors,
    selectedTab: QuantTab,
    onSelect: (QuantTab) -> Unit,
) {
    val items = listOf(
        QuantTab.HOME to Icons.Outlined.Home,
        QuantTab.MARKET to Icons.Outlined.CandlestickChart,
        QuantTab.TRADE to Icons.Outlined.SwapHoriz,
        QuantTab.BOTS to Icons.Outlined.SmartToy,
        QuantTab.ASSETS to Icons.Outlined.Wallet,
    )
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = colors.card,
        tonalElevation = 0.dp,
    ) {
        items.forEach { (tab, icon) ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
                },
                label = { Text(tab.label, fontSize = 11.sp) },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = Ink,
                    selectedTextColor = colors.text,
                    indicatorColor = BinanceYellow,
                    unselectedIconColor = colors.muted,
                    unselectedTextColor = colors.muted,
                ),
            )
        }
    }
}

@Composable
private fun HomePanel(
    colors: QuantColors,
    tickers: List<MarketTicker>,
    bots: List<QuantBot>,
    mode: TradingMode,
    hidden: Boolean,
    marketError: String?,
    onHiddenChange: () -> Unit,
    onTabChange: (QuantTab) -> Unit,
    onSymbolClick: (String) -> Unit,
    onBotClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("今日概览", color = colors.text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(if (mode == TradingMode.DEMO) "模拟资金 · 仅供策略验证" else "实盘账户 · 请先完成安全配置", color = colors.muted, fontSize = 13.sp)
            }
            AssistChip(
                onClick = { onTabChange(QuantTab.BOTS) },
                label = { Text("${bots.count { it.running }} 个运行中", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = BinanceGreen.copy(alpha = 0.12f),
                    labelColor = BinanceGreen,
                    leadingIconContentColor = BinanceGreen,
                ),
                border = null,
            )
        }

        BalanceHero(colors = colors, hidden = hidden, onHiddenChange = onHiddenChange, mode = mode)
        Spacer(Modifier.height(16.dp))
        QuickActions(colors = colors, onTabChange = onTabChange, onSymbolClick = onSymbolClick)
        Spacer(Modifier.height(20.dp))

        SectionTitle(colors, "策略运行中", "查看全部", onClick = onBotClick)
        Spacer(Modifier.height(10.dp))
        if (bots.none { it.running }) {
            EmptyStrategyCard(colors, onClick = onBotClick)
        } else {
            bots.filter { it.running }.take(2).forEach { bot ->
                RunningBotCard(colors, bot)
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        SectionTitle(colors, "市场雷达", "查看更多", onClick = { onTabChange(QuantTab.MARKET) })
        if (marketError != null) {
            Text(marketError, color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                tickers.take(5).forEachIndexed { index, ticker ->
                    MarketRow(colors = colors, ticker = ticker, compact = true, onClick = { onSymbolClick(ticker.symbol) })
                    if (index != minOf(tickers.size, 5) - 1) Divider(color = colors.divider)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun BalanceHero(colors: QuantColors, hidden: Boolean, onHiddenChange: () -> Unit, mode: TradingMode) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF20242B), Color(0xFF111318)))),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("预估总资产", color = Color(0xFFADB4C0), fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(if (mode == TradingMode.DEMO) "DEMO" else "LIVE", color = BinanceYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onHiddenChange, modifier = Modifier.size(28.dp)) {
                    Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color(0xFFADB4C0), modifier = Modifier.size(18.dp))
                }
            }
            Text(if (hidden) "••••••" else "12,480.32 USDT", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("今日盈亏", color = Color(0xFFADB4C0), fontSize = 13.sp)
                Spacer(Modifier.width(10.dp))
                Text(if (hidden) "••••" else "+246.88 USDT  (+2.02%)", color = BinanceGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            QuantAreaChart(color = BinanceYellow, modifier = Modifier.fillMaxWidth().height(54.dp))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("可用 8,923.44", color = Color(0xFFADB4C0), fontSize = 12.sp)
                Text("保证金率 100%", color = Color(0xFFADB4C0), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun QuickActions(colors: QuantColors, onTabChange: (QuantTab) -> Unit, onSymbolClick: (String) -> Unit) {
    val actions = listOf(
        Triple("买入 BTC", Icons.Default.ArrowDownward, BinanceGreen),
        Triple("卖出 BTC", Icons.Default.ArrowUpward, BinanceRed),
        Triple("新建策略", Icons.Default.Add, BinanceYellow),
        Triple("资产划转", Icons.Default.SwapHoriz, colors.text),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        actions.forEachIndexed { index, (label, icon, tint) ->
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clickable {
                        when (index) {
                            0, 1 -> { onSymbolClick("BTCUSDT") }
                            2 -> onTabChange(QuantTab.BOTS)
                            else -> onTabChange(QuantTab.ASSETS)
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(colors.card),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.height(7.dp))
                Text(label, color = colors.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SectionTitle(colors: QuantColors, title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text(action, color = colors.muted, fontSize = 12.sp)
            Icon(Icons.Default.ChevronRight, null, tint = colors.muted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun RunningBotCard(colors: QuantColors, bot: QuantBot) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinAvatar(bot.symbol, 34.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(bot.symbol, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${bot.strategy} · ${bot.leverage}", color = colors.muted, fontSize = 12.sp)
                }
                StatusPill("运行中", BinanceGreen)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(colors, "收益额", signedMoney(bot.pnl), bot.pnl >= 0)
                StatItem(colors, "收益率", signedPercent(bot.roi), bot.roi >= 0)
                StatItem(colors, "运行时间", bot.runtime, null)
            }
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { 0.68f },
                    modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape),
                    color = BinanceGreen,
                    trackColor = colors.elevated,
                )
                Spacer(Modifier.width(10.dp))
                Text("网格效率 68%", color = colors.muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EmptyStrategyCard(colors: QuantColors, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(BinanceYellow.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, null, tint = BinanceYellow)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("让策略替你盯盘", color = colors.text, fontWeight = FontWeight.SemiBold)
                Text("创建一个现货网格或 DCA 机器人", color = colors.muted, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = colors.muted)
        }
    }
}

@Composable
private fun MarketPanel(
    colors: QuantColors,
    tickers: List<MarketTicker>,
    marketError: String?,
    onSymbolClick: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("自选") }
    val filtered = tickers.filter { query.isBlank() || it.symbol.contains(query.uppercase(Locale.US)) || it.name.contains(query, ignoreCase = true) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchBox(colors, query, onQueryChange = { query = it }, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null, tint = colors.muted) }
        }
        if (marketError != null) Text(marketError, color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        val categories = listOf("自选", "现货", "合约", "涨幅榜")
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp)) {
            categories.forEach { item ->
                val selected = item == category
                Text(
                    item,
                    color = if (selected) colors.text else colors.muted,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .padding(end = 25.dp)
                        .clickable { category = item }
                        .padding(vertical = 8.dp),
                )
            }
        }
        Divider(color = colors.divider)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("名称 / 成交额", color = colors.muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                Text("最新价格", color = colors.muted, fontSize = 12.sp)
                Text("24h涨跌", color = colors.muted, fontSize = 12.sp)
            }
        }
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
            items(filtered, key = { it.symbol }) { ticker ->
                MarketRow(colors = colors, ticker = ticker, compact = false, onClick = { onSymbolClick(ticker.symbol) })
            }
        }
    }
}

@Composable
private fun SearchBox(colors: QuantColors, query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(colors.card)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, null, tint = colors.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 14.sp),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("搜索交易对和策略", color = colors.muted, fontSize = 14.sp)
                inner()
            },
        )
        if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = colors.muted, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
private fun MarketRow(colors: QuantColors, ticker: MarketTicker, compact: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 0.dp else 18.dp, vertical = if (compact) 11.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinAvatar(ticker.symbol, if (compact) 32.dp else 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ticker.symbol, color = colors.text, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold)
                if (!compact) {
                    Spacer(Modifier.width(5.dp))
                    Text("永续", color = colors.muted, fontSize = 10.sp, modifier = Modifier.border(1.dp, colors.divider, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            Text(if (compact) ticker.name else "${ticker.name}  ·  ${compactVolume(ticker.quoteVolume)} USDT", color = colors.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!compact) MiniSparkline(ticker.spark, if (ticker.change >= 0) BinanceGreen else BinanceRed, Modifier.width(52.dp).height(28.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(if (compact) 92.dp else 94.dp)) {
            Text(formatPrice(ticker.price), color = colors.text, fontSize = if (compact) 14.sp else 15.sp, fontWeight = FontWeight.SemiBold)
            Text("≈ ¥${formatPrice(ticker.price * 6.98)}", color = colors.muted, fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(if (compact) 68.dp else 76.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (ticker.change >= 0) BinanceGreen.copy(alpha = 0.14f) else BinanceRed.copy(alpha = 0.14f))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) { Text(signedPercent(ticker.change), color = if (ticker.change >= 0) BinanceGreen else BinanceRed, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun TradePanel(
    colors: QuantColors,
    ticker: MarketTicker,
    mode: TradingMode,
    onPairClick: () -> Unit,
    onOrderSubmitted: (String) -> Unit,
) {
    var side by rememberSaveable { mutableStateOf("买入") }
    var orderType by rememberSaveable { mutableStateOf("限价") }
    var price by rememberSaveable { mutableStateOf(formatPrice(ticker.price)) }
    var quantity by rememberSaveable { mutableStateOf("") }
    var ratio by remember { mutableFloatStateOf(0f) }
    val isBuy = side == "买入"
    val actionColor = if (isBuy) BinanceGreen else BinanceRed

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onPairClick)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ticker.symbol.chunked(3).joinToString("/"), color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = colors.muted, modifier = Modifier.size(20.dp))
                }
                Text("${ticker.name} · ${if (mode == TradingMode.DEMO) "模拟盘" else "实盘"}", color = colors.muted, fontSize = 12.sp)
            }
            IconButton(onClick = onPairClick) { Icon(Icons.Default.ShowChart, null, tint = colors.text) }
            IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, null, tint = colors.muted) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("最新价格", color = colors.muted, fontSize = 12.sp)
                        Text(formatPrice(ticker.price), color = if (ticker.change >= 0) BinanceGreen else BinanceRed, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text("≈ ¥${formatPrice(ticker.price * 6.98)}  ${signedPercent(ticker.change)}", color = if (ticker.change >= 0) BinanceGreen else BinanceRed, fontSize = 12.sp)
                    }
                    MiniSparkline(ticker.spark, if (ticker.change >= 0) BinanceGreen else BinanceRed, Modifier.width(120.dp).height(54.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(colors.card).padding(3.dp)) {
            listOf("买入", "卖出").forEach { item ->
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (side == item) actionColor else Color.Transparent).clickable { side = item }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(item, color = if (side == item) Color.White else colors.muted, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("限价", "市价", "止盈止损").forEach { item ->
                FilterChip(
                    selected = orderType == item,
                    onClick = { orderType = item },
                    label = { Text(item, fontSize = 12.sp) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        containerColor = colors.card,
                        selectedContainerColor = BinanceYellow.copy(alpha = 0.16f),
                        selectedLabelColor = colors.text,
                        labelColor = colors.muted,
                    ),
                    border = null,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (orderType != "市价") {
            OrderInput(colors, "价格 (USDT)", price, { price = it })
            Spacer(Modifier.height(10.dp))
        }
        OrderInput(colors, "数量 (${ticker.symbol.removeSuffix("USDT")})", quantity, { quantity = it })
        Spacer(Modifier.height(9.dp))
        Slider(value = ratio, onValueChange = { ratio = it }, valueRange = 0f..1f, steps = 3, colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = actionColor, activeTrackColor = actionColor))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0%", "25%", "50%", "75%", "100%").forEach { Text(it, color = colors.muted, fontSize = 10.sp) }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("可用", color = colors.muted, fontSize = 12.sp)
            Text(if (isBuy) "8,923.44 USDT" else "0.142 BTC", color = colors.text, fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                val label = if (orderType == "市价") "市价" else "${price.ifBlank { formatPrice(ticker.price) }} USDT"
                onOrderSubmitted("模拟订单已提交：$side ${ticker.symbol} · $label")
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(containerColor = actionColor),
        ) { Text("${side}${ticker.symbol.removeSuffix("USDT")}", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        if (mode == TradingMode.LIVE) {
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, null, tint = BinanceYellow, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("实盘下单前请在设置中完成 API 权限校验", color = colors.muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        OrderBook(colors, ticker)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun OrderInput(colors: QuantColors, label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '.' }) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(13.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BinanceYellow,
            unfocusedBorderColor = colors.divider,
            focusedLabelColor = BinanceYellow,
            unfocusedLabelColor = colors.muted,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
        ),
    )
}

@Composable
private fun OrderBook(colors: QuantColors, ticker: MarketTicker) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("盘口", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("深度", color = colors.muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            listOf(0.0012, 0.0008, 0.0005).forEachIndexed { index, qty ->
                OrderBookLine(colors, ticker.price + (3 - index) * ticker.price * 0.0002, qty, BinanceRed)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatPrice(ticker.price), color = BinanceYellow, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("≈ ¥${formatPrice(ticker.price * 6.98)}", color = colors.muted, fontSize = 11.sp)
            }
            listOf(0.0007, 0.0011, 0.0009).forEachIndexed { index, qty ->
                OrderBookLine(colors, ticker.price - (index + 1) * ticker.price * 0.0002, qty, BinanceGreen)
            }
        }
    }
}

@Composable
private fun OrderBookLine(colors: QuantColors, price: Double, quantity: Double, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatPrice(price), color = color, fontSize = 12.sp)
        Text(String.format(Locale.US, "%.4f", quantity), color = colors.text, fontSize = 12.sp)
    }
}

@Composable
private fun BotsPanel(
    colors: QuantColors,
    bots: List<QuantBot>,
    onCreateBot: () -> Unit,
    onToggleBot: (String) -> Unit,
    onDeleteBot: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("交易机器人", color = colors.text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("把规则变成可观察、可暂停的自动化策略", color = colors.muted, fontSize = 13.sp)
            }
            IconButton(onClick = {}) { Icon(Icons.Default.Tune, null, tint = colors.muted) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(BinanceYellow.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SmartToy, null, tint = BinanceYellow, modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("策略引擎", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${bots.count { it.running }} 个策略正在运行", color = colors.muted, fontSize = 12.sp)
                    }
                    StatusPill("在线", BinanceGreen)
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(colors, "策略总收益", signedMoney(bots.sumOf { it.pnl }), bots.sumOf { it.pnl } >= 0)
                    StatItem(colors, "胜率", "68.4%", null)
                    StatItem(colors, "最大回撤", "-6.20%", false)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionTitle(colors, "策略模板", "全部", onClick = onCreateBot)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("现货网格" to Icons.Default.GridView, "合约网格" to Icons.Default.Assessment, "DCA 定投" to Icons.Default.AutoGraph, "再平衡" to Icons.Default.Tune).forEach { (name, icon) ->
                StrategyTemplate(colors, name, icon, onCreateBot)
            }
        }
        Spacer(Modifier.height(24.dp))
        SectionTitle(colors, "我的机器人", "${bots.size} 个", onClick = {})
        Spacer(Modifier.height(9.dp))
        if (bots.isEmpty()) EmptyStrategyCard(colors, onCreateBot)
        bots.forEach { bot ->
            BotDetailCard(colors, bot, onToggle = { onToggleBot(bot.id) }, onDelete = { onDeleteBot(bot.id) })
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StrategyTemplate(colors: QuantColors, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(Modifier.width(88.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(70.dp).clip(RoundedCornerShape(18.dp)).background(colors.card), contentAlignment = Alignment.Center) { Icon(icon, null, tint = colors.text, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(7.dp))
        Text(title, color = colors.text, fontSize = 12.sp)
    }
}

@Composable
private fun BotDetailCard(colors: QuantColors, bot: QuantBot, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinAvatar(bot.symbol, 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(bot.symbol, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${bot.strategy} · ${bot.leverage}", color = colors.muted, fontSize = 12.sp)
                }
                StatusPill(if (bot.running) "运行中" else "已暂停", if (bot.running) BinanceGreen else colors.muted)
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) { Icon(if (bot.running) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = colors.text, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = colors.muted, modifier = Modifier.size(17.dp)) }
            }
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(colors, "收益额", signedMoney(bot.pnl), bot.pnl >= 0)
                StatItem(colors, "收益率", signedPercent(bot.roi), bot.roi >= 0)
                StatItem(colors, "最低投资", "${formatPrice(bot.minInvestment)} USDT", null)
            }
            Spacer(Modifier.height(11.dp))
            MiniSparkline(if (bot.pnl >= 0) listOf(0.28f, 0.30f, 0.26f, 0.42f, 0.50f, 0.48f, 0.70f, 0.76f) else listOf(0.70f, 0.63f, 0.67f, 0.53f, 0.58f, 0.42f, 0.40f, 0.32f), if (bot.pnl >= 0) BinanceGreen else BinanceRed, Modifier.fillMaxWidth().height(34.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBotSheet(
    colors: QuantColors,
    symbols: List<String>,
    onDismiss: () -> Unit,
    onCreate: (String, String, Double) -> Unit,
) {
    var symbol by remember { mutableStateOf(symbols.firstOrNull() ?: "BTCUSDT") }
    var strategy by remember { mutableStateOf("现货网格") }
    var investment by remember { mutableStateOf("500") }
    var symbolMenu by remember { mutableStateOf(false) }
    val strategies = listOf("现货网格", "合约网格", "DCA 定投", "再平衡")
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(), containerColor = colors.page) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("新建交易机器人", color = colors.text, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = colors.muted) }
            }
            Text("从小额模拟开始，确认参数后再切换实盘", color = colors.muted, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            Text("交易对", color = colors.muted, fontSize = 12.sp)
            Box {
                OutlinedButton(onClick = { symbolMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(symbol, color = colors.text, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    Icon(Icons.Default.KeyboardArrowDown, null)
                }
                DropdownMenu(expanded = symbolMenu, onDismissRequest = { symbolMenu = false }) {
                    symbols.take(6).forEach { item ->
                        DropdownMenuItem(text = { Text(item) }, onClick = { symbol = item; symbolMenu = false })
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("策略类型", color = colors.muted, fontSize = 12.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                strategies.forEach { item ->
                    FilterChip(selected = strategy == item, onClick = { strategy = item }, label = { Text(item, fontSize = 12.sp) }, border = null)
                }
            }
            Spacer(Modifier.height(14.dp))
            OrderInput(colors, "最低投资 (USDT)", investment) { investment = it }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = BinanceGreen, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("风控保护已开启：单日最大回撤 8%", color = colors.muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { onCreate(symbol, strategy, investment.toDoubleOrNull() ?: 500.0) },
                modifier = Modifier.fillMaxWidth().height(51.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BinanceYellow, contentColor = Ink),
            ) { Text("启动模拟策略", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AssetsPanel(colors: QuantColors, hidden: Boolean, onHiddenChange: () -> Unit, onTrade: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("资产", color = colors.text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("账户总览 · DEMO 账户", color = colors.muted, fontSize = 13.sp)
            }
            IconButton(onClick = onHiddenChange) { Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = colors.muted) }
            IconButton(onClick = {}) { Icon(Icons.Default.AccountBalanceWallet, null, tint = colors.text) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("预估总资产 (USDT)", color = colors.muted, fontSize = 13.sp)
                Text(if (hidden) "••••••" else "12,480.32", color = colors.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(if (hidden) "••••" else "+246.88 (+2.02%) 今日盈亏", color = BinanceGreen, fontSize = 13.sp)
                Spacer(Modifier.height(15.dp))
                QuantAreaChart(color = BinanceYellow, modifier = Modifier.fillMaxWidth().height(62.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AssetAction(colors, "充币", Icons.Default.ArrowDownward)
            AssetAction(colors, "提币", Icons.Default.ArrowUpward)
            AssetAction(colors, "划转", Icons.Default.SwapHoriz)
            AssetAction(colors, "交易", Icons.Default.ShowChart, onTrade)
        }
        Spacer(Modifier.height(21.dp))
        SectionTitle(colors, "币种分布", "资产配置", onClick = {})
        Spacer(Modifier.height(9.dp))
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.weight(0.62f).height(8.dp).clip(CircleShape).background(Color(0xFF2775CA)))
                    Box(Modifier.weight(0.20f).height(8.dp).clip(CircleShape).background(BinanceYellow))
                    Box(Modifier.weight(0.11f).height(8.dp).clip(CircleShape).background(Color(0xFF9945FF)))
                    Box(Modifier.weight(0.07f).height(8.dp).clip(CircleShape).background(colors.elevated))
                }
                Spacer(Modifier.height(12.dp))
                AllocationLine(colors, "USDT", "62.0%", Color(0xFF2775CA))
                AllocationLine(colors, "BTC", "20.0%", BinanceYellow)
                AllocationLine(colors, "ETH", "11.0%", Color(0xFF9945FF))
                AllocationLine(colors, "其他", "7.0%", colors.muted)
            }
        }
        Spacer(Modifier.height(21.dp))
        SectionTitle(colors, "数字货币", "管理", onClick = {})
        Spacer(Modifier.height(9.dp))
        listOf(
            Triple("USDT", "8,923.44", "8,923.44 USDT"),
            Triple("BTC", "0.14208", "9,029.66 USDT"),
            Triple("ETH", "0.8600", "2,980.11 USDT"),
        ).forEach { (coin, amount, value) ->
            BalanceRow(colors, coin, amount, value, onTrade)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun AssetAction(colors: QuantColors, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}) {
    Column(Modifier.width(72.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(colors.card), contentAlignment = Alignment.Center) { Icon(icon, null, tint = colors.text, modifier = Modifier.size(23.dp)) }
        Spacer(Modifier.height(7.dp))
        Text(label, color = colors.text, fontSize = 12.sp)
    }
}

@Composable
private fun AllocationLine(colors: QuantColors, coin: String, percent: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(7.dp))
        Text(coin, color = colors.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(percent, color = colors.muted, fontSize = 13.sp)
    }
}

@Composable
private fun BalanceRow(colors: QuantColors, coin: String, amount: String, value: String, onTrade: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CoinAvatar("${coin}USDT", 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(coin, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = colors.muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (coin == "USDT") amount else "$amount $coin", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onTrade, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), modifier = Modifier.height(25.dp)) { Text("交易", color = BinanceYellow, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Row(Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.13f)).padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatItem(colors: QuantColors, label: String, value: String, positive: Boolean?) {
    Column {
        Text(label, color = colors.muted, fontSize = 11.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = when (positive) { true -> BinanceGreen; false -> BinanceRed; null -> colors.text }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CoinAvatar(symbol: String, size: androidx.compose.ui.unit.Dp) {
    val base = symbol.removeSuffix("USDT").take(1).uppercase(Locale.US)
    val color = when (base) { "B" -> Color(0xFFF7931A); "E" -> Color(0xFF627EEA); "S" -> Color(0xFF9945FF); "X" -> Color(0xFF23292F); "D" -> Color(0xFFC3A634); else -> BinanceYellow }
    Box(Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Text(base, color = color, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniSparkline(values: List<Float>, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val minValue = values.minOrNull() ?: 0f
        val maxValue = values.maxOrNull() ?: 1f
        val range = max(maxValue - minValue, 0.001f)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / (values.size - 1).toFloat()
            val y = size.height - ((value - minValue) / range) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 2.3f, cap = StrokeCap.Round))
    }
}

@Composable
private fun QuantAreaChart(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val points = listOf(0.15f, 0.31f, 0.24f, 0.48f, 0.39f, 0.58f, 0.52f, 0.67f, 0.55f, 0.83f, 0.76f, 0.92f)
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = size.width * index / (points.size - 1).toFloat()
            val y = size.height - value * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 3f, cap = StrokeCap.Round))
    }
}

private object BinanceMarketClient {
    private val client = OkHttpClient()
    private const val endpoint = "https://api.binance.com/api/v3/ticker/24hr?symbols=%5B%22BTCUSDT%22%2C%22ETHUSDT%22%2C%22BNBUSDT%22%2C%22SOLUSDT%22%2C%22XRPUSDT%22%2C%22DOGEUSDT%22%5D"

    suspend fun load24hTickers(): List<MarketTicker> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(endpoint).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("empty response")
            val json = JSONArray(body)
            val names = mapOf("BTCUSDT" to "Bitcoin", "ETHUSDT" to "Ethereum", "BNBUSDT" to "BNB", "SOLUSDT" to "Solana", "XRPUSDT" to "XRP", "DOGEUSDT" to "Dogecoin")
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    val symbol = item.getString("symbol")
                    val price = item.getDouble("lastPrice")
                    val change = item.getDouble("priceChangePercent")
                    add(MarketTicker(symbol, names[symbol] ?: symbol, price, change, item.optDouble("quoteVolume", 0.0), makeSpark(symbol, change)))
                }
            }.sortedBy { it.symbol }
        }
    }

    private fun makeSpark(symbol: String, change: Double): List<Float> {
        val seed = symbol.fold(0) { acc, char -> acc + char.code }
        val result = ArrayList<Float>(8)
        repeat(8) { index ->
            val wave = ((seed + index * 17) % 31) / 100f
            result += (0.3f + wave + (if (change >= 0) index * 0.045f else -index * 0.035f)).coerceIn(0.05f, 0.95f)
        }
        return result
    }
}

private fun formatPrice(value: Double): String = when {
    value >= 1000 -> String.format(Locale.US, "%,.2f", value)
    value >= 1 -> String.format(Locale.US, "%,.2f", value)
    else -> String.format(Locale.US, "%.4f", value)
}

private fun signedPercent(value: Double): String = String.format(Locale.US, "%+.2f%%", value)
private fun signedMoney(value: Double): String = String.format(Locale.US, "%+.2f", value)
private fun compactVolume(value: Double): String = when {
    value >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", value / 1_000_000_000)
    value >= 1_000_000 -> String.format(Locale.US, "%.2fM", value / 1_000_000)
    else -> String.format(Locale.US, "%.0fK", value / 1_000)
}
