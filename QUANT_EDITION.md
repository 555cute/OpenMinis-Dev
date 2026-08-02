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
- Agent tools are first-class Binance tools: market, account, order book and order submission;
- order submission always pauses for human approval in the Android UI;
- the model never receives API secrets and cannot bypass approval with a parameter;
- the original AI agent runtime remains available as the foundation, but the product prompt and tool surface are Binance-first;

## Build

Use **Actions → Build Binance Quant Android APK → Run workflow**. The workflow
builds the Android release using JDK 17, NDK r28 and the OpenMinis PRoot/Alpine
sandbox, then uploads the universal APK and an arm64-named APK artifact. No AAB
is generated.

The LIVE switch is connected to the official Binance production endpoints. Use
Binance Demo Trading first. The app validates credentials against the selected
product/mode before saving them; keys remain in Android Keystore-backed
preferences and are never logged or committed.
