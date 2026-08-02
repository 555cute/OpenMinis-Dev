package com.openminis.app.ui.quant

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max

private val BinanceYellow = Color(0xFFF0B90B)
private val BinanceGreen = Color(0xFF0ECB81)
private val BinanceRed = Color(0xFFF6465D)
private val Ink = Color(0xFF181A20)
private val MutedInk = Color(0xFF707A8A)
private val LightPage = Color(0xFFF5F5F5)
private val DarkPage = Color(0xFF0B0E11)
private val DarkCard = Color(0xFF171A1F)

private enum class QuantTab(val label: String) {
    HOME("首页"), MARKET("行情"), TRADE("交易"), BOTS("机器人"), ASSETS("资产")
}

data class MarketTicker(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val quoteVolume: Double,
    val spark: List<Float>,
)

private data class QuantColors(
    val page: Color,
    val card: Color,
    val elevated: Color,
    val text: Color,
    val muted: Color,
    val divider: Color,
)

@Composable
private fun quantColors(): QuantColors {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.35f
    return if (dark) {
        QuantColors(DarkPage, DarkCard, Color(0xFF232830), Color.White, Color(0xFF9AA4B2), Color(0xFF2B3038))
    } else {
        QuantColors(LightPage, Color.White, Color(0xFFF0F1F3), Ink, MutedInk, Color(0xFFE7E9EC))
    }
}

private val supportedSymbols = listOf("BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT", "DOGEUSDT")

/**
 * Binance Quant root surface backed by the official Binance REST APIs.
 * There is intentionally no demo/fake fallback: missing credentials and API
 * errors are rendered as disconnected/error states instead of balances.
 */
@Composable
fun BinanceQuantScreen(
    onSettingsClick: () -> Unit,
    onAgentClick: () -> Unit,
) {
    val context = LocalContext.current
    val colors = quantColors()
    val client = remember { BinanceApiClient() }
    var tabName by rememberSaveable { mutableStateOf(QuantTab.HOME.name) }
    var modeName by rememberSaveable { mutableStateOf(TradingMode.DEMO.name) }
    var productName by rememberSaveable { mutableStateOf(BinanceProduct.SPOT.name) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    var selectedSymbol by rememberSaveable { mutableStateOf("BTCUSDT") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var credentialVersion by remember { mutableIntStateOf(0) }
    var tickers by remember { mutableStateOf<List<MarketTicker>>(emptyList()) }
    var tickerLoading by remember { mutableStateOf(false) }
    var tickerError by remember { mutableStateOf<String?>(null) }
    var account by remember { mutableStateOf<BinanceAccountSnapshot?>(null) }
    var accountLoading by remember { mutableStateOf(false) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var showCredentialSheet by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val mode = TradingMode.valueOf(modeName)
    val product = BinanceProduct.valueOf(productName)
    val tab = QuantTab.valueOf(tabName)
    val credentials = remember(product, mode, credentialVersion) {
        BinanceCredentialStore.load(context, product, mode)
    }
    val selectedTicker = tickers.firstOrNull { it.symbol == selectedSymbol }
        ?: tickers.firstOrNull()

    LaunchedEffect(Unit) {
        BinanceQuantEvents.events.collectLatest {
            refreshKey++
        }
    }

    LaunchedEffect(refreshKey, product, mode) {
        tickerLoading = true
        tickerError = null
        try {
            tickers = client.load24hTickers(product, mode, supportedSymbols)
        } catch (error: Throwable) {
            tickers = emptyList()
            tickerError = apiErrorText(error)
        } finally {
            tickerLoading = false
        }
    }

    LaunchedEffect(refreshKey, product, mode, credentialVersion, tickers) {
        account = null
        accountError = null
        if (credentials == null || tickers.isEmpty()) return@LaunchedEffect
        accountLoading = true
        try {
            account = client.loadAccount(product, mode, credentials, tickers)
        } catch (error: Throwable) {
            accountError = apiErrorText(error)
        } finally {
            accountLoading = false
        }
    }

    if (showCredentialSheet) {
        BinanceCredentialSheet(
            context = context,
            client = client,
            product = product,
            mode = mode,
            existing = credentials,
            onDismiss = { showCredentialSheet = false },
            onSaved = {
                credentialVersion++
                refreshKey++
                showCredentialSheet = false
            },
        )
    }

    Scaffold(
        containerColor = colors.page,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            QuantBottomBar(colors, tab) { tabName = it.name }
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
                product = product,
                mode = mode,
                credentialsConfigured = credentials != null,
                tickerLoading = tickerLoading,
                onProductChange = {
                    productName = it.name
                    tickers = emptyList()
                    account = null
                },
                onModeChange = {
                    modeName = it.name
                    tickers = emptyList()
                    account = null
                },
                onRefresh = { refreshKey++ },
                onCredentialsClick = { showCredentialSheet = true },
                onSettingsClick = onSettingsClick,
                onAgentClick = onAgentClick,
            )

            when (tab) {
                QuantTab.HOME -> HomePanel(
                    colors = colors,
                    product = product,
                    mode = mode,
                    hidden = hidden,
                    account = account,
                    accountLoading = accountLoading,
                    accountError = accountError,
                    credentialsConfigured = credentials != null,
                    tickers = tickers,
                    tickerLoading = tickerLoading,
                    tickerError = tickerError,
                    onHiddenChange = { hidden = !hidden },
                    onTabChange = { tabName = it.name },
                    onSymbolClick = {
                        selectedSymbol = it
                        tabName = QuantTab.TRADE.name
                    },
                    onCredentialsClick = { showCredentialSheet = true },
                )

                QuantTab.MARKET -> MarketPanel(
                    colors = colors,
                    tickers = tickers,
                    loading = tickerLoading,
                    error = tickerError,
                    onSymbolClick = {
                        selectedSymbol = it
                        tabName = QuantTab.TRADE.name
                    },
                    onRefresh = { refreshKey++ },
                )

                QuantTab.TRADE -> TradePanel(
                    colors = colors,
                    client = client,
                    product = product,
                    mode = mode,
                    ticker = selectedTicker,
                    credentials = credentials,
                    account = account,
                    onPairClick = { tabName = QuantTab.MARKET.name },
                    onCredentialsClick = { showCredentialSheet = true },
                    onOrderSubmitted = { message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                            refreshKey++
                        }
                    },
                )

                QuantTab.BOTS -> BotsPanel(
                    colors = colors,
                    product = product,
                    mode = mode,
                    credentialsConfigured = credentials != null,
                    onCredentialsClick = { showCredentialSheet = true },
                    onTradeClick = { tabName = QuantTab.TRADE.name },
                )

                QuantTab.ASSETS -> AssetsPanel(
                    colors = colors,
                    product = product,
                    mode = mode,
                    account = account,
                    loading = accountLoading,
                    error = accountError,
                    hidden = hidden,
                    onHiddenChange = { hidden = !hidden },
                    onCredentialsClick = { showCredentialSheet = true },
                    onTrade = { tabName = QuantTab.TRADE.name },
                )
            }
        }
    }
}

