import com.android.build.api.variant.FilterConfiguration
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.powerassert.gradle.PowerAssertCompilationFilter
import java.util.Properties

plugins {
    alias(libs.plugins.aboutLibraries.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.powerAssert)
    alias(libs.plugins.metro)
    alias(libs.plugins.roborazzi)
}

// Release signing comes from keystore.properties beside settings.gradle.kts, which is never committed, or on GitHub
// Actions from the environment, which the release workflow fills from its secrets. Without either the release build is
// left unsigned.
val keystoreProperties: Properties? = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
} ?: mapOf(
    "storeFile" to "RELEASE_KEYSTORE_FILE",
    "storePassword" to "RELEASE_KEYSTORE_PASSWORD",
    "keyAlias" to "RELEASE_KEY_ALIAS",
    "keyPassword" to "RELEASE_KEY_PASSWORD",
).mapValues { (_, variable) -> providers.environmentVariable(variable).orNull }
    // All four or none: a step that builds nothing signed is given none of them.
    .takeIf { values -> values.values.all { !it.isNullOrEmpty() } }
    ?.let { values -> Properties().apply { values.forEach { (key, value) -> setProperty(key, value) } } }

// One APK for each processor, so that a phone downloads only its own native code, and a universal APK with all of
// them for whoever does not know which they have. An APK's version code is the version's, times ten, plus the digit
// its processor has here: version code 4 gives 41, 42 and 43, and 40 for the universal APK, which is the lowest so
// that a phone's own APK always installs over it. F-Droid builds the three and finds each by its version code.
val abiDigits = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86_64" to 3)

android {
    namespace = "io.github.barqallayl.burkan"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }
    // Nothing here is compiled with the NDK, but the build uses it to strip the debug symbols from the native
    // libraries the app's dependencies bring. F-Droid rebuilds each release and ships it only if its build is
    // identical, so a release has to be made with this very version: its recipe names the same one, as r28c.
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "io.github.barqallayl.burkan"
        minSdk = 33
        targetSdk = 37
        versionCode = 4
        versionName = "1.2.0" // x-release-please-version
    }

    splits {
        abi {
            isEnable = true
            reset()
            include(*abiDigits.keys.toTypedArray())
            isUniversalApk = true
        }
    }
    // The splits choose what each processor's APK holds; the universal APK would take every processor the libraries
    // bring, 32-bit x86 too, which no phone of this decade has.
    packaging {
        jniLibs {
            excludes += "lib/x86/**"
        }
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
        // The shell Shizuku starts for the app is a bound service in a process of its own: IShellService.aidl.
        aidl = true
        compose = true
    }
    // The build tools would add a list of the dependencies to the APK, encrypted with a key of Google's, for the
    // Play Store to read. Nobody else can read it, and F-Droid asks for it to be left out.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    lint {
        // MetroAppComponentFactory constructs activities, services and receivers, so they take constructor arguments.
        disable += "Instantiatable"
        // The splits include x86_64, but from a list lint cannot read, so it reports the ABI as missing.
        disable += "ChromeOsAbiSupport"
        // Only inside bcpkix and libadb: ADB's TLS to the phone's own adbd has no certificate authority to check
        // against, as adbd's certificate is its own. The app's own code has no trust manager.
        disable += "TrustAllX509TrustManager"
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

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters.find { it.filterType == FilterConfiguration.FilterType.ABI }?.identifier
            output.versionCode.set(output.versionCode.get()!! * 10 + (abiDigits[abi] ?: 0))
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

// A failed assertion prints the value of each part of its expression, where it said only "Expected value to be true".
// Test code only: the plugin rewrites each call into code that builds that message, which has no place in the app.
// The JVM tests are the compilations whose names end in "UnitTest" (debugUnitTest and the like); the plugin's own
// TESTS filter looks for one named "test", which an Android app does not have. Not "Test" alone: that also takes
// debugAndroidTest, whose classpath then asks for the plugin's runtime with no version, and lint, which resolves that
// classpath, fails. The list is the kotlin.test functions the suite calls that take a message last, which is what the
// plugin needs; a new one gets the diagram once it is here.
// PowerAssertTest fails if the plugin stops being applied.
powerAssert {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilationFilter = PowerAssertCompilationFilter { it.name.endsWith("UnitTest") }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    functions = listOf(
        "kotlin.assert",
        "kotlin.test.assertEquals",
        "kotlin.test.assertNotEquals",
        "kotlin.test.assertTrue",
        "kotlin.test.assertFalse",
        "kotlin.test.assertNull",
        "kotlin.test.assertNotNull",
        "kotlin.test.assertSame",
    )
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
        // Compares a little more loosely than the default, so that screenshots recorded on a Mac pass on Linux.
        testerQualifiedClassName = "io.github.barqallayl.burkan.BurkanPreviewTester"
        // The settings above reach that tester only when asked for.
        useScanOptionParametersInTester = true
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
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.tabler.icons.outline)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.composable.preview.scanner.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    // The plugin adds this itself, but with no version under AGP's built-in Kotlin, which then cannot resolve it.
    testImplementation(libs.kotlin.powerAssert.runtime)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.orbit.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.compose.preview.scanner.support)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Releasing: the checkRelease and githubRelease tasks, in release.gradle.kts beside this file, which reads what it
// needs of this build from here.
extra["release.versionName"] = android.defaultConfig.versionName!!
extra["release.versionCode"] = android.defaultConfig.versionCode!!
extra["release.abiDigits"] = abiDigits
extra["release.hasReleaseKey"] = keystoreProperties != null
extra["release.ndkVersion"] = android.ndkVersion
extra["release.sdk"] = androidComponents.sdkComponents.sdkDirectory
apply(from = "release.gradle.kts")
