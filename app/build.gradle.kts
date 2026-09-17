import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.lineageos.generatebp.GenerateBpPluginExtension
import org.lineageos.generatebp.models.Module
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    // Emits Android.bp + vendored libs/ for the in-tree (Soong) build: ./gradlew :app:generateBp
    alias(libs.plugins.lineageos.generatebp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

fun localStringPropertyOr(name: String, default: String): String {
    val value = localProperties.getProperty(name)?.trim()?.trim('"')?.takeIf { it.isNotBlank() } ?: default
    return "\"$value\""
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "org.fundamentalos.weather"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.fundamentalos.weather"
        minSdk = 24
        targetSdk = 35
        // Static literals, bumped by hand with each v* tag: the build needs no git history, and
        // F-Droid reads the version straight off this file for Tags-based auto-update. Kept as
        // literals (not a val) so F-Droid's manifest parser can read them. Only ever goes up.
        versionCode = 96
        versionName = "0.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // FundamentalOS API; override with app.fosApiBaseUrl in local.properties (e.g. a local dev server).
        buildConfigField("String", "FOS_API_BASE_URL", localStringPropertyOr("app.fosApiBaseUrl", "https://api.fundamentalos.org"))
    }

    // How the app reaches the phone: built into FundamentalOS as its weather app, or installed
    // on its own on any Android. The inline build is simply "Weather" and leaves out what the
    // OS already covers; the standalone one carries the OS's name.
    flavorDimensions += "distribution"
    productFlavors {
        create("inline") {
            dimension = "distribution"
            buildConfigField("boolean", "INLINE", "true")
        }
        create("standalone") {
            dimension = "distribution"
            isDefault = true
            buildConfigField("boolean", "INLINE", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("benchmark") {
            initWith(getByName("release"))
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".benchmark"
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
}

extensions.configure<KotlinAndroidProjectExtension>("kotlin") {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material)
    implementation(libs.androidx.graphics.shapes)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.accompanist.permissions)
    implementation(libs.material.motion)
    implementation(libs.haze)
    implementation(libs.kyant.shapes)
    implementation(libs.osmdroid.android)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.haze.materials)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.viewmodel.navigation)

    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Generates app/Android.bp (static_libs / aaptflags / sdk_version) and vendors non-AOSP
// dependencies under app/libs/ for the in-tree Soong build. Run: ./gradlew :app:generateBp
// The predicate decides which dependencies are assumed already present as Soong modules in the
// AOSP tree (not vendored); everything else is copied into app/libs/ as a prebuilt.
configure<GenerateBpPluginExtension> {
    targetSdk.set(35)
    minSdk.set(24)
    versionCode.set(96)
    versionName.set("0.1.3")
    availableInAOSP.set { module: Module ->
        listOf(
            // Present in the AOSP tree already.
            "androidx.",
            "org.jetbrains.",
            "com.google.android.material",
            "com.google.errorprone",
            "com.google.guava",
            "junit",
            // Provided as shared Soong prebuilts in vendor/fundamental/libraries,
            // so treat them as in-tree here: generateBp emits a static_libs name
            // reference instead of vendoring a private copy under app/libs/.
            "io.ktor",
            "io.insert-koin",
            "io.github.fornewid",
            "io.github.kyant0",
            "dev.chrisbanes.haze",
            "com.google.accompanist",
            "co.touchlab",
            "org.slf4j",
            "org.osmdroid",
        ).any { module.group.startsWith(it) || module.group == it }
    }
}

// generatebp resolves a hardcoded "releaseRuntimeClasspath" configuration, but this app has a
// "distribution" flavor dimension, so the real runtime classpaths are per-flavor
// (inlineReleaseRuntimeClasspath / standaloneReleaseRuntimeClasspath) and no bare
// releaseRuntimeClasspath exists. FundamentalOS ships the "inline" distribution, so expose a
// resolvable configuration under the name generatebp expects that mirrors the inline release
// runtime classpath (same dependencies + variant attributes) purely for :app:generateBp.
afterEvaluate {
    val inlineRelease = configurations.getByName("inlineReleaseRuntimeClasspath")
    configurations.create("releaseRuntimeClasspath") {
        extendsFrom(inlineRelease)
        isCanBeConsumed = false
        isCanBeResolved = true
        inlineRelease.attributes.keySet().forEach { key ->
            @Suppress("UNCHECKED_CAST")
            val typed = key as org.gradle.api.attributes.Attribute<Any>
            attributes.attribute(typed, inlineRelease.attributes.getAttribute(typed) as Any)
        }
    }
}
