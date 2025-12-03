plugins {
    // ✅ Use Android Gradle Plugin 8.7.1 or newer
    id("com.android.application") version "8.7.1" apply false

    // ✅ Upgrade Kotlin to 2.0.21 (compatible with Compose & Vico)
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false

    // ✅ Match KSP to Kotlin 2.0.21
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false

    // ✅ Compose Compiler Plugin for Kotlin 2.0+
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
