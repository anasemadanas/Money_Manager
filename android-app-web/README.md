# Money Manager Android

Money Manager Android is a small Android WebView companion application for the hosted Money Manager web service. It lives as a root-level project because it has its own Gradle build, SDK requirements, and release artifact, independent of the Spring Boot source tree.

## Behavior

- Opens `https://money-manager-t4ed.onrender.com`.
- Stores finance data on the web server rather than locally.
- Supports browser-style back navigation inside the app.
- Sends external links to the installed browser.
- Shows a retry screen when the hosted service cannot be reached.

## Requirements

- JDK 17
- Android SDK API 35

## Build

From the repository root:

```powershell
cd android-app
.\gradlew.bat assembleDebug
```

The debug APK is generated at:

```text
android-app\app\build\outputs\apk\debug\app-debug.apk
```
