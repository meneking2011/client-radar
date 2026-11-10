# ClientRadar Companion

ClientRadar Companion is the Android companion app for **ClientRadar**. It is designed to detect potentially relevant customer enquiries appearing in Facebook and Facebook Lite notifications, match those notifications against keywords saved in a user's ClientRadar workspace, and send matching leads to that workspace for the user to review.

The companion is a notification-capture bridge between Android and the ClientRadar web app. It is not a Facebook automation or messaging bot.

> **Project status:** The Android app is under active development. The repository currently includes an automated APK build and Android-emulator startup test. The latest recorded emulator test installed the APK but failed to launch the launcher activity because Android could not find `ai.clientradar.companion.MainActivity`. This is a known unresolved build/runtime issue; do not treat the app as production-ready until the launch test passes.

## 1. What the product does

A typical workflow is:

1. The user signs in to their ClientRadar web workspace.
2. The user generates a temporary phone-pairing code in the ClientRadar dashboard.
3. The user opens ClientRadar Companion, enters the code, and connects the phone.
4. The user grants Android notification access to ClientRadar Companion in system settings.
5. The companion receives notifications from Facebook or Facebook Lite.
6. It checks the notification text against the keywords saved for the connected ClientRadar workspace.
7. Relevant matches are sent to ClientRadar using the paired device's authentication token.
8. The user reviews the captured lead in ClientRadar and decides what to do next.

The app is intended to support **one active ClientRadar workspace/client connection per phone at a time**. Disconnecting clears the locally saved pairing token.

### What it does not do

- It does not send Facebook messages, comments, friend requests, or replies.
- It does not automatically contact a lead.
- It does not scrape Facebook pages or browse Facebook.
- It does not read all notifications indiscriminately: the listener is configured to handle notifications from the Facebook and Facebook Lite Android packages.
- It does not request access to contacts, photos, the microphone, SMS, or device location.

Notification access is powerful Android access. Users must explicitly enable it in Android settings and should understand that Android grants a notification listener visibility into notifications delivered to it. The app's intended processing scope is Facebook/Facebook Lite notifications only.

## 2. Main parts of the system

There are two cooperating products:

### ClientRadar web app

The web app is the user's workspace. It is responsible for the account/workspace experience, pairing-code generation, saved keyword configuration, and receiving/displaying captured opportunities.

- Web app: https://clientradar-fxrsau.v2.appdeploy.ai/
- AppDeploy project: `clientradar-fxrsau`
- Source repository: https://github.com/meneking2011/client-radar

The Android app depends on the web app's pairing and notification-ingestion API contracts. If those API routes or payload formats change, the Android client may need corresponding changes.

### ClientRadar Companion Android app

The Android app runs on the user's phone. Its main responsibilities are:

- Pairing with the ClientRadar workspace.
- Securely storing the pairing token.
- Requesting the user's notification-listener authorization through Android settings.
- Listening for notifications from supported Facebook apps.
- Retrieving/caching the workspace's keyword configuration.
- Matching notification content against those keywords.
- Forwarding qualifying notifications to ClientRadar.

## 3. Android app structure

The original Android project is stored in the repository as `ClientRadar-Android-Companion-Source.zip`. The repository's `android-companion/` directory contains files that are overlaid onto the extracted project by the GitHub Actions workflow.

Important custom/overlaid files:

