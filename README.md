# Money Manager Android

Jetpack Compose Android client for the Money Manager API.

## Prerequisites

Install these before running the Android app locally:

- Android Studio with the Android SDK.
- JDK 17.
- Android SDK Platform 35.
- Android SDK Build Tools compatible with the installed Android Gradle Plugin.
- An Android emulator or a physical Android device.
- Docker Desktop if you also want to run the backend locally.

Verify Java is available:

```sh
java -version
```

Verify Gradle can start:

```sh
./gradlew --version
```

If `./gradlew` fails because Java is missing, install JDK 17 and make sure `JAVA_HOME` points to it.

## Start The Backend

From this directory:

```sh
cd ../money-manager-server
docker compose up --build
```

Verify the API:

```sh
curl http://localhost:8080/health
```

Expected response:

```text
ok
```

The Android emulator uses `10.0.2.2` to reach the host machine, so the app defaults to:

```text
http://10.0.2.2:8080
```

That value is configured in `app/build.gradle.kts` as `moneyManagerDebugApiBaseUrl`.

## Open In Android Studio

1. Open Android Studio.
2. Choose `Open`.
3. Select the `money-manager-android` directory.
4. Wait for Gradle sync to finish.
5. Select an emulator or connected device.
6. Run the `app` configuration.

## Build From Terminal

```sh
cd money-manager-android
./gradlew :app:assembleDebug
```

The debug APK will be generated under:

```text
app/build/outputs/apk/debug/
```

## SDK Location

Android Studio usually creates `local.properties` automatically. If Gradle asks for it, create:

```properties
sdk.dir=/path/to/android/sdk
```

Common macOS SDK location:

```properties
sdk.dir=/Users/YOUR_USER/Library/Android/sdk
```

`local.properties` is ignored by git.

## Emulator Setup

1. Open Android Studio.
2. Open Device Manager.
3. Create an emulator with API 35 or another supported API level.
4. Start the emulator.
5. Start the backend with Docker Compose.
6. Run the app.

For the emulator, keep the default backend URL:

```text
http://10.0.2.2:8080
```

## Physical Device Setup

For a real phone, `10.0.2.2` will not work. Use your computer's LAN IP address.

Find your IP address, for example:

```sh
ipconfig getifaddr en0
```

Build with:

```sh
./gradlew :app:assembleDebug -PmoneyManagerDebugApiBaseUrl=http://YOUR_MACHINE_IP:8080
```

Both the phone and computer must be on the same network. Your firewall must allow inbound traffic to port `8080`.

## Notification Permission

The app requests notification permission on Android 13+ because it shows local notifications for simulated wallet purchase signals.

If you deny the permission, the `Simulate wallet signal` button will not show a notification. Re-enable permission from:

```text
Android Settings -> Apps -> Money Manager -> Notifications
```

## End-To-End Local Smoke Test

1. Start the backend:

```sh
cd ../money-manager-server
docker compose up --build
```

2. Verify backend health:

```sh
curl http://localhost:8080/health
```

3. Run the Android app from Android Studio or terminal.

4. Register a new account or log in.

5. Add a transaction with the `+` button.

6. Confirm it appears in the transaction list.

7. Tap `Simulate wallet signal`.

8. Confirm a notification appears:

```text
Physical purchase detected
```

9. Tap the notification.

10. Confirm the app opens the Add Transaction modal prefilled as an expense with category `shopping` and today's date.

11. Enter an amount and save.

12. Confirm the transaction appears in the list and the spending pie updates.

## Current Features

- Register and login with email/password.
- Store the JWT locally and delete it on logout.
- Dashboard with monthly spending pie chart.
- Tap pie slices to filter transactions by expense category.
- Previous month navigation, with future months blocked.
- Grouped daily transaction list.
- Add, edit, and delete EUR transactions.
- Local notification flow for simulated wallet purchase signals.
- Replaceable purchase signal abstraction for future BLE integration.

## Troubleshooting

If login fails with `Failed to connect to /10.0.2.2:8080`:

- Start the backend first.
- Check `curl http://localhost:8080/health` on the host machine.
- Confirm the app was built with the emulator URL `http://10.0.2.2:8080`.

If notifications do not appear:

- Confirm notification permission is granted.
- On Android 13+, check app notification settings.
- Tap `Simulate wallet signal` while logged in.

If Gradle cannot find Java:

- Install JDK 17.
- Set `JAVA_HOME`.
- Restart Android Studio or your terminal.

If Gradle cannot find the Android SDK:

- Open the project in Android Studio and let it create `local.properties`.
- Or create `local.properties` manually with `sdk.dir=/path/to/android/sdk`.

If using a physical device and the backend cannot be reached:

- Use your computer's LAN IP instead of `10.0.2.2`.
- Ensure phone and computer are on the same Wi-Fi.
- Ensure port `8080` is not blocked by firewall.

If you need a clean app login state:

- Uninstall the app from the emulator/device.
- Reinstall from Android Studio.

## Future BLE Integration

The current app has a fake purchase signal source. A future BLE implementation should replace or sit beside that source and call the same notification path when the wallet device emits a purchase signal.

Expected future shape:

```text
BLE wallet device -> BlePurchaseSignalSource -> PurchaseNotificationManager -> local notification -> Add Transaction modal
```
