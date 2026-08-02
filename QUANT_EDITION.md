# Binance Quant Android edition

This branch is a clean rebuild of the Android product surface on top of the
OpenMinis runtime. The old quant implementation is not used. The first-run
surface is a dedicated Binance-style quant dashboard with:

- public Binance 24h market data with a fallback demo snapshot;
- explicit DEMO / LIVE mode separation;
- home, market, trade, bot and asset tabs;
- local demo order flow and strategy state;
- grid, DCA, rebalance and risk-control UI seams for later authenticated APIs;
- the original AI agent retained behind the AI assistant action.

## Build

Use **Actions → Build Binance Quant Android APK → Run workflow**. The workflow
builds the Android release using JDK 17, NDK r28 and the OpenMinis PRoot/Alpine
sandbox, then uploads the universal APK and an arm64-named APK artifact. No AAB
is generated.

The LIVE switch is intentionally a UI mode gate in this first rebuild. Real
Binance API keys and order signing must be added behind a separate encrypted
credential store and server-time/signature validation layer before real funds
are enabled.
