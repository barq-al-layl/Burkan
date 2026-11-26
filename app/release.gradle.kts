import groovy.json.JsonSlurper
import java.io.ByteArrayOutputStream
import javax.inject.Inject

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

// What the app's build script hands over, before it applies this one.
val releaseVersionName = extra["release.versionName"] as String
val releaseVersionCode = extra["release.versionCode"] as Int
val releaseHasKey = extra["release.hasReleaseKey"] as Boolean
val releaseNdkVersion = extra["release.ndkVersion"] as String

@Suppress("UNCHECKED_CAST")
val releaseSdk = extra["release.sdk"] as Provider<Directory>

// Each processor's APK's version code, made as the comment on abiDigits in build.gradle.kts says.
val releaseAbiVersionCodes = (extra["release.abiDigits"] as Map<*, *>).entries
    .associate { (abi, digit) -> abi as String to releaseVersionCode * 10 + digit as Int }
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
    hasReleaseKey = releaseHasKey
    ndkVersion = releaseNdkVersion
    sdk = releaseSdk
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
