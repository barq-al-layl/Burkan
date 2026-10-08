import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.inject.Inject

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
    // Nothing here is compiled with the NDK, but the build uses it to strip the debug symbols from the native
    // libraries the app's dependencies bring. F-Droid rebuilds each release and ships it only if its build is
    // identical, so a release has to be made with this very version: its recipe names the same one, as r28c.
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "io.github.barqallayl.burkan"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        // Every phone the app is for is 64-bit ARM. The libraries' copies for other processors were half the APK.
        ndk {
            abiFilters += "arm64-v8a"
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

// Releasing. `./gradlew :app:githubRelease` tags the commit that is checked out as v<versionName>, builds the
// release from it, pushes the branch and the tag, and publishes a GitHub release with the APK and the version's
// changelog. F-Droid picks a new version up from the same tag, so a tag is a release: nothing here runs without
// being asked to. `:app:checkRelease` is the first half alone: it says what stands in the way and changes nothing.

/** What the two release tasks share: the version, and running `git` and `gh` from the repository's root. */
abstract class ReleaseTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Internal
    abstract val repository: DirectoryProperty

    @get:Internal
    val tag: String get() = "v${versionName.get()}"

    @get:Internal
    val changelog: File
        get() = repository.file("fastlane/metadata/android/en-US/changelogs/${versionCode.get()}.txt").get().asFile

    /** What the command printed, or null when it failed. */
    protected fun output(vararg command: String): String? {
        val printed = ByteArrayOutputStream()
        val result = execOperations.exec {
            workingDir = repository.get().asFile
            commandLine(*command)
            standardOutput = printed
            errorOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }
        return printed.toString().trim().takeIf { result.exitValue == 0 }
    }

    /** Runs the command, showing what it prints, and stops the build if it fails. */
    protected fun run(vararg command: String) {
        execOperations.exec {
            workingDir = repository.get().asFile
            commandLine(*command)
        }
    }
}

/** Everything that must hold before a release is made, checked before the long build and before anything is pushed. */
abstract class CheckRelease : ReleaseTask() {
    @get:Input
    abstract val hasReleaseKey: Property<Boolean>

    @get:Input
    abstract val ndkVersion: Property<String>

    @get:Internal
    abstract val sdk: DirectoryProperty

    @TaskAction
    fun check() {
        val problems = buildList {
            if (!hasReleaseKey.get()) add("keystore.properties is missing, so the release would not be signed.")
            if (!sdk.dir("ndk/${ndkVersion.get()}").get().asFile.isDirectory) {
                add("NDK ${ndkVersion.get()} is not installed, so the APK would not match the one F-Droid builds.")
            }
            if (!changelog.exists()) add("There is no changelog for version code ${versionCode.get()}: ${changelog.path}")
            if (output("git", "status", "--porcelain") != "") add("There are changes that are not committed.")
            if (output("git", "rev-parse", "--abbrev-ref", "HEAD") != "main") add("The branch checked out is not main.")
            if (output("gh", "auth", "status") == null) add("The GitHub CLI is not installed or not signed in.")
            val head = output("git", "rev-parse", "HEAD")
            val tagged = output("git", "rev-parse", "--quiet", "--verify", "refs/tags/$tag^{commit}")
            if (tagged != null && tagged != head) {
                add("$tag already exists on another commit. Raise versionName and versionCode, or check that commit out.")
            }
            if (output("gh", "release", "view", tag) != null) add("$tag is already released on GitHub.")
        }
        if (problems.isNotEmpty()) {
            throw GradleException("Not releasing $tag:\n" + problems.joinToString(separator = "\n") { "  - $it" })
        }
        logger.lifecycle("$tag is ready to release.")
    }
}

abstract class GithubRelease : ReleaseTask() {
    @get:InputFile
    abstract val apk: RegularFileProperty

    @get:OutputFile
    abstract val namedApk: RegularFileProperty

    @TaskAction
    fun release() {
        val name = "Burkan ${versionName.get()}"
        apk.get().asFile.copyTo(namedApk.get().asFile, overwrite = true)
        if (output("git", "rev-parse", "--quiet", "--verify", "refs/tags/$tag") == null) {
            run("git", "tag", "--sign", tag, "--message", name)
        }
        run("git", "push", "origin", "main", tag)
        run(
            "gh", "release", "create", tag, namedApk.get().asFile.path,
            "--verify-tag", "--title", name, "--notes-file", changelog.path,
        )
    }
}

val releaseVersionName = android.defaultConfig.versionName!!
val releaseVersionCode = android.defaultConfig.versionCode!!

val checkRelease by tasks.registering(CheckRelease::class) {
    group = "release"
    description = "Says what stands in the way of releasing this commit. Changes nothing."
    versionName = releaseVersionName
    versionCode = releaseVersionCode
    repository = rootProject.layout.projectDirectory
    hasReleaseKey = keystoreProperties != null
    ndkVersion = android.ndkVersion
    sdk = androidComponents.sdkComponents.sdkDirectory
    // It reads the state of the repository, which Gradle cannot see: never up to date.
    outputs.upToDateWhen { false }
}

// The checks come before the build, so a release that cannot go out fails in seconds.
tasks.configureEach { if (name == "assembleRelease") mustRunAfter(checkRelease) }

tasks.register<GithubRelease>("githubRelease") {
    group = "release"
    description = "Tags this commit, builds the release, pushes both and publishes it on GitHub with its changelog."
    dependsOn(checkRelease, "assembleRelease")
    versionName = releaseVersionName
    versionCode = releaseVersionCode
    repository = rootProject.layout.projectDirectory
    apk = layout.buildDirectory.file("outputs/apk/release/app-release.apk")
    namedApk = layout.buildDirectory.file("release/Burkan-$releaseVersionName.apk")
    outputs.upToDateWhen { false }
}
