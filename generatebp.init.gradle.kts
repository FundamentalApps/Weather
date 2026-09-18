// Init script for the in-tree Soong Android.bp generation. Run:
//   ./gradlew :app:generateBp -I generatebp.init.gradle.kts
//
// Kept as an init script (not part of the project build files) so the LineageOS gradle-generatebp
// plugin -- and its non-standard raw.githubusercontent.com maven repo, which F-Droid's scanner
// rejects -- stay entirely out of the ordinary build. A clean gradle build (assembleStandaloneRelease,
// what F-Droid runs) never sees the plugin, its repo, or these types. generatebp only emits
// app/Android.bp and vendors app/libs/ for the Soong build; it has no effect on the app APK.
import org.lineageos.generatebp.GenerateBpPlugin
import org.lineageos.generatebp.GenerateBpPluginExtension
import org.lineageos.generatebp.models.Module

initscript {
    repositories {
        maven("https://raw.githubusercontent.com/lineage-next/gradle-generatebp/v1.32/.m2")
    }
    dependencies {
        classpath("org.lineageos:gradle-generatebp:1.32")
    }
}

gradle.rootProject {
    project(":app") {
        // Apply by class (available on this init script's classpath) rather than by id: the plugin
        // id registry is resolved against the project buildscript classpath, which -- by design --
        // does not carry generatebp.
        pluginManager.apply(GenerateBpPlugin::class.java)

        // Which dependencies are assumed already present as Soong modules in the AOSP tree (not
        // vendored); everything else is copied into app/libs/ as a prebuilt.
        configure<GenerateBpPluginExtension> {
            targetSdk.set(35)
            minSdk.set(24)
            versionCode.set(102)
            versionName.set("0.1.9")
            availableInAOSP.set { module: Module ->
                listOf(
                    // Present in the AOSP tree already.
                    "androidx.",
                    "org.jetbrains.",
                    "com.google.android.material",
                    "com.google.errorprone",
                    "com.google.guava",
                    "junit",
                    // Provided as shared Soong prebuilts in vendor/fundamental/libraries, so treat
                    // them as in-tree here: generateBp emits a static_libs name reference instead of
                    // vendoring a private copy under app/libs/.
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
    }
}

// generatebp resolves a hardcoded "releaseRuntimeClasspath" configuration, but this app has a
// "distribution" flavor dimension, so the real runtime classpaths are per-flavor
// (inlineReleaseRuntimeClasspath / standaloneReleaseRuntimeClasspath) and no bare
// releaseRuntimeClasspath exists. FundamentalOS ships the "inline" distribution, so expose a
// resolvable configuration under the name generatebp expects that mirrors the inline release
// runtime classpath. Done in projectsEvaluated: those flavour configs exist only after AGP has
// finished evaluating :app (an init-registered afterEvaluate would run before them).
gradle.projectsEvaluated {
    val app = rootProject.project(":app")
    val inlineRelease = app.configurations.getByName("inlineReleaseRuntimeClasspath")
    app.configurations.create("releaseRuntimeClasspath") {
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
