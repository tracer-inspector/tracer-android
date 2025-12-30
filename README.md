# Tracer Android

Android SDK, Gradle plugin, and sample app for capturing network traffic.

## Modules
- `sdk/` – Android library (`io.tracer:sdk`)
- `gradle-plugin/` – Gradle plugin (`io.tracer.plugin`)
- `sample/` – Demo app using the SDK and plugin

## Quick start
1) Include the build in your app’s `settings.gradle.kts`:
```kotlin
includeBuild("../tracer-android")
```
2) Apply the plugin in your app module:
```kotlin
plugins {
  id("io.tracer.plugin")
}
```
3) Add the SDK dependency (version from `VERSION`):
```kotlin
dependencies {
  debugImplementation("io.tracer:sdk:1.0.0")
}
```
4) Initialize Tracer in your `Application`/debug init:
```kotlin
io.tracer.Tracer.init()
```
5) Run the web UI/server (see `tracer-web`) and start your app; traffic will stream to the UI.

## Build
```bash
./gradlew assemble
```

## License
MIT – see `LICENSE`.
