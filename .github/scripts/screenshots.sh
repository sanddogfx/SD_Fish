#!/usr/bin/env bash
# Runs the screenshot tour on the emulator and pulls the screenshots.
# Installs and runs the test by hand (not connectedAndroidTest) so the app stays
# installed and its screenshots folder can be pulled afterwards.
set -u
adb emu geo fix 21.0122 52.2297   # Warsaw, so the app gets a GPS fix
./gradlew --no-daemon installDebug installDebugAndroidTest || exit 1
adb shell am instrument -w com.sdfish.app.test/androidx.test.runner.AndroidJUnitRunner | tee instrument.log
mkdir -p screenshots
adb pull /sdcard/Android/data/com.sdfish.app/files/screenshots/. screenshots/ || true
adb logcat -d > screenshots/logcat.txt || true
grep -q "^OK (" instrument.log
