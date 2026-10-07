# Publishing on F-Droid

What is in place for [F-Droid](https://f-droid.org), what is still to do, and where its build may stumble. F-Droid
builds the app itself from this repository and signs it with its own key, so everything it needs has to be here.

## In place

- **The store listing**, in `fastlane/metadata/android/en-US/`: the title, a short and a full description, the
  changelog for version code 1, the icon and six screenshots. F-Droid reads these straight from the repository.
  The screenshots are copies of the app's own screenshot tests, so they show sample data only.
- **No dependency list in the APK.** `dependenciesInfo` is switched off in `app/build.gradle.kts`, as F-Droid asks.
- **A draft build recipe**, in [`f-droid/io.github.barqallayl.burkan.yml`](f-droid/io.github.barqallayl.burkan.yml).
- **Nothing F-Droid forbids.** The licence is Apache-2.0, every library is open source, and the libraries come from
  Maven Central, Google's repository and JitPack, all of which F-Droid trusts. There is no Google Play Services,
  Firebase, advertising or analytics, and the licence list is generated without the network.

## Still to do

1. **Make the repository public.** F-Droid cannot build from, or review, a private one.
2. **Tag the release.** `git tag -s v1.0.0` on the release commit and push it. The recipe names that commit by
   its full hash, which F-Droid asks for in place of the tag's name: put the output of
   `git rev-parse 'v1.0.0^{commit}'` where the recipe says so. Later versions are picked up from their tags by
   themselves, as long as each raises `versionCode` and is tagged `v` and the version.
3. **Add a changelog for each new version**, as `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
4. **Try the recipe.** Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata), copy the recipe to
   `metadata/io.github.barqallayl.burkan.yml`, and run `fdroid lint io.github.barqallayl.burkan` and
   `fdroid build -v -l io.github.barqallayl.burkan`.
5. **Open a merge request** on fdroiddata with the recipe. Once it is merged the app appears in a day or two.

## Where the build may stumble

None of this has been tried on F-Droid's build server. What has been tried: on a clean copy of the project, with
no signing key, the recipe's three `prebuild` lines applied and a JDK 25 already installed, `assembleRelease`
built `app-release-unsigned.apk` without fetching a JDK. That APK holds no Google Play Services or Firebase
code, and the only binary file in the repository is the Gradle wrapper.

- **The JDK.** The project builds with JDK 25 and lets Gradle fetch one, which F-Droid does not allow. The recipe
  installs JDK 25 from Debian and removes what would fetch one. Debian 13 (trixie) has the package
  `openjdk-25-jdk-headless`; Debian 12 does not. If the build server runs an older Debian, the toolchain in
  `app/build.gradle.kts` and `gradle/gradle-daemon-jvm.properties` has to come down to a version it does have.
- **The Android platform.** The app compiles against platform 37. The build server needs that platform as an
  official release.
- **The Gradle version.** F-Droid runs the Gradle version named in the wrapper from its own verified copies. A
  version newer than its list has to be added there first.

## The same app from two places

An F-Droid build is signed with F-Droid's key and a build from this repository's releases with its own. Android
will not update one with the other: someone who installed from one has to uninstall before installing from the
other, and sets the app up again.
