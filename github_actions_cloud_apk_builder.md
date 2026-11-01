name: Build TikhiKhabar Studio Android APK

on:
  push:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build-apk:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Generate Gradle Wrapper (if not committed)
        run: |
          if [ ! -f "./gradlew" ]; then
            gradle wrapper --gradle-version 8.5
          fi
          chmod +x ./gradlew

      - name: Build Debug APK
        run: ./gradlew assembleDebug --stacktrace

      - name: Upload TikhiKhabar Studio APK
        uses: actions/upload-artifact@v4
        with:
          name: TikhiKhabarStudio-APK
          path: app/build/outputs/apk/debug/app-debug.apk