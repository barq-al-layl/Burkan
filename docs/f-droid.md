# Publishing on F-Droid

How Burkan is published on [F-Droid](https://f-droid.org), what a new version needs to get there, and where its
build may stumble. F-Droid builds the app itself from this repository, so everything it needs has to be here.

## In place

- **The store listing**, in `fastlane/metadata/android/en-US/`: the title, a short and a full description, a
  changelog for each version code, the icon and six screenshots. F-Droid reads these straight from the repository.
  The screenshots are copies of the app's own screenshot tests, so they show sample data only.
- **No dependency list in the APK.** `dependenciesInfo` is switched off in `app/build.gradle.kts`, as F-Droid asks.
- **The build recipe**, merged into F-Droid's
  [fdroiddata](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/io.github.barqallayl.burkan.yml) by
  [merge request 51717](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/51717). The copy in
  [`f-droid/io.github.barqallayl.burkan.yml`](f-droid/io.github.barqallayl.burkan.yml) is the recipe as merged,
  for reference. From now on the one in fdroiddata is the one that counts: F-Droid's bot adds each new version to
  it, and this copy is not kept up with it. It carries no comments, because F-Droid's files do not keep them; what
  each part is for is under "The recipe, line by line" below.
- **F-Droid's own checks pass**, run with its tools (`fdroidserver` 2.4.5) against this repository: `fdroid lint`
  has no complaint, `fdroid rewritemeta` changes nothing, and `fdroid scanner` finds 0 problems in the source.
- **The recipe asks for a reproducible build.** F-Droid builds the app, checks that its build is the same as
  the APK published on GitHub, and then ships that APK, signed with this project's own key.
- **1.1.0 built on F-Droid's build server**, in the build cycle that started on 19 October 2026, where it is
  listed as `io.github.barqallayl.burkan:2`. With `Binaries` in the recipe, a build whose APK differs from the
  published one fails, so a successful one is a reproduced one. 1.1.0 was also the first release with
  `gradle/verification-metadata.xml`, a pinned checksum for the Gradle distribution, Kotlin 2.4.21 and Material 3
  1.5.0-beta01; the build server took all of them.
- **The package page**, <https://f-droid.org/packages/io.github.barqallayl.burkan/>, answers 404 until the build
  cycle that built the app has finished and its builds have been signed and published. That is usually a day or
  two after the build itself, and nothing here or in the recipe hurries it.
- **Nothing F-Droid forbids.** The licence is Apache-2.0, every library is open source, and the libraries come from
  Maven Central, Google's repository and JitPack, all of which F-Droid trusts. There is no Google Play Services,
  Firebase, advertising or analytics, and the licence list is generated without the network.

## Releasing a new version

F-Droid finds new versions by itself. Its bot checks the repository's tags, and for a tag shaped like `v1.2.3`
reads `versionName` and `versionCode` from `app/build.gradle.kts` at that tag, adds a build entry for it to the
recipe in fdroiddata and updates `CurrentVersion`. Nothing in fdroiddata is edited by hand for a release. What a
release needs here:

1. **A higher `versionCode`**, and the new `versionName`, in `app/build.gradle.kts`. A tag whose `versionCode` is
   not higher than the last one is not picked up.
2. **A changelog**, as `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`. F-Droid shows it with the
   version.
3. **The release itself, with `./gradlew :app:githubRelease`** from a clean `main`. It tags `v<versionName>` and
   publishes the APK under the name the recipe's `Binaries` expects. F-Droid compares its own build with that
   APK, so neither the tag nor the APK may change after the release.
4. **Watching the build.** Once the bot has picked up the tag, the version shows on
   <https://monitor.f-droid.org/builds>, with its log if the build fails, and it reaches the package page when
   its build cycle is published. The bot's runs are on the same site, which is where to look if a tag has not
   been picked up within a few days.

The recipe needs a change by hand only when the build itself must change: another JDK, another NDK, a new step
before the build. That is a merge request to fdroiddata, changing the new version's build entry once the bot has
added it.

### What 1.1.1 brings that F-Droid has not built yet

Each of these builds on a Mac and has not yet been through F-Droid's build server. If the next build fails, or
its APK differs from the published one, look here first.

- **Android 17's second minor SDK.** The app compiles against SDK 37 minor 2 (`release(37) { minorApiLevel = 2 }`
  in `app/build.gradle.kts`), so the build server has to install that platform; 1.1.0 needed only platform 37.