@Composable
private fun QuantHeader(
    colors: QuantColors,
    product: BinanceProduct,
    mode: TradingMode,
    credentialsConfigured: Boolean,
    tickerLoading: Boolean,
    onProductChange: (BinanceProduct) -> Unit,
    onModeChange: (TradingMode) -> Unit,
    onRefresh: () -> Unit,
    onCredentialsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAgentClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(BinanceYellow),
                contentAlignment = Alignment.Center,
            ) { Text("Q", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("BINANCE QUANT", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (credentialsConfigured) BinanceGreen else BinanceYellow))
                    Spacer(Modifier.width(5.dp))
                    Text(if (credentialsConfigured) "API 已连接" else "未配置 API", color = colors.muted, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "刷新", tint = if (tickerLoading) BinanceYellow else colors.muted)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreHoriz, "更多", tint = colors.text)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("配置 Binance API") },
                        leadingIcon = { Icon(Icons.Default.Key, null) },
                        onClick = { menuExpanded = false; onCredentialsClick() },
                    )
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
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            BinanceProduct.values().forEach { item ->
                FilterChip(
                    selected = product == item,
                    onClick = { onProductChange(item) },
                    label = { Text(item.label, fontSize = 11.sp) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        containerColor = colors.card,
                        labelColor = colors.muted,
                        selectedContainerColor = BinanceYellow.copy(alpha = 0.18f),
                        selectedLabelColor = colors.text,
                    ),
                    border = null,
                )
            }
            Spacer(Modifier.weight(1f))
            FilterChip(
                selected = mode == TradingMode.LIVE,
                onClick = { onModeChange(if (mode == TradingMode.LIVE) TradingMode.DEMO else TradingMode.LIVE) },
                label = { Text(if (mode == TradingMode.LIVE) "LIVE" else "DEMO", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Box(Modifier.size(6.dp).clip(CircleShape).background(if (mode == TradingMode.LIVE) BinanceRed else BinanceYellow)) },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    containerColor = colors.card,
                    labelColor = colors.muted,
                    selectedContainerColor = BinanceRed.copy(alpha = 0.14f),
                    selectedLabelColor = BinanceRed,
                ),
                border = null,
            )
        }
    }
}

