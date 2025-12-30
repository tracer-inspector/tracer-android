# Tracer Android

Android SDK, Gradle plugin, and sample app for capturing network traffic.

## Modules
- `sdk/` – Android library (`com.github.tracer-inspector.tracer-android:sdk`)
- `gradle-plugin/` – Gradle plugin (`com.github.tracer-inspector.tracer-android:gradle-plugin`)
- `sample/` – Demo app using the SDK and plugin

## Quick start (JitPack, v1.0.2)
1) Add JitPack to repositories (settings.gradle.kts):
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```
2) Add the plugin classpath and apply the plugin in your app module:
```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.github.tracer-inspector.tracer-android:gradle-plugin:v1.0.2")
    }
}

apply(plugin = "io.tracer.plugin")
```
3) Add the SDK dependency:
```kotlin
dependencies {
    debugImplementation("com.github.tracer-inspector.tracer-android:sdk:v1.0.2")
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
