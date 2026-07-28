# Minis Dev edition

This fork is configured as a separate Android developer edition:

- Application name: `Minis Dev`
- Android application ID: `com.openminis.app.dev`
- Kotlin namespace: `com.openminis.app` (kept for source compatibility)
- Embedded Alpine rootfs host directory: `<filesDir>/alpine-rootfs1`
- APK architecture: `arm64-v8a`

Use **Actions → Build Android Dev APK → Run workflow**. The resulting
`Minis-Dev-arm64-v8a.apk` is available from the workflow run's Artifacts area.
The release build uses the project's debug signing configuration, as documented
by upstream, so it can be installed directly but is not Play Store signed.