@Composable
private fun QuantBottomBar(colors: QuantColors, selected: QuantTab, onSelect: (QuantTab) -> Unit) {
    val items = listOf(
        QuantTab.HOME to Icons.Outlined.Home,
        QuantTab.MARKET to Icons.Outlined.Assessment,
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
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = { Icon(icon, tab.label, Modifier.size(22.dp)) },
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
    product: BinanceProduct,
    mode: TradingMode,
    hidden: Boolean,
    account: BinanceAccountSnapshot?,
    accountLoading: Boolean,
    accountError: String?,
    credentialsConfigured: Boolean,
    tickers: List<MarketTicker>,
    tickerLoading: Boolean,
    tickerError: String?,
    onHiddenChange: () -> Unit,
    onTabChange: (QuantTab) -> Unit,
    onSymbolClick: (String) -> Unit,
    onCredentialsClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("账户总览", color = colors.text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("${product.label} · ${mode.label} · 数据来自 Binance", color = colors.muted, fontSize = 12.sp)
            }
            AssistChip(
                onClick = onCredentialsClick,
                label = { Text(if (credentialsConfigured) "已连接" else "连接 API", fontSize = 12.sp) },
                leadingIcon = { Icon(if (credentialsConfigured) Icons.Default.CheckCircle else Icons.Default.Key, null, Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (credentialsConfigured) BinanceGreen.copy(alpha = 0.12f) else BinanceYellow.copy(alpha = 0.15f),
                    labelColor = if (credentialsConfigured) BinanceGreen else BinanceYellow,
                    leadingIconContentColor = if (credentialsConfigured) BinanceGreen else BinanceYellow,
                ),
                border = null,
            )
        }
        BalanceHero(colors, product, mode, account, accountLoading, accountError, hidden, onHiddenChange)
        Spacer(Modifier.height(16.dp))
        QuickActions(colors, onTabChange, onSymbolClick, onCredentialsClick)
        Spacer(Modifier.height(20.dp))
        SectionTitle(colors, "策略执行器", "查看说明", onClick = { onTabChange(QuantTab.BOTS) })
        Spacer(Modifier.height(9.dp))
        StrategyStatusCard(colors, product, mode, credentialsConfigured, onCredentialsClick, onTabChange)
        Spacer(Modifier.height(20.dp))
        SectionTitle(colors, "实时市场", "查看更多", onClick = { onTabChange(QuantTab.MARKET) })
        if (tickerError != null) ErrorText(colors, tickerError)
        if (tickerLoading && tickers.isEmpty()) LoadingCard(colors, "正在读取 Binance 行情…")
        else if (tickers.isEmpty()) EmptyDataCard(colors, "暂无行情数据", "请检查网络、地区访问或刷新")
        else {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    tickers.take(5).forEachIndexed { index, ticker ->
                        MarketRow(colors, ticker, true) { onSymbolClick(ticker.symbol) }
                        if (index != minOf(tickers.size, 5) - 1) Divider(color = colors.divider)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun BalanceHero(
    colors: QuantColors,
    product: BinanceProduct,
    mode: TradingMode,
    account: BinanceAccountSnapshot?,
    loading: Boolean,
    error: String?,
    hidden: Boolean,
    onHiddenChange: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF20242B), Color(0xFF111318)))),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("预估总资产 (USDT)", color = Color(0xFFADB4C0), fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text("${product.label} · ${mode.name}", color = BinanceYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onHiddenChange, modifier = Modifier.size(28.dp)) {
                    Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color(0xFFADB4C0), modifier = Modifier.size(18.dp))
                }
            }
            val total = account?.totalEquityUsdt
            Text(
                when {
                    hidden -> "••••••"
                    loading -> "读取中…"
                    total != null -> formatPrice(total)
                    else -> "未连接"
                },
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold,
            )
            val pnl = account?.unrealizedPnlUsdt
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("可用", color = Color(0xFFADB4C0), fontSize = 13.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    when {
                        hidden -> "••••"
                        account?.availableUsdt != null -> "${formatPrice(account.availableUsdt!!)} USDT"
                        else -> "--"
                    },
                    color = Color.White,
                    fontSize = 13.sp,
                )
                if (pnl != null) {
                    Spacer(Modifier.width(14.dp))
                    Text("未实现 ${signedMoney(pnl)}", color = if (pnl >= 0) BinanceGreen else BinanceRed, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (error != null) {
                Text("账户读取失败", color = BinanceRed, fontSize = 12.sp)
                Text(error, color = Color(0xFFADB4C0), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            } else if (account == null && !loading) {
                Text("配置对应的 Binance API Key 和 Secret 后读取账户", color = Color(0xFFADB4C0), fontSize = 12.sp)
            } else {
                QuantAreaChart(BinanceYellow, Modifier.fillMaxWidth().height(48.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("API 权限：${if (account?.canTrade == true) "允许交易" else if (account != null) "只读或不可交易" else "未验证"}", color = Color(0xFFADB4C0), fontSize = 11.sp)
        }
    }
}

@Composable
private fun QuickActions(
    colors: QuantColors,
    onTabChange: (QuantTab) -> Unit,
    onSymbolClick: (String) -> Unit,
    onCredentialsClick: () -> Unit,
) {
    val actions = listOf(
        Triple("买入 BTC", Icons.Default.ArrowDownward, BinanceGreen),
        Triple("卖出 BTC", Icons.Default.ArrowUpward, BinanceRed),
        Triple("盘口", Icons.Default.ShowChart, BinanceYellow),
        Triple("连接 API", Icons.Default.Key, colors.text),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        actions.forEachIndexed { index, (label, icon, tint) ->
            Column(
                Modifier.width(76.dp).clickable {
                    when (index) {
                        0, 1 -> onSymbolClick("BTCUSDT")
                        2 -> onTabChange(QuantTab.MARKET)
                        else -> onCredentialsClick()
                    }
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(colors.card), contentAlignment = Alignment.Center) {
                    Icon(icon, label, tint, Modifier.size(24.dp))
                }
                Spacer(Modifier.height(7.dp))
                Text(label, color = colors.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun StrategyStatusCard(
    colors: QuantColors,
    product: BinanceProduct,
    mode: TradingMode,
    credentialsConfigured: Boolean,
    onCredentialsClick: () -> Unit,
    onTabChange: (QuantTab) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(BinanceYellow.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SmartToy, null, tint = BinanceYellow)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("Binance 策略中心", color = colors.text, fontWeight = FontWeight.Bold)
                    Text("${product.label} · ${mode.label}", color = colors.muted, fontSize = 12.sp)
                }
                StatusPill(if (credentialsConfigured) "API 已配置" else "待连接", if (credentialsConfigured) BinanceGreen else BinanceYellow)
            }
            Spacer(Modifier.height(11.dp))
            Text(
                "当前页面只展示 Binance API 返回的真实账户和行情。策略机器人不会虚构收益或运行状态；接入策略前请先完成 API 权限配置。",
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = if (credentialsConfigured) ({ onTabChange(QuantTab.BOTS) }) else onCredentialsClick) {
                Text(if (credentialsConfigured) "查看策略接入说明" else "配置 API 凭据", color = BinanceYellow)
                Icon(Icons.Default.ChevronRight, null, tint = BinanceYellow, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MarketPanel(
    colors: QuantColors,
    tickers: List<MarketTicker>,
    loading: Boolean,
    error: String?,
    onSymbolClick: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("全部") }
    val filtered = tickers
        .filter { query.isBlank() || it.symbol.contains(query.uppercase(Locale.US)) || it.name.contains(query, true) }
        .let { list -> if (category == "涨幅榜") list.sortedByDescending { it.change } else list }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchBox(colors, query, { query = it }, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "刷新", tint = colors.muted) }
        }
        if (error != null) ErrorText(colors, error)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp)) {
            listOf("全部", "现货", "合约", "涨幅榜").forEach { item ->
                Text(
                    item,
                    color = if (category == item) colors.text else colors.muted,
                    fontSize = 15.sp,
                    fontWeight = if (category == item) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(end = 25.dp).clickable { category = item }.padding(vertical = 8.dp),
                )
            }
        }
        Divider(color = colors.divider)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("交易对 / 成交额", color = colors.muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                Text("最新价格", color = colors.muted, fontSize = 12.sp)
                Text("24h涨跌", color = colors.muted, fontSize = 12.sp)
            }
        }
        if (loading && tickers.isEmpty()) LoadingCard(colors, "正在读取 Binance 行情…")
        else if (filtered.isEmpty()) EmptyDataCard(colors, "没有可显示的行情", "实时接口没有返回数据")
        else {
            LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
                items(filtered, key = { it.symbol }) { ticker -> MarketRow(colors, ticker, false) { onSymbolClick(ticker.symbol) } }
            }
        }
    }
}

@Composable
private fun SearchBox(colors: QuantColors, query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.height(44.dp).clip(RoundedCornerShape(13.dp)).background(colors.card).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Search, null, tint = colors.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 14.sp),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("搜索交易对", color = colors.muted, fontSize = 14.sp)
                inner()
            },
        )
        if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = colors.muted, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
