import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.aboutLibraries.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.roborazzi)
}

// Release signing comes from keystore.properties beside settings.gradle.kts, which is never committed. Without it the
// release build is left unsigned.
val keystoreProperties: Properties? = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

android {
    namespace = "io.github.barqallayl.burkan"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.barqallayl.burkan"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Code and resource shrinking; library keep rules are in src/main/keepRules/.
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        // MetroAppComponentFactory constructs activities, services and receivers, so they take constructor arguments.
        disable += "Instantiatable"
    }
    testOptions {
        unitTests {
            // Without the merged resources every screenshot renders blank.
            isIncludeAndroidResources = true
            all {
                it.systemProperties["robolectric.pixelCopyRenderMode"] = "hardware"
            }
        }
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xreturn-value-checker=check")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

// Virtual time in coroutine tests is experimental API; it stays out of the app's own code.
tasks.withType<KotlinCompile>().configureEach {
    if (name.contains("UnitTest")) {
        compilerOptions.optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

// Conscrypt's Android build carries native code for Android only. On the JVM it shadows the Conscrypt Robolectric
// installs for itself, and every Robolectric test fails to load it.
configurations.configureEach {
    if (name.endsWith("UnitTestRuntimeClasspath")) {
        exclude(group = "org.conscrypt", module = "conscrypt-android")
    }
}

roborazzi {
    @OptIn(ExperimentalRoborazziApi::class)
    generateComposePreviewRobolectricTests {
        enable = true
        packages = listOf("io.github.barqallayl.burkan")
        includePrivatePreviews = true
        robolectricConfig = mapOf(
            "sdk" to "[34]",
            "qualifiers" to "\"w411dp-h914dp-xxhdpi\"",
            "application" to "io.github.barqallayl.burkan.RoborazziTestApplication::class",
        )
    }
    // Committed baselines. Left in build/, a clean would wipe them and verify would compare against nothing.
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}

// The open-source licences screen reads R.raw.aboutlibraries, generated here at build time from the resolved
// dependencies. Nothing is fetched from the network, at build time or at run time.
aboutLibraries {
    offlineMode = true
    collect {
        // libadb-android's published metadata calls its licence "Other". It is Apache-2.0 or GPL-3.0-or-later, at the
        // user's choice, and Burkan takes it under Apache-2.0; aboutlibraries/licenses/ names it, under the hash the plugin gives that entry.
        configPath = file("aboutlibraries")
    }
    library {
        // One entry for each project rather than one for each artifact: artifacts of the same group and licence
        // are merged.
        duplicationMode = DuplicateMode.MERGE
        duplicationRule = DuplicateRule.GROUP
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.aboutlibraries.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.arrow.core)
    implementation(libs.bouncycastle.pkix)
    implementation(libs.conscrypt.android)
    implementation(libs.kermit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ksafe)
    implementation(libs.libadb.android)
    implementation(libs.material.kolor)
    implementation(libs.metrox.android)
    implementation(libs.metrox.viewmodel.compose)
    implementation(libs.orbit.compose)
    implementation(libs.orbit.viewmodel)
    implementation(libs.tabler.icons.outline)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.composable.preview.scanner.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.orbit.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.compose.preview.scanner.support)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