- **Gradle 9.8.2**, with its distribution's checksum pinned in `gradle/wrapper/gradle-wrapper.properties`.
- **kotlinx.serialization 1.12.0.** Nothing to check for it in `gradle/verification-metadata.xml`, which pins only
  the libraries from JitPack.
- **Conscrypt 2.8.0**, whose `libconscrypt_jni.so` is the largest native library in the APK; it is stripped with
  the same NDK as before.
- **A new permission, `ACCESS_LOCAL_NETWORK`.** F-Droid lists an app's permissions on its page. It is the Nearby
  devices permission Android 17 asks for before an app looks for services on the network, and the app uses it
  only to find the phone's own wireless debugging.

## What the build needed

F-Droid's build server (Debian 13) built the app at the first attempt: it had the JDK 25 package, Android
platform 37 and the Gradle version the wrapper names. Its scanner removes the signing configuration from
`app/build.gradle.kts`, as it does for every app, and the Gradle wrapper, which it replaces with its own.

- **The NDK decides whether the build is reproducible.** The first attempt built, but its APK differed from the
  published one in a single library, `libdatastore_shared_counter.so`. A release build strips the debug symbols
  from native libraries, and needs the NDK to do it: the Mac that made the release had one, the build server
  did not, so the server packaged that library as it came. With `ndk: r28c` in the recipe the two builds are
  identical. `app/build.gradle.kts` now names the same version, and `:app:checkRelease` refuses to release
  without it installed.
- **The JDK.** The project builds with JDK 25 and lets Gradle fetch one, which F-Droid does not allow. The recipe
  installs JDK 25 from Debian and removes what would fetch one.
- **A new version must reproduce too.** A release whose APK F-Droid cannot rebuild identically is not published
  there until it can. Changing the NDK, the JDK or the build tools is what to look at first if one does not.

## The recipe, line by line

- `commit`: the full hash of the commit the version's tag points at, `v1.1.0` in the copy here. F-Droid wants
  the hash, not the tag's name, and its bot writes it for each new version.
- `sudo`: installs JDK 25 from Debian's packages and makes it the default, since the project builds with it.
- `gradle: yes`: build the release with Gradle, with no product flavour.
- `prebuild`: takes out the plugin that would fetch a JDK, written the way F-Droid's other recipes write it.
  The recipe once also removed `gradle/gradle-daemon-jvm.properties`; a reviewer had that line taken out, and
  the build passes without it.
- `ndk`: installs the NDK the release was built with, so native libraries are stripped the same way.
- `Binaries` and `AllowedAPKSigningKeys`: where the APK of each release is published, and the fingerprint of
  the certificate it must be signed with. Together they ask for the reproducible build. The task
  `:app:githubRelease` publishes each APK under the name `Binaries` expects.
- `AutoUpdateMode` and `UpdateCheckMode`: a new version is picked up from a tag shaped like `v1.2.3`, with the
  version name and code read from `app/build.gradle.kts` at that tag.

## The same app from two places

With the reproducible build, F-Droid ships the same APK as this repository's releases, signed with the same key,
so an install from one can be updated from the other. That holds only while every release reproduces: a version
F-Droid cannot rebuild identically is not published there until it can.

If the reproducible build is dropped, F-Droid signs with its own key instead, and Android will not update one
with the other: someone who installed from one has to uninstall before installing from the other, and sets the
app up again. That choice cannot be reversed later.

## What the review asked for

A static review of the merge request, on 10 October 2026, found one thing to fix and one to consider. Both are
answered by 1.1.0.

- **Three permissions nobody asked for.** The APK of 1.0.0 declares `READ_PHONE_STATE`, `READ_EXTERNAL_STORAGE`
  and `WRITE_EXTERNAL_STORAGE`. The adb library's manifest names no SDK version, so the manifest merger takes
  it for a library from before Android 1.6 and adds the permissions every app had then. The app's manifest now
  removes the three, and `app/build/outputs/logs/manifest-merger-release-report.txt` is where to look if they
  come back.
- **A smaller APK.** About 7 MB of the 15 MB of 1.0.0 is native code for processors no Galaxy S23 has (x86,
  x86_64 and 32-bit ARM). The build is limited to 64-bit ARM from 1.1.0.
