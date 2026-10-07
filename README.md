# Burkan

Burkan keeps your Galaxy S23 running on Vulkan, the faster way for the phone to draw its screen. You set it up
once, and it takes care of the rest, even after you restart the phone. No computer needed.

Normally, switching a phone to Vulkan means plugging it into a computer and typing commands, and doing that
again every time the phone restarts, because a restart puts everything back. Burkan does the same job from
inside the phone, by itself.

> **Burkan is new.** It has been tried on a Galaxy S23 with One UI 8.5 and works there. The list of what has and
> has not been checked is in [`docs/testing.md`](docs/testing.md).

## How it looks

Burkan comes in two looks. On a Samsung phone it starts in **One UI**, so it feels like one of the phone's own
apps. You can switch to **Material** in Settings whenever you like.

**One UI**

| Setup | Home | During a run | Settings | Log |
|---|---|---|---|---|
| <img src="app/src/test/screenshots/io.github.barqallayl.burkan.feature.setup.ui.SetupScreenKt.SetupPairOneUiDarkPreview.WITH_BACKGROUND.png" width="160" alt="Setup in the One UI look, at the pairing step"> | <img src="app/src/test/screenshots/io.github.barqallayl.burkan.feature.status.ui.HomeScreenKt.HomeActiveOneUiDarkPreview.WITH_BACKGROUND.png" width="160" alt="Home in the One UI look, showing Vulkan is active"> | <img src="app/src/test/screenshots/io.github.barqallayl.burkan.feature.status.ui.HomeScreenKt.HomeRunningOneUiDarkPreview.WITH_BACKGROUND.png" width="160" alt="Home in the One UI look while Vulkan is being applied"> | <img src="app/src/test/screenshots/io.github.barqallayl.burkan.feature.settings.ui.SettingsScreenKt.SettingsOneUiDarkPreview.WITH_BACKGROUND.png" width="160" alt="Settings in the One UI look"> | <img src="app/src/test/screenshots/io.github.barqallayl.burkan.feature.log.ui.LogScreenKt.LogExpandedOneUiDarkPreview.WITH_BACKGROUND.png" width="160" alt="The log in the One UI look, with one run opened"> |

**Material**

| Setup | Home | During a run | Settings | Log |
|---|---|---|---|---|
| <img src="app/src/test/screenshots/io.github.barqallayl.burkan.feature.setup.ui.SetupScreenKt.SetupPairDarkPreview.WITH_BACKGROUND.png" width="160" alt="Setup in the Material look, at the pairing step"> | <img src="app/src/test/screenshots/io.github.barqallayl.burkan.feature.status.ui.HomeScreenKt.HomeActiveDarkPreview.WITH_BACKGROUND.png" width="160" alt="Home in the Material look, showing Vulkan is active"> | <img src="app/src/test/screenshots/io.github.barqallayl.burkan.feature.status.ui.HomeScreenKt.HomeRunningDarkPreview.WITH_BACKGROUND.png" width="160" alt="Home in the Material look while Vulkan is being applied"> | <img src="app/src/test/screenshots/io.github.barqallayl.burkan.feature.settings.ui.SettingsScreenKt.SettingsDarkPreview.WITH_BACKGROUND.png" width="160" alt="Settings in the Material look"> | <img src="app/src/test/screenshots/io.github.barqallayl.burkan.feature.log.ui.LogScreenKt.LogExpandedDarkPreview.WITH_BACKGROUND.png" width="160" alt="The log in the Material look, with one run opened"> |

These pictures are made from the app itself with sample data. Both looks also have a light theme.

## What Burkan does for you

- **Turns Vulkan back on after every restart.** As soon as the phone is on Wi-Fi, Burkan applies Vulkan without
  you opening it. If there is no Wi-Fi yet, it waits until there is.
