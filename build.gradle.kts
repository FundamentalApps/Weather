// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    // Declared here (apply false) so its version is known at the root classpath. generatebp pulls
    // the Kotlin Gradle plugin onto the build classpath, which otherwise makes :app's versioned
    // parcelize request fail with "already on the classpath with an unknown version".
    alias(libs.plugins.kotlin.parcelize) apply false
}