private fun MarketRow(colors: QuantColors, ticker: MarketTicker, compact: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = if (compact) 0.dp else 18.dp, vertical = if (compact) 11.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinAvatar(ticker.symbol, if (compact) 32.dp else 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ticker.symbol, color = colors.text, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold)
                if (!compact) {
                    Spacer(Modifier.width(5.dp))
                    Text("实时", color = BinanceGreen, fontSize = 10.sp, modifier = Modifier.border(1.dp, BinanceGreen.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            Text("${ticker.name} · ${compactVolume(ticker.quoteVolume)} USDT", color = colors.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!compact) MiniSparkline(ticker.spark, if (ticker.change >= 0) BinanceGreen else BinanceRed, Modifier.width(52.dp).height(28.dp))
        Column(Modifier.width(if (compact) 92.dp else 94.dp), horizontalAlignment = Alignment.End) {
            Text(formatPrice(ticker.price), color = colors.text, fontSize = if (compact) 14.sp else 15.sp, fontWeight = FontWeight.SemiBold)
            Text("≈ ¥${formatPrice(ticker.price * 6.98)}", color = colors.muted, fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.width(if (compact) 68.dp else 76.dp).clip(RoundedCornerShape(8.dp)).background(if (ticker.change >= 0) BinanceGreen.copy(alpha = 0.14f) else BinanceRed.copy(alpha = 0.14f)).padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) { Text(signedPercent(ticker.change), color = if (ticker.change >= 0) BinanceGreen else BinanceRed, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun TradePanel(
    colors: QuantColors,
    client: BinanceApiClient,
    product: BinanceProduct,
    mode: TradingMode,
    ticker: MarketTicker?,
    credentials: BinanceCredentials?,
    account: BinanceAccountSnapshot?,
    onPairClick: () -> Unit,
    onCredentialsClick: () -> Unit,
    onOrderSubmitted: (String) -> Unit,
) {
    var side by rememberSaveable { mutableStateOf("买入") }
    var orderType by rememberSaveable { mutableStateOf("限价") }
    var price by remember(ticker?.symbol) { mutableStateOf(ticker?.let { formatPrice(it.price) }.orEmpty()) }
    var quantity by rememberSaveable { mutableStateOf("") }
    var ratio by remember { mutableFloatStateOf(0f) }
    var orderBook by remember(ticker?.symbol, product, mode) { mutableStateOf<BinanceOrderBook?>(null) }
    var bookError by remember(ticker?.symbol, product, mode) { mutableStateOf<String?>(null) }
    var bookLoading by remember(ticker?.symbol, product, mode) { mutableStateOf(false) }
    var confirmOrder by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isBuy = side == "买入"
    val actionColor = if (isBuy) BinanceGreen else BinanceRed

    LaunchedEffect(ticker?.symbol, product, mode) {
        if (ticker == null) return@LaunchedEffect
        bookLoading = true
        bookError = null
        try {
            orderBook = client.loadOrderBook(product, mode, ticker.symbol)
        } catch (error: Throwable) {
            orderBook = null
            bookError = apiErrorText(error)
        } finally {
            bookLoading = false
        }
    }

    if (confirmOrder && ticker != null) {
        AlertDialog(
            onDismissRequest = { if (!submitting) confirmOrder = false },
            title = { Text(if (mode == TradingMode.LIVE) "确认实盘下单" else "确认 Demo 下单") },
            text = {
                Text("${side}${ticker.symbol.removeSuffix("USDT")} · ${if (orderType == "市价") "市价" else "限价 ${price} USDT"}\n数量：$quantity\n\n请求将发送到 ${if (mode == TradingMode.LIVE) "Binance 正式环境" else "Binance Demo 环境"}。")
            },
            confirmButton = {
                Button(
                    enabled = !submitting,
                    onClick = {
                        confirmOrder = false
                        submitting = true
                        scope.launch {
                            try {
                                if (credentials == null) error("请先配置 API Key 和 Secret")
                                if (quantity.toDoubleOrNull()?.let { it > 0 } != true) error("请输入有效数量")
                                val result = client.placeOrder(
                                    product,
                                    mode,
                                    credentials,
                                    BinanceOrderRequest(
                                        symbol = ticker.symbol,
                                        side = if (isBuy) "BUY" else "SELL",
                                        type = if (orderType == "市价") "MARKET" else "LIMIT",
                                        quantity = quantity,
                                        price = price.takeIf { orderType != "市价" },
                                    ),
                                )
                                onOrderSubmitted("订单已提交：${result.orderId} · ${result.status}")
                            } catch (error: Throwable) {
                                onOrderSubmitted("下单失败：${apiErrorText(error)}")
                            } finally {
                                submitting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (mode == TradingMode.LIVE) BinanceRed else BinanceYellow, contentColor = if (mode == TradingMode.LIVE) Color.White else Ink),
                ) { Text(if (mode == TradingMode.LIVE) "确认实盘" else "确认下单") }
            },
            dismissButton = { TextButton(onClick = { confirmOrder = false }) { Text("取消") } },
        )
    }

    if (ticker == null) {
        EmptyDataCard(colors, "暂无交易对数据", "先从行情接口读取实时交易对")
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onPairClick)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ticker.symbol.removeSuffix("USDT") + "/USDT", color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = colors.muted, modifier = Modifier.size(20.dp))
                }
                Text("${ticker.name} · ${product.label} · ${mode.label}", color = colors.muted, fontSize = 12.sp)
            }
            IconButton(onClick = onPairClick) { Icon(Icons.Default.ShowChart, "选择交易对", tint = colors.text) }
            IconButton(onClick = onCredentialsClick) { Icon(Icons.Default.Key, "API 设置", tint = if (credentials == null) BinanceYellow else colors.muted) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("最新价格", color = colors.muted, fontSize = 12.sp)
                    Text(formatPrice(ticker.price), color = if (ticker.change >= 0) BinanceGreen else BinanceRed, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("≈ ¥${formatPrice(ticker.price * 6.98)}  ${signedPercent(ticker.change)}", color = if (ticker.change >= 0) BinanceGreen else BinanceRed, fontSize = 12.sp)
                }
                MiniSparkline(ticker.spark, if (ticker.change >= 0) BinanceGreen else BinanceRed, Modifier.width(120.dp).height(54.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(colors.card).padding(3.dp)) {
            listOf("买入", "卖出").forEach { item ->
                Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (side == item) actionColor else Color.Transparent).clickable { side = item }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(item, color = if (side == item) Color.White else colors.muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("限价", "市价").forEach { item ->
                FilterChip(selected = orderType == item, onClick = { orderType = item }, label = { Text(item, fontSize = 12.sp) }, border = null)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (orderType != "市价") {
            OrderInput(colors, "价格 (USDT)", price) { price = it }
            Spacer(Modifier.height(10.dp))
        }
        OrderInput(colors, "数量 (${ticker.symbol.removeSuffix("USDT")})", quantity) { quantity = it }
        Spacer(Modifier.height(9.dp))
        Slider(value = ratio, onValueChange = { ratio = it }, valueRange = 0f..1f, steps = 3, colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = actionColor, activeTrackColor = actionColor))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("0%", "25%", "50%", "75%", "100%").forEach { Text(it, color = colors.muted, fontSize = 10.sp) } }
        Spacer(Modifier.height(10.dp))
        Text("可用：${availableText(account, ticker.symbol, isBuy)}", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Button(
            enabled = !submitting,
            onClick = { if (credentials == null) onCredentialsClick() else confirmOrder = true },
            Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(containerColor = actionColor),
        ) { Text(if (credentials == null) "先配置 API" else "${side}${ticker.symbol.removeSuffix("USDT")}", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        if (mode == TradingMode.LIVE) {
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, null, tint = BinanceRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("LIVE 会向 Binance 正式账户发送真实订单", color = BinanceRed, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        SectionTitle(colors, "实时盘口", "刷新", onClick = {
            scope.launch {
                bookLoading = true
                try { orderBook = client.loadOrderBook(product, mode, ticker.symbol); bookError = null }
                catch (error: Throwable) { bookError = apiErrorText(error) }
                finally { bookLoading = false }
            }
        })
        if (bookError != null) ErrorText(colors, bookError!!)
        if (bookLoading) LoadingCard(colors, "正在读取实时盘口…")
        else if (orderBook != null) OrderBook(colors, ticker, orderBook!!)
        else EmptyDataCard(colors, "暂无盘口数据", "接口没有返回深度数据")
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
private fun OrderBook(colors: QuantColors, ticker: MarketTicker, book: BinanceOrderBook) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("盘口", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${book.bids.size + book.asks.size} 档 · 实时", color = colors.muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(9.dp))
            book.asks.take(5).reversed().forEach { level -> OrderBookLine(colors, level, BinanceRed) }
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatPrice(ticker.price), color = BinanceYellow, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("≈ ¥${formatPrice(ticker.price * 6.98)}", color = colors.muted, fontSize = 11.sp)
            }
            book.bids.take(5).forEach { level -> OrderBookLine(colors, level, BinanceGreen) }
        }
    }
}

@Composable
private fun OrderBookLine(colors: QuantColors, level: BinanceOrderBookLevel, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatPrice(level.price), color = color, fontSize = 12.sp)
        Text(String.format(Locale.US, "%.6f", level.quantity), color = colors.text, fontSize = 12.sp)
    }
}

@Composable
private fun BotsPanel(
    colors: QuantColors,
    product: BinanceProduct,
    mode: TradingMode,
    credentialsConfigured: Boolean,
    onCredentialsClick: () -> Unit,
    onTradeClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("交易机器人", color = colors.text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("真实账户策略状态不会用假数据填充", color = colors.muted, fontSize = 13.sp)
            }
            StatusPill(if (credentialsConfigured) "已连接" else "未连接", if (credentialsConfigured) BinanceGreen else BinanceYellow)
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(BinanceYellow.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SmartToy, null, tint = BinanceYellow, modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("策略中心", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${product.label} · ${mode.label}", color = colors.muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(15.dp))
                Text("当前没有本地虚构机器人。Binance 原生策略产品的订单、收益和生命周期需要对应的策略 API；本版本先提供真实行情、账户、盘口和签名下单。", color = colors.muted, fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick = onCredentialsClick, shape = RoundedCornerShape(10.dp)) { Text(if (credentialsConfigured) "更换 API" else "配置 API") }
                    Button(onClick = onTradeClick, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = BinanceYellow, contentColor = Ink)) { Text("进入交易") }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        SectionTitle(colors, "策略模板", "仅参数参考", onClick = {})
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("现货网格" to Icons.Default.GridView, "合约网格" to Icons.Default.Assessment, "DCA 定投" to Icons.Default.AutoGraph, "再平衡" to Icons.Default.Tune).forEach { (title, icon) ->
                Column(Modifier.width(88.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(70.dp).clip(RoundedCornerShape(18.dp)).background(colors.card), contentAlignment = Alignment.Center) { Icon(icon, title, tint = colors.text, modifier = Modifier.size(28.dp)) }
                    Spacer(Modifier.height(7.dp))
                    Text(title, color = colors.text, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun AssetsPanel(
    colors: QuantColors,
    product: BinanceProduct,
    mode: TradingMode,
    account: BinanceAccountSnapshot?,
    loading: Boolean,
    error: String?,
    hidden: Boolean,
    onHiddenChange: () -> Unit,
    onCredentialsClick: () -> Unit,
    onTrade: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("资产", color = colors.text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("${product.label} · ${mode.label} · Binance API", color = colors.muted, fontSize = 12.sp)
            }
            IconButton(onClick = onHiddenChange) { Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, "隐藏资产", tint = colors.muted) }
            IconButton(onClick = onCredentialsClick) { Icon(Icons.Default.Key, "API 设置", tint = colors.text) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("账户权益 (USDT)", color = colors.muted, fontSize = 13.sp)
                Text(if (hidden) "••••••" else account?.totalEquityUsdt?.let(::formatPrice) ?: if (loading) "读取中…" else "未连接", color = colors.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(if (hidden) "••••" else account?.unrealizedPnlUsdt?.let { "未实现盈亏 ${signedMoney(it)} USDT" } ?: "--", color = account?.unrealizedPnlUsdt?.let { if (it >= 0) BinanceGreen else BinanceRed } ?: colors.muted, fontSize = 13.sp)
            }
        }
        if (error != null) ErrorText(colors, error)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AssetAction(colors, "交易", Icons.Default.ShowChart, onTrade)
            AssetAction(colors, "刷新", Icons.Default.Refresh) {}
            AssetAction(colors, "API 设置", Icons.Default.Key, onCredentialsClick)
        }
        Spacer(Modifier.height(21.dp))
        SectionTitle(colors, "资产明细", "实时读取", onClick = {})
        Spacer(Modifier.height(9.dp))
        val assets = account?.assets.orEmpty()
        if (loading && assets.isEmpty()) LoadingCard(colors, "正在读取账户资产…")
        else if (assets.isEmpty()) EmptyDataCard(colors, "暂无资产数据", "请配置对应 Binance API 凭据")
        else assets.forEach { asset ->
            ActualBalanceRow(colors, asset, hidden, onTrade)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun AssetAction(colors: QuantColors, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(Modifier.width(78.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(colors.card), contentAlignment = Alignment.Center) { Icon(icon, label, tint = colors.text, modifier = Modifier.size(23.dp)) }
        Spacer(Modifier.height(7.dp))
        Text(label, color = colors.text, fontSize = 12.sp)
    }
}

@Composable
private fun ActualBalanceRow(colors: QuantColors, asset: BinanceAssetBalance, hidden: Boolean, onTrade: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CoinAvatar(asset.asset + "USDT", 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(asset.asset, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("可用 ${if (hidden) "••••" else formatQuantity(asset.free)} · 冻结 ${if (hidden) "••••" else formatQuantity(asset.locked)}", color = colors.muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (hidden) "••••" else asset.valueUsdt?.let { "${formatPrice(it)} USDT" } ?: "--", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onTrade, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(25.dp)) { Text("交易", color = BinanceYellow, fontSize = 12.sp) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BinanceCredentialSheet(
    context: Context,
    client: BinanceApiClient,
    product: BinanceProduct,
    mode: TradingMode,
    existing: BinanceCredentials?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var apiKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("配置 Binance API", color = MaterialTheme.colorScheme.onSurface, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("${product.label} · ${mode.label}", color = MutedInk, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (mode == TradingMode.DEMO) "请使用 Binance Demo Trading 创建的 API Key。Demo 和正式环境密钥不能混用。"
                else "请只授予必要权限。API 密钥只保存在本机 Android Keystore 加密偏好中，不会上传到 GitHub。",
                color = MutedInk,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            if (existing != null) {
                Spacer(Modifier.height(8.dp))
                Text("当前已有一组凭据：${maskKey(existing.apiKey)}。重新保存会替换它。Secret 不会回显。", color = BinanceGreen, fontSize = 12.sp)
            }
            Spacer(Modifier.height(13.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                placeholder = { Text(if (existing == null) "粘贴 API Key" else "留空以保留现有 Key") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Key, null) },
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = secretKey,
                onValueChange = { secretKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Secret Key") },
                placeholder = { Text(if (existing == null) "粘贴 Secret Key" else "留空以保留现有 Secret") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Security, null) },
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = BinanceRed, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Button(
                enabled = !testing,
                onClick = {
                    val resolvedApi = apiKey.trim().ifBlank { existing?.apiKey.orEmpty() }
                    val resolvedSecret = secretKey.trim().ifBlank { existing?.secretKey.orEmpty() }
                    if (resolvedApi.isBlank() || resolvedSecret.isBlank()) {
                        error = "API Key 和 Secret Key 不能为空"
                        return@Button
                    }
                    testing = true
                    error = null
                    scope.launch {
                        try {
                            val creds = BinanceCredentials(resolvedApi, resolvedSecret)
                            client.loadAccount(product, mode, creds, emptyList())
                            BinanceCredentialStore.save(context, product, mode, creds)
                            onSaved()
                        } catch (failure: Throwable) {
                            error = "连接验证失败：${apiErrorText(failure)}"
                        } finally {
                            testing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(51.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BinanceYellow, contentColor = Ink),
            ) {
                if (testing) {
                    CircularProgressIndicator(Modifier.size(19.dp), color = Ink, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (testing) "验证中…" else "验证并保存")
            }
            if (existing != null) {
                TextButton(
                    onClick = { BinanceCredentialStore.clear(context, product, mode); onSaved() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("清除当前凭据", color = BinanceRed) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionTitle(colors: QuantColors, title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
            Text(action, color = colors.muted, fontSize = 12.sp)
            Icon(Icons.Default.ChevronRight, null, tint = colors.muted, modifier = Modifier.size(16.dp))
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
private fun CoinAvatar(symbol: String, size: Dp) {
    val base = symbol.removeSuffix("USDT").take(1).uppercase(Locale.US)
    val color = when (base) { "B" -> Color(0xFFF7931A); "E" -> Color(0xFF627EEA); "S" -> Color(0xFF9945FF); "X" -> Color(0xFF23292F); "D" -> Color(0xFFC3A634); else -> BinanceYellow }
    Box(Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Text(base, color = color, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoadingCard(colors: QuantColors, text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), color = BinanceYellow, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(text, color = colors.muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyDataCard(colors: QuantColors, title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = colors.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ErrorText(colors: QuantColors, error: String) {
    Text(error, color = BinanceRed, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp, bottom = 3.dp))
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
        drawPath(path, color, style = Stroke(width = 2.3f, cap = StrokeCap.Round))
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
        drawPath(path, color, style = Stroke(width = 3f, cap = StrokeCap.Round))
    }
}

private fun availableText(account: BinanceAccountSnapshot?, symbol: String, isBuy: Boolean): String {
    if (account == null) return "未连接"
    if (isBuy) return account.availableUsdt?.let { "${formatPrice(it)} USDT" } ?: "--"
    val asset = symbol.removeSuffix("USDT")
    return account.assets.firstOrNull { it.asset == asset }?.free?.let { "${formatQuantity(it)} $asset" } ?: "0 $asset"
}

private fun formatPrice(value: Double): String = when {
    value >= 1000 -> String.format(Locale.US, "%,.2f", value)
    value >= 1 -> String.format(Locale.US, "%,.4f", value).trimEnd('0').trimEnd('.')
    else -> String.format(Locale.US, "%.8f", value).trimEnd('0').trimEnd('.')
}

private fun formatQuantity(value: Double): String = String.format(Locale.US, "%.8f", value).trimEnd('0').trimEnd('.')
private fun signedPercent(value: Double): String = String.format(Locale.US, "%+.2f%%", value)
private fun signedMoney(value: Double): String = String.format(Locale.US, "%+.4f", value)
private fun compactVolume(value: Double): String = when {
    value >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", value / 1_000_000_000)
    value >= 1_000_000 -> String.format(Locale.US, "%.2fM", value / 1_000_000)
    else -> String.format(Locale.US, "%.0fK", value / 1_000)
}

private fun maskKey(value: String): String = if (value.length <= 8) "••••••••" else value.take(4) + "••••" + value.takeLast(4)

private fun apiErrorText(error: Throwable): String = when (error) {
    is BinanceApiException -> {
        if (error.binanceCode != null) "${error.message} (${error.binanceCode})" else error.message
    }
    else -> error.message?.takeIf { it.isNotBlank() } ?: "网络请求失败"
}
