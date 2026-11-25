import com.android.build.api.variant.FilterConfiguration
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.powerassert.gradle.PowerAssertCompilationFilter
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.inject.Inject

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
    compilationFilter = PowerAssertCompilationFilter { it.name.endsWith("UnitTest") }
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

// Releasing. A version is normally released by merging the release pull request that release-please keeps open
// (release-please-config.json): its merge tags the commit and makes a draft release, and .github/workflows/release.yml
// then runs `:app:githubRelease`, which uploads the APKs to the draft and publishes it. F-Droid picks a new version
// up from the same tag, so a tag is a release: nothing here runs without being asked to. `:app:checkRelease` is the
// first half alone: it says what stands in the way and changes nothing.
//
// Two ways remain for when that one cannot be used, each with .release-please-manifest.json raised with the version:
// `./gradlew :app:githubRelease` on a computer tags the commit checked out, builds it, pushes the branch and the tag
// and publishes the release; and a signed tag pushed by hand makes release.yml run the same task, which then neither
// makes nor pushes the tag, only checks that it is this commit, on main.

/** Where a release comes from, which decides who makes the tag and the GitHub release. */
enum class ReleaseSource {
    /** A computer: the task makes and pushes the tag, and creates the release. */
    LOCAL,

    /** GitHub Actions, from a tag pushed by hand: the task creates the release. */
    TAG,

    /** GitHub Actions, after a release pull request was merged: release-please made the tag and a draft release. */
    RELEASE_PLEASE,
}

/** What the two release tasks share: the version, and running `git` and `gh` from the repository's root. */
abstract class ReleaseTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    /** Each processor's APK and its version code. */
    @get:Input
    abstract val abiVersionCodes: MapProperty<String, Int>

    @get:Internal
    abstract val repository: DirectoryProperty

    @get:Input
    abstract val source: Property<ReleaseSource>

    @get:Internal
    val tag: String get() = "v${versionName.get()}"

    /**
     * The version's changelog under each processor's version code, which is where F-Droid looks for it. They say the
     * same, and the GitHub release takes the first.
     */
    @get:Internal
    val changelogs: List<File>
        get() = abiVersionCodes.get().values.sorted().map { code ->
            repository.file("fastlane/metadata/android/en-US/changelogs/$code.txt").get().asFile
        }

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
            val missing = changelogs.filterNot { it.exists() }
            if (missing.isNotEmpty()) {
                add("There is no changelog for version code ${missing.joinToString { it.nameWithoutExtension }}.")
            } else if (changelogs.map { it.readText() }.distinct().size > 1) {
                val names = changelogs.joinToString { it.name }
                add("The changelogs $names differ, though they are for the same version.")
            } else if (changelogs.first().readText().trim().length > CHANGELOG_LIMIT) {
                add("The changelog is longer than the $CHANGELOG_LIMIT characters F-Droid shows.")
            }
            // release-please starts its next version from the manifest, so a release made any other way raises it too.
            val manifest = repository.file(".release-please-manifest.json").get().asFile
            val released = (JsonSlurper().parse(manifest) as Map<*, *>)["."]
            if (released != versionName.get()) {
                add("${manifest.name} says $released, not ${versionName.get()}.")
            }
            // F-Droid takes a tag only if its versionCode is higher than the last one's.
            val previous = output("git", "describe", "--tags", "--abbrev=0", "--match", "v*", "--exclude", tag, "HEAD")
            if (previous != null) {
                val previousCode = output("git", "show", "$previous:app/build.gradle.kts")
                    ?.let { Regex("""versionCode = (\d+)""").find(it)?.groupValues?.get(1)?.toInt() }
                if (previousCode != null && versionCode.get() <= previousCode) {
                    add("versionCode ${versionCode.get()} is not higher than $previous's, $previousCode.")
                }
            }
            if (output("git", "status", "--porcelain") != "") add("There are changes that are not committed.")
            if (output("gh", "auth", "status") == null) add("The GitHub CLI is not installed or not signed in.")
            val head = output("git", "rev-parse", "HEAD")
            val tagged = output("git", "rev-parse", "--quiet", "--verify", "refs/tags/$tag^{commit}")
            if (source.get() != ReleaseSource.LOCAL) {
                // A tag checked out, not a branch: it must be this version's, and on main.
                if (tagged != head) add("The commit checked out is not $tag, the tag of version ${versionName.get()}.")
                if (output("git", "merge-base", "--is-ancestor", "HEAD", "origin/main") == null) {
                    add("$tag is not on main.")
                }
            } else {
                if (output(
                        "git",
                        "rev-parse",
                        "--abbrev-ref",
                        "HEAD",
                    ) != "main"
                ) add("The branch checked out is not main.")
                if (tagged != null && tagged != head) {
                    add("$tag already exists on another commit. Raise versionName and versionCode, or check that commit out.")
                }
            }
            val draft = output("gh", "release", "view", tag, "--json", "isDraft", "--jq", ".isDraft")
            if (source.get() == ReleaseSource.RELEASE_PLEASE) {
                // release-please made it a draft, which this release fills; a published one is out already.
                if (draft == null) add("release-please made no GitHub release for $tag.")
                if (draft == "false") add("$tag is already released on GitHub.")
            } else if (draft == "true") {
                add("$tag has a draft release on GitHub. Run release.yml again for it, or delete it to release here.")
            } else if (draft != null) {
                add("$tag is already released on GitHub.")
            }
        }
        if (problems.isNotEmpty()) {
            throw GradleException("Not releasing $tag:\n" + problems.joinToString(separator = "\n") { "  - $it" })
        }
        logger.lifecycle("$tag is ready to release.")
    }

    private companion object {
        /** The most F-Droid shows of a version's changelog. */
        const val CHANGELOG_LIMIT = 500
    }
}

