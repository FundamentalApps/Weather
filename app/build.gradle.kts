import com.android.build.api.dsl.ApplicationExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val gitVersionCode = providers.exec {
    commandLine("git", "rev-list", "--count", "--first-parent", "HEAD")
}.standardOutput.asText.map { it.trim().toInt() }

val gitVersionName = providers.exec {
    commandLine("git", "describe", "--tags", "--match", "v[0-9]*")
}.standardOutput.asText.map { it.trim().removePrefix("v") }

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

fun localStringProperty(name: String): String {
    val value = localProperties.getProperty(name)
        ?: error("Missing '$name' in local.properties")
    return "\"${value.trim().trim('"')}\""
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
        versionCode = gitVersionCode.get()
        versionName = gitVersionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // FundamentalOS API; override with app.fosApiBaseUrl in local.properties (e.g. a local dev server).
        buildConfigField("String", "FOS_API_BASE_URL", localStringPropertyOr("app.fosApiBaseUrl", "https://api.fundamentalos.org"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "API_BASE_URL", localStringProperty("app.apiBaseUrl.prod"))
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
        debug {
            buildConfigField("String", "API_BASE_URL", localStringProperty("app.apiBaseUrl.dev"))
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

    ksp(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
}

extensions.configure<KspExtension>("ksp") {
    arg("room.schemaLocation", "$projectDir/schemas")
}
