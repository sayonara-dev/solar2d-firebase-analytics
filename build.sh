#!/usr/bin/env bash
# Builds dist/android.tgz: metadata.lua + corona.gradle + plugin.firebaseAnalytics.jar.
# Compile-only classpath: android.jar, Corona.aar (Solar2D Native), Firebase aars from Google Maven.
# Env overrides: JDK_BIN, ANDROID_JAR, CORONA_AAR.
set -euo pipefail
cd "$(dirname "$0")"

JDK_BIN="${JDK_BIN:-/c/Program Files/Unity/Hub/Editor/6000.5.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK/bin}"
ANDROID_JAR="${ANDROID_JAR:-/c/Program Files/Unity/Hub/Editor/6000.5.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/SDK/platforms/android-36/android.jar}"
CORONA_AAR="${CORONA_AAR:-/c/Program Files (x86)/Corona Labs/Corona/Native/Corona/android/lib/gradle/Corona.aar}"
MAVEN=https://dl.google.com/android/maven2
DEPS=(
  "com/google/android/gms/play-services-measurement-api/24.0.0/play-services-measurement-api-24.0.0.aar"
  "com/google/firebase/firebase-common/23.0.0/firebase-common-23.0.0.aar"
)

rm -rf build dist && mkdir -p build/libs build/classes build/pkg dist

unzip -p "$CORONA_AAR" classes.jar > build/libs/corona.jar
CP="build/libs/corona.jar"
for dep in "${DEPS[@]}"; do
  name="$(basename "$dep" .aar)"
  curl -sfL "$MAVEN/$dep" -o "build/libs/$name.aar"
  unzip -p "build/libs/$name.aar" classes.jar > "build/libs/$name.jar"
  CP="$CP;build/libs/$name.jar"
done

"$JDK_BIN/javac" -nowarn -Xlint:-options -source 1.8 -target 1.8 \
  -bootclasspath "$ANDROID_JAR" -cp "$CP" -d build/classes \
  src/android/plugin/firebaseAnalytics/*.java

"$JDK_BIN/jar" cf build/pkg/plugin.firebaseAnalytics.jar -C build/classes .
cp plugin/android/metadata.lua plugin/android/corona.gradle build/pkg/
tar --owner=0 --group=0 --numeric-owner -czf dist/android.tgz -C build/pkg metadata.lua corona.gradle plugin.firebaseAnalytics.jar
tar -tzvf dist/android.tgz
