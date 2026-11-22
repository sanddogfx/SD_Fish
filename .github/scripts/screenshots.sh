#!/usr/bin/env bash
# Runs the screenshot tour on the emulator and pulls the screenshots.
# Installs and runs the test by hand (not connectedAndroidTest) so the app stays
# installed and its screenshots folder can be pulled afterwards.
set -u
adb emu geo fix 21.0122 52.2297   # Warsaw, so the app gets a GPS fix
# The slow CI emulator often shows "Pixel Launcher isn't responding" over the app; hide such dialogs.
adb shell settings put global hide_error_dialogs 1
./gradlew --no-daemon installDebug installDebugAndroidTest || exit 1
adb shell am instrument -w com.sdfish.app.test/androidx.test.runner.AndroidJUnitRunner | tee instrument.log
mkdir -p screenshots
adb pull /sdcard/Android/data/com.sdfish.app/files/screenshots/. screenshots/ || true
adb logcat -d > screenshots/logcat.txt || true
adb logcat -d -s WeatherSync:V AndroidRuntime:E > screenshots/app.log || true
u='https://api.open-meteo.com/v1/forecast?latitude=52.23&longitude=21.01&hourly=temperature_2m&forecast_days=1'
echo "runner curl default UA: $(curl -s -o /dev/null -w '%{http_code}' "$u")" >> screenshots/app.log
echo "runner curl Dalvik UA: $(curl -s -o /dev/null -w '%{http_code}' -A 'Dalvik/2.1.0 (Linux; U; Android 14)' "$u")" >> screenshots/app.log
echo "runner curl app UA: $(curl -s -o /dev/null -w '%{http_code}' -A 'LoopcastFishing/0.1 (Android)' "$u")" >> screenshots/app.log
grep -q "^OK (" instrument.log
