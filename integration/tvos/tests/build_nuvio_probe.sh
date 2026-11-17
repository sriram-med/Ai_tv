#!/usr/bin/env bash
set -euo pipefail
PROBE_DIR="$RUNNER_TEMP/nuvio-probe"
mkdir -p "$PROBE_DIR/src/org/example/nuviolegacy" "$PROBE_DIR/classes" "$PROBE_DIR/dex"
cat > "$PROBE_DIR/AndroidManifest.xml" <<'XML'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="org.example.nuviolegacy"><uses-sdk android:minSdkVersion="24" android:targetSdkVersion="30"/><application android:label="Nuvio Legacy"><activity android:name=".MainActivity" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/><category android:name="android.intent.category.LEANBACK_LAUNCHER"/></intent-filter><intent-filter><action android:name="android.intent.action.VIEW"/><category android:name="android.intent.category.DEFAULT"/><category android:name="android.intent.category.BROWSABLE"/><data android:scheme="nuvio"/></intent-filter></activity></application></manifest>
XML
cat > "$PROBE_DIR/src/org/example/nuviolegacy/MainActivity.java" <<'JAVA'
package org.example.nuviolegacy;
public class MainActivity extends android.app.Activity {
 public void onCreate(android.os.Bundle state){super.onCreate(state);android.widget.TextView t=new android.widget.TextView(this);t.setText("NUVIO PROBE RECEIVED "+getIntent().getDataString());setContentView(t);}
}
JAVA
SDK_JAR="$ANDROID_HOME/platforms/android-34/android.jar"
BUILD_TOOLS="$ANDROID_HOME/build-tools/34.0.0"
javac -source 11 -target 11 -classpath "$SDK_JAR" -d "$PROBE_DIR/classes" "$PROBE_DIR/src/org/example/nuviolegacy/MainActivity.java"
"$BUILD_TOOLS/d8" --lib "$SDK_JAR" --min-api 24 --output "$PROBE_DIR/dex" "$PROBE_DIR/classes/org/example/nuviolegacy/MainActivity.class"
"$BUILD_TOOLS/aapt" package -f -M "$PROBE_DIR/AndroidManifest.xml" -I "$SDK_JAR" -F "$PROBE_DIR/probe.apk"
(cd "$PROBE_DIR/dex" && zip -q "$PROBE_DIR/probe.apk" classes.dex)
keytool -genkeypair -keystore "$PROBE_DIR/test.jks" -storepass android -keypass android -alias probe -dname 'CN=AI TV Emulator Probe' -keyalg RSA -validity 30 -noprompt
"$BUILD_TOOLS/apksigner" sign --ks "$PROBE_DIR/test.jks" --ks-pass pass:android --out nuvio-probe.apk "$PROBE_DIR/probe.apk"
