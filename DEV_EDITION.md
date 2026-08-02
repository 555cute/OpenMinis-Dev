# Minis Quant

This Android build is now the Binance Quant edition. See [QUANT_EDITION.md](QUANT_EDITION.md) and run the **Build Binance Quant Android APK** workflow.

- Application name: `币安量化`
- Android application ID: `com.openminis.quant`
- Kotlin namespace: `com.openminis.app` (kept for runtime compatibility)
- Embedded Alpine rootfs host directory: `<filesDir>/alpine-rootfs1`
- APK architecture: `arm64-v8a`

Use **Actions → Build Binance Quant Android APK → Run workflow**. The resulting
APKs are available from the workflow run's `Binance-Quant-APKs` artifact.
The release build uses the project's debug signing configuration, as documented
by upstream, so it can be installed directly but is not Play Store signed.