- **Tells you what is really going on.** Home shows whether Vulkan is active, and for which parts of the phone.
  If it cannot check, it tells you why in plain words, for example that the phone is not on Wi-Fi.
- **Lets you do it yourself, too.** *Apply now* switches the phone to Vulkan on the spot. *Restart all apps* goes
  further and reopens your apps so they pick it up as well. You can choose apps that should never be restarted.
- **Shows its work.** While it is busy you see each step ticked off, and a log keeps the last 50 runs in case
  you want to look back or share one.
- **Fits your taste.** Pick the look, a light or dark theme, and a color.

## What you need

- A Galaxy S23, S23+ or S23 Ultra. Burkan will open on other phones, with a warning, but it is not tested there.
- Android 13 or newer.
- Wi-Fi. Burkan works through a feature of the phone called wireless debugging, and Android only allows that
  while the phone is connected to a Wi-Fi network.

## Getting started

Install Burkan and open it. It walks you through seven short steps, one at a time. Each step tells you what to
do, checks that it worked, and waits for you to tap **Done** before moving on.

1. **Allow notifications.** You will type a code into a notification in step 4, and Burkan uses notifications to
   tell you if something went wrong.
2. **Turn on Developer options.** In the phone's Settings, open *About phone*, then *Software information*, and
   tap *Build number* seven times.
3. **Turn on Wireless debugging.** You will find it in Developer options. Tick *Always allow on this network*
   when the phone asks.
4. **Pair.** In Wireless debugging, tap *Pair device with pairing code*. Then pull down the notification shade
   and type the six-digit code into Burkan's notification. Stay on the pairing screen while you do this, because
   the code disappears if you leave it.
5. **Connect.** Burkan does this one for you.
6. **Permission.** Burkan does this one too. It gives itself what it needs to switch wireless debugging on and
   off without asking you each time.
7. **Battery.** Let Burkan run without battery limits, so it is not held back after a restart. You can skip
   this step.

That's it. From now on Burkan looks after things on its own, and Home shows you how it is going. If you ever want
to start over, *Redo setup* is in Settings.

## Your privacy

Burkan only ever talks to your own phone. It does not go online, does not collect anything about you, and has
no ads or tracking. The key it uses to talk to the phone is stored safely on the phone and is never backed up.

## Good to know

- Samsung and Google do not officially support forcing Vulkan. Most things work well, but if something looks odd,
  restarting the phone puts everything back to normal. Burkan then applies Vulkan again by itself, unless you
  switch off *Apply after restart* in Settings first.
- Burkan needs no root and no extra apps.

## For developers

You need the Android SDK (platform 37). Gradle fetches the right JDK by itself.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug
./gradlew :app:assembleRelease
```

The release build is signed when a `keystore.properties` file sits next to `settings.gradle.kts`, and left
unsigned otherwise:

```properties
storeFile=/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Never commit that file or the keystore; both are in `.gitignore`.

More detail lives in these documents:

- [`docs/architecture.md`](docs/architecture.md): how the app is put together.
- [`docs/device-notes.md`](docs/device-notes.md): what Burkan does on the phone, and what was measured on a real
  S23.
- [`docs/testing.md`](docs/testing.md): what has been checked on a phone and what has not.
- [`CONTRIBUTING.md`](CONTRIBUTING.md): building, testing and the code conventions.

## Thanks

The idea comes from the community scripts in
[s23-vulkan-support](https://github.com/Ameen-Sha-Cheerangan/s23-vulkan-support), which do the same job from a
computer. Burkan brings it onto the phone.

## License

Copyright 2026 Mohammed Barq AL Layl.

Burkan is licensed under the [Apache License, Version 2.0](LICENSE). It is provided "as is", without warranties
or conditions of any kind; see the license for details.

The libraries it is built with keep their own licenses, listed in the app under Settings › Open-source
licences. `libadb-android` is used under its Apache-2.0 option, and its pairing helper `spake2-java` under the
LGPL-3.0.
