# Publishing on F-Droid

What is in place for [F-Droid](https://f-droid.org), what is still to do, and where its build may stumble. F-Droid
builds the app itself from this repository and signs it with its own key, so everything it needs has to be here.

## In place

- **The store listing**, in `fastlane/metadata/android/en-US/`: the title, a short and a full description, the
  changelog for version code 1, the icon and six screenshots. F-Droid reads these straight from the repository.
  The screenshots are copies of the app's own screenshot tests, so they show sample data only.
- **No dependency list in the APK.** `dependenciesInfo` is switched off in `app/build.gradle.kts`, as F-Droid asks.
- **The build recipe**, in [`f-droid/io.github.barqallayl.burkan.yml`](f-droid/io.github.barqallayl.burkan.yml),
  in the form F-Droid's own formatter writes it. It carries no comments, because F-Droid's files do not keep them;
  what each part is for is under "The recipe, line by line" below.
- **F-Droid's own checks pass**, run with its tools (`fdroidserver` 2.4.5) against this repository: `fdroid lint`
  has no complaint, `fdroid rewritemeta` changes nothing, and `fdroid scanner` finds 0 problems in the source.
- **The recipe asks for a reproducible build.** F-Droid builds the app, checks that its build is the same as
  the APK published on GitHub, and then ships that APK, signed with this project's own key.
- **The merge request's pipeline passes.** F-Droid's build server built 1.0.0 from the tagged commit and found
  its APK identical to the published one.
- **Nothing F-Droid forbids.** The licence is Apache-2.0, every library is open source, and the libraries come from
  Maven Central, Google's repository and JitPack, all of which F-Droid trusts. There is no Google Play Services,
  Firebase, advertising or analytics, and the licence list is generated without the network.

## Still to do

1. **Make the repository public.** F-Droid cannot build from, or review, a private one.
2. **Nothing more for the tag.** The release is tagged `v1.0.0` and published on GitHub, and the recipe names
   the commit the tag points at by its full hash, which F-Droid asks for in place of the tag's name. Later
   versions are picked up from their tags by themselves, as long as each raises `versionCode` and is tagged `v`
   and the version; `./gradlew :app:githubRelease` does the tagging.
3. **Add a changelog for each new version**, as `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
4. **Wait for the merge request** on fdroiddata to be reviewed and merged. Once it is, the app appears in a day
   or two.

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

- `commit`: the full hash of the commit that the tag `v1.0.0` points at. F-Droid wants the hash, not the tag's
  name.
- `sudo`: installs JDK 25 from Debian's packages and makes it the default, since the project builds with it.
- `gradle: yes`: build the release with Gradle, with no product flavour.
- `rm`: removes the file that tells the Gradle daemon which JDK to run on, and where to fetch it from.
- `prebuild`: takes out the plugin that would fetch a JDK. Both this and `rm` are written the way F-Droid's
  other recipes write them.
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

## From the next release

- **A smaller APK.** About 7 MB of the 15 MB of 1.0.0 is native code for processors no Galaxy S23 has (x86,
  x86_64 and 32-bit ARM). The build is now limited to 64-bit ARM, which F-Droid's checklist asks about, so the
  release after 1.0.0 is about half the size.
