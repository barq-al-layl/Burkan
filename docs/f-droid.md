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
  the APK published on GitHub, and then ships that APK, signed with this project's own key. Built again from the
  tag on the Mac that made the release, every file in the APK but one came out identical; the odd one records the
  git commit, and differed only because that copy was not a git checkout.
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
4. **Try the whole build.** Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata), copy the recipe to
   `metadata/io.github.barqallayl.burkan.yml`, and run `fdroid build -v -l io.github.barqallayl.burkan` on
   F-Droid's build server image. Their merge request runs the same build, so this step can also be left to it.
5. **Open a merge request** on fdroiddata with the recipe. Once it is merged the app appears in a day or two.

## Where the build may stumble

None of this has been tried on F-Droid's build server. What has been tried, on a Mac with a JDK 25 already
installed: F-Droid's scanner checked the source out and removed the signing configuration from
`app/build.gradle.kts`, as it does for every app. From a copy of the tagged commit with the recipe's `rm` and
`prebuild` applied, `assembleRelease` built `app-release-unsigned.apk` without fetching a JDK. That APK holds no
Google Play Services or Firebase code, and the only binary file in the repository is the Gradle wrapper, which
F-Droid removes and replaces with its own.

- **The build may not come out the same on Linux.** The comparison above was Mac against Mac. If F-Droid's
  build differs from the published APK, its pipeline says so before anything is merged; the cause is then fixed
  here, or the two reproducible-build lines come out of the recipe and F-Droid signs with its own key.
- **The JDK.** The project builds with JDK 25 and lets Gradle fetch one, which F-Droid does not allow. The recipe
  installs JDK 25 from Debian and removes what would fetch one. Debian 13 (trixie) has the package
  `openjdk-25-jdk-headless`; Debian 12 does not. If the build server runs an older Debian, the toolchain in
  `app/build.gradle.kts` and `gradle/gradle-daemon-jvm.properties` has to come down to a version it does have.
- **The Android platform.** The app compiles against platform 37. The build server needs that platform as an
  official release.
- **The Gradle version.** F-Droid runs the Gradle version named in the wrapper from its own verified copies. A
  version newer than its list has to be added there first.

## The recipe, line by line

- `commit`: the full hash of the commit that the tag `v1.0.0` points at. F-Droid wants the hash, not the tag's
  name.
- `sudo`: installs JDK 25 from Debian's packages and makes it the default, since the project builds with it.
- `gradle: yes`: build the release with Gradle, with no product flavour.
- `rm`: removes the file that tells the Gradle daemon which JDK to run on, and where to fetch it from.
- `prebuild`: takes out the plugin that would fetch a JDK. Both this and `rm` are written the way F-Droid's
  other recipes write them.
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

## For a later release

- **A smaller APK.** About 7 MB of the 15 MB is native code for processors no Galaxy S23 has (x86, x86_64 and
  32-bit ARM). Limiting the build to 64-bit ARM would roughly halve it. F-Droid's checklist asks about this.
