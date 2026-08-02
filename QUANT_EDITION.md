# Binance Quant Android edition

This branch is a clean rebuild of the Android product surface on top of the
OpenMinis runtime. The old quant implementation is not used. The first-run
surface is a dedicated Binance-style quant dashboard with:

- official Binance 24h ticker, klines and order-book data;
- Spot Demo Mode (`demo-api.binance.com`) and USDⓈ-M Futures Demo (`demo-fapi.binance.com`);
- explicit DEMO / LIVE mode separation;
- Android Keystore-backed API Key / Secret storage;
- HMAC-SHA256 signed account and order requests with Binance server time;
- home, market, trade, strategy and asset tabs;
- no fake balance, PnL, order-book or order-success fallback;
- the original AI agent retained behind the AI assistant action.

## Build

Use **Actions → Build Binance Quant Android APK → Run workflow**. The workflow
builds the Android release using JDK 17, NDK r28 and the OpenMinis PRoot/Alpine
sandbox, then uploads the universal APK and an arm64-named APK artifact. No AAB
is generated.

The LIVE switch is connected to the official Binance production endpoints. Use
Binance Demo Trading first. The app validates credentials against the selected
product/mode before saving them; keys remain in Android Keystore-backed
preferences and are never logged or committed.