abstract class GithubRelease : ReleaseTask() {
    /** Where the release build leaves its APKs, as app-<processor>-release.apk and app-universal-release.apk. */
    @get:InputDirectory
    abstract val apks: DirectoryProperty

    @get:OutputDirectory
    abstract val namedApks: DirectoryProperty

    @TaskAction
    fun release() {
        val name = "Burkan ${versionName.get()}"
        val named = namedApks.get().asFile.apply { deleteRecursively() }
        // Under the names F-Droid's recipe looks for: Burkan-<version>-<processor>.apk.
        val published = (abiVersionCodes.get().keys + "universal").map { abi ->
            val apk = apks.file("app-$abi-release.apk").get().asFile
            if (!apk.exists()) throw GradleException("The release build made no APK for $abi: ${apk.path}")
            apk.copyTo(named.resolve("Burkan-${versionName.get()}-$abi.apk"))
        }
        val files = published.map { it.path }.toTypedArray()
        when (source.get()) {
            ReleaseSource.RELEASE_PLEASE -> {
                // The draft has release-please's notes. Replacing an APK is safe while it is a draft, and lets a
                // failed run be run again; once published, nothing in it changes.
                run("gh", "release", "upload", tag, *files, "--clobber")
                run("gh", "release", "edit", tag, "--title", name, "--draft=false", "--latest")
            }
            ReleaseSource.TAG, ReleaseSource.LOCAL -> {
                // From a pushed tag, the tag is what started the run; from a computer, it is made and pushed here.
                if (source.get() == ReleaseSource.LOCAL) {
                    if (output("git", "rev-parse", "--quiet", "--verify", "refs/tags/$tag") == null) {
                        run("git", "tag", "--sign", tag, "--message", name)
                    }
                    run("git", "push", "origin", "main", tag)
                }
                run(
                    "gh", "release", "create", tag, *files,
                    "--verify-tag", "--title", name, "--notes-file", changelogs.first().path,
                )
            }
        }
    }
}

val releaseVersionName = android.defaultConfig.versionName!!
val releaseVersionCode = android.defaultConfig.versionCode!!
val releaseAbiVersionCodes = abiDigits.mapValues { (_, digit) -> releaseVersionCode * 10 + digit }
// release.yml names the source; a run on GitHub Actions without one is from a tag pushed by hand.
val releaseSource = providers.environmentVariable("RELEASE_SOURCE")
    .map { ReleaseSource.valueOf(it.uppercase().replace('-', '_')) }
    .orElse(
        providers.environmentVariable("GITHUB_ACTIONS")
            .map { if (it == "true") ReleaseSource.TAG else ReleaseSource.LOCAL },
    )
    .orElse(ReleaseSource.LOCAL)

val checkRelease = tasks.register<CheckRelease>("checkRelease") {
    group = "release"
    description = "Says what stands in the way of releasing this commit. Changes nothing."
    versionName = releaseVersionName
    versionCode = releaseVersionCode
    abiVersionCodes = releaseAbiVersionCodes
    repository = rootProject.layout.projectDirectory
    source = releaseSource
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
    description = "Builds the release of this commit and publishes it on GitHub, tagging it first when run here."
    dependsOn(checkRelease, "assembleRelease")
    versionName = releaseVersionName
    versionCode = releaseVersionCode
    abiVersionCodes = releaseAbiVersionCodes
    repository = rootProject.layout.projectDirectory
    source = releaseSource
    apks = layout.buildDirectory.dir("outputs/apk/release")
    namedApks = layout.buildDirectory.dir("release")
    outputs.upToDateWhen { false }
}
