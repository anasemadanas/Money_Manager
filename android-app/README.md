# Money Manager Android

Money Manager Android is a standalone, offline personal finance app located in
the repository root at `android-app`. It is native Android Java and does not
connect to the web service.

## Offline Features

- Dashboard with current-month income, expenses, balance, recent entries, and budget progress.
- Local transaction management for income and expenses.
- Monthly category budgets with warning and limit progress states.
- Savings goals with contribution tracking.
- Financial notes.
- Private SQLite storage on the device; the manifest requests no Internet permission.

Money amounts are persisted as integer cents to avoid floating-point rounding
errors. Removing the application clears its local database unless device backup
behavior is changed later.

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
