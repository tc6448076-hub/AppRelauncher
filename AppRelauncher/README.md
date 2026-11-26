# App Relauncher (Android / Android TV)

Opens a chosen app again every N minutes. Everything is set on screen with the TV remote.

## Settings (all set by the user)
- App to launch (picked from the installed apps)
- Interval in minutes (1 or more)
- Launch straight away when started (on/off)
- Restart the app fresh each time (on/off)
- Start this launcher automatically when the TV starts up (on/off)
- Launch the app again whenever the TV is switched on / wakes from standby (on/off)

## Build the APK
**Option A - Android Studio:** open this folder, wait for sync, then Build > Build APK(s).
Result: `app/build/outputs/apk/debug/app-debug.apk`

**Option B - GitHub (no install needed):** upload this folder to a GitHub repository, open the
Actions tab, run "Build APK", then download `AppRelauncher-apk`.

## Install on Android TV
- With adb: `adb connect <tv-ip>` then `adb install app-debug.apk`
- Or copy the APK to a USB drive / use "Send Files to TV" and open it with a file manager.
  (Allow "Install unknown apps" for that file manager.)

## First-time setup on the TV
1. Open **App Relauncher** from the TV home screen (Apps row).
2. Choose the app, set the minutes, tap **Save & Start**.
3. Allow **Display over other apps** when asked. If the TV has no such screen, run on a computer:
   `adb shell appops set com.example.apprelauncher SYSTEM_ALERT_WINDOW allow`
4. Use **Launch app now (test)** to check it works.

## Running automatically when the TV is on
- **TV fully restarts:** the launcher starts by itself and opens the app about 15 seconds after startup.
- **TV wakes from standby:** the launcher keeps running in standby and re-opens the app about 5 seconds after the TV wakes.
- Open the launcher once after installing and press **Save & Start**. Android does not allow an app to auto-start before it has been opened.
- Tip: turn off battery saving / "app standby" for this app in the TV settings if your device has it.