| Path | Responsibility |
| --- | --- |
| `android-companion/app/src/main/AndroidManifest.xml` | Declares the launcher activity, notification-listener service, Internet permission, app theme and app label. |
| `android-companion/app/src/main/java/ai/clientradar/companion/MainActivity.kt` | Native Android pairing screen, connection status, notification-access status, settings shortcut and disconnect action. |
| `android-companion/app/src/main/res/values/strings.xml` | App name and notification-listener label. |
| `android-companion/app/src/main/res/values/styles.xml` | App theme and system-bar styling. |
| `android-companion/app/src/main/res/values/colors.xml` | Launcher icon background color. |
| `android-companion/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive launcher icon definition. |
| `android-companion/app/src/main/res/drawable/ic_launcher_foreground.xml` | Launcher icon foreground artwork. |

The extracted Android source project also contains the supporting Kotlin classes. The key responsibilities are:

- **`MainActivity`** — user interface and pairing flow.
- **`FacebookNotificationListener`** — Android `NotificationListenerService` implementation; receives notification events and limits processing to supported Facebook app package names.
- **`ApiClient`** — communicates with ClientRadar's backend, including exchanging a pairing code and sending notification matches.
- **`SecurePrefs`** — stores and clears the paired-device token using AndroidX encrypted preferences.

The package/namespace used by the Android app is:

```
ai.clientradar.companion
```

The launcher activity declared in the manifest is:

```
ai.clientradar.companion.MainActivity
```

## 4. Technology and tools

| Technology/tool | Use |
| --- | --- |
| Kotlin | Native Android application and notification-listener implementation. |
| Android SDK / Android framework | Activity lifecycle, notification listener, system settings, UI widgets and device integration. |
| Gradle | Android project build system. |
| Java 17 (Temurin) | Java runtime used by the CI build. |
| AndroidX EncryptedSharedPreferences | Encrypted local storage for the workspace pairing token. |
| Android NotificationListenerService | Receives notifications after the user grants notification access. |
| GitHub | Source control and project collaboration. |
| GitHub Actions | Automated APK build and emulator test workflow. |
| Android Emulator (API 29) | Automated launch/startup validation against an Android 10 environment. |
| AppDeploy | Hosting/deployment platform used for the ClientRadar web app. |

The current workflow downloads Gradle 8.9, installs Android SDK platform 35 and Build Tools 35.0.0, then changes the extracted Android app's `targetSdk` from 35 to 29 before building. The emulator test runs at API level 29. Keep these workflow choices documented and review compatibility/security implications before changing target SDK versions.

## 5. Permissions and privacy boundaries

The overlaid manifest currently declares:

- `android.permission.INTERNET` — required for pairing, keyword retrieval and sending matches to ClientRadar.
- A notification-listener service protected by `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` — Android uses this protected binding permission for notification-listener services. The user must separately enable notification access in system settings.

The app does **not** declare permissions for contacts, camera, microphone, photos/media, SMS, phone calls, or location in the current overlay manifest.

The manifest also sets `allowBackup=false` and `usesCleartextTraffic=false`.

When changing notification handling, preserve the narrow source-app filter for:

- Facebook: `com.facebook.katana`
- Facebook Lite: `com.facebook.lite`

Do not expand collection to unrelated apps without a clear product requirement and an updated privacy explanation.

## 6. Pairing and data flow

At a high level, pairing works as follows:

1. ClientRadar creates a short-lived pairing code for the signed-in workspace. The Android UI currently tells the user that the code expires after five minutes.
2. The user enters the code in the companion app.
3. `ApiClient.exchangeCode(code)` exchanges it with the ClientRadar service for an authentication token.
4. `SecurePrefs.saveToken(...)` stores the returned token locally.
5. The listener uses the token when communicating with ClientRadar.
6. Keyword configuration is retrieved from the connected workspace and cached on the device for a limited period (currently 10 minutes, according to the implementation notes).
7. Matching notifications are forwarded to the ClientRadar service for review in the web workspace.

**Important for contributors:** Before changing endpoints, request/response fields, authentication, keyword retrieval, or notification payloads, inspect both the Android API client and the corresponding ClientRadar web/backend implementation. Keep the contract synchronized. Do not invent endpoint paths or payload fields based only on this overview.

## 7. Build and automated test

The build is currently automated through:

```
.github/workflows/android-build.yml
```

The workflow:

1. Checks out the repository.
2. Sets up Java 17.
3. Installs Android SDK components.
4. Downloads Gradle 8.9.
5. Extracts `ClientRadar-Android-Companion-Source.zip`.
6. Copies `android-companion/` over the extracted Android project.
7. Patches the extracted app's target SDK to 29.
8. Runs `assembleDebug`.
9. Starts an Android 10 / API 29 emulator.
10. Installs and launches the APK.
11. Collects logcat/activity diagnostics and uploads them as a workflow artifact.
12. Uploads the debug APK as a separate artifact.

To run the workflow manually, open the repository's **Actions** tab, select **Build ClientRadar Android APK**, and choose **Run workflow**. A successful Gradle build alone is not proof that the app works: the emulator must also install and launch it, and the process should remain alive after startup.

### Local build (developer machine)

The exact Gradle configuration is contained in the extracted project. A developer with Android SDK tools and Java 17 can use the same build sequence as CI:

```bash
unzip ClientRadar-Android-Companion-Source.zip -d source
cp -R android-companion/. source/ClientRadar-Android-Companion/
# Apply the same targetSdk adjustment used by CI if reproducing its build.
cd source/ClientRadar-Android-Companion
gradle assembleDebug
```

The debug APK is expected at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Use the repository's GitHub Actions workflow as the source of truth for the CI build environment and exact SDK setup.

## 8. Current known issue: launcher activity is missing at runtime

The latest recorded emulator diagnostic showed:

```
FATAL EXCEPTION: main
java.lang.RuntimeException: Unable to instantiate activity
Caused by: java.lang.ClassNotFoundException:
Didn't find class "ai.clientradar.companion.MainActivity"
```

This means Android read the launcher activity name from the manifest but could not load that class from the installed APK. It is not evidence of a notification-access permission problem.

The corresponding build log showed Java compilation tasks but did not show a `compileDebugKotlin` task. That is a significant clue to investigate, but it has **not yet been proven** to be the root cause.

### Required debugging sequence

A contributor should investigate this before changing unrelated UI or notification code:

1. Inspect the extracted project's root `build.gradle`, `settings.gradle`, and `app/build.gradle`.
2. Verify that the app module applies the Android application plugin and Kotlin Android plugin, and that the Kotlin plugin version is compatible with the Gradle/Android Gradle Plugin versions.
3. Verify that `:app` is included in the Gradle settings and that the Kotlin source directory is included in the app module's source set.
4. Verify the actual package declaration at the top of `MainActivity.kt` matches `ai.clientradar.companion`.
5. Confirm the manifest's launcher activity resolves to that same fully qualified class.
6. Inspect the built APK's DEX contents and verify that `ai.clientradar.companion.MainActivity` is actually present.
7. Rebuild, install on the API 29 emulator, launch the app, and confirm its process remains alive.
8. Only after launch succeeds should notification permission, pairing and end-to-end notification delivery be tested.

Do not mark this issue fixed because Gradle reports `BUILD SUCCESSFUL`. The fix is verified only when the class is packaged and the emulator can launch the activity successfully.

## 9. Guidance for developers contributing

Before making changes:

- Read this README and inspect the complete extracted Android project, not only the overlay directory.
- Treat `ClientRadar-Android-Companion-Source.zip` as the base Android project and `android-companion/` as the repository-managed overlay applied by CI.
- Check the GitHub Actions workflow before changing paths, build commands, SDK versions, or artifact locations.
- Keep the package name and manifest declarations synchronized with Kotlin package/class names.
- Keep API contracts synchronized with the ClientRadar web/backend implementation.
- Preserve secure token storage; do not log or commit pairing tokens, API secrets, or personal notification contents.
- Keep notification collection restricted to the intended Facebook packages.
- Do not add permissions unless they are required, explained, and reflected in the manifest and privacy documentation.
- Test both build success and actual emulator launch. For notification changes, also test with notification access enabled and disabled.
- Include the workflow run and relevant logcat evidence when reporting a bug or claiming a fix.

### Suggested contribution checklist

- [ ] Understand the web-to-Android pairing and API contract.
- [ ] Confirm the app compiles Kotlin sources.
- [ ] Confirm the launcher class exists in the APK's DEX.
- [ ] Confirm installation and launch on Android API 29.
- [ ] Confirm the app process remains alive after launch.
- [ ] Test pairing and disconnect.
- [ ] Test notification access enabled/disabled.
- [ ] Test Facebook and Facebook Lite package filtering.
- [ ] Test keyword matching and delivery to the correct workspace.
- [ ] Check that no unrelated permissions or data collection were introduced.

## 10. Repository map

```
.
├── .github/
│   └── workflows/
│       └── android-build.yml
├── android-companion/
│   └── app/src/main/
│       ├── AndroidManifest.xml
│       ├── java/ai/clientradar/companion/
│       │   └── MainActivity.kt
│       └── res/
│           ├── drawable/
│           ├── mipmap-anydpi-v26/
│           └── values/
└── ClientRadar-Android-Companion-Source.zip
```

The extracted project contains additional files and Kotlin classes not duplicated in the overlay tree. Inspect the ZIP when you need the full Gradle configuration, notification listener, API client, secure preferences implementation, or other original project resources.

---

## Contact / project context

- Product: ClientRadar
- Android companion package: `ai.clientradar.companion`
- Repository: https://github.com/meneking2011/client-radar
- ClientRadar web app: https://clientradar-fxrsau.v2.appdeploy.ai/

This README describes the current repository layout and known implementation state. Update it whenever the app architecture, API contract, permissions, build pipeline, or known issues change.
