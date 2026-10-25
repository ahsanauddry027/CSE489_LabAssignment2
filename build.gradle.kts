// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 compiles Kotlin itself ("built-in Kotlin") with the Kotlin version it bundles (2.2.10).
    // Coil 3.6 is built with Kotlin 2.4, which that compiler cannot read, so we put a newer Kotlin
    // Gradle plugin on the build classpath. "apply false" means it is not applied to any module;
    // AGP's built-in Kotlin simply uses this newer version.
    alias(libs.plugins.kotlin.android) apply false
}
