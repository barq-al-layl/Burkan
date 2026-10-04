# Contributing

Burkan is a small Android app with one unusual property: most of what it does can only be confirmed on a real
Galaxy S23. This page covers building and testing, the rules that protect the user's phone, and the code
conventions. How the app works is in [`docs/architecture.md`](docs/architecture.md).

## Building and testing

Requires the Android SDK (platform 37); `local.properties` or `ANDROID_HOME` points to it. Gradle resolves the
JDK 25 toolchain by itself.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest          # unit tests
./gradlew :app:verifyRoborazziDebug       # compare every preview against its baseline
./gradlew :app:recordRoborazziDebug       # re-record after an intentional UI change
./gradlew :app:lintDebug
```

Run the tests, the screenshot comparison and a debug build before opening a pull request.

Screenshot baselines are recorded on macOS. Another platform anti-aliases a few edges differently, so compare and
record on the same one.

## What only a phone can show

Pairing, connecting to the phone's own wireless debugging, the run after a restart, and anything specific to
One UI cannot be tested without the device.

- Do not claim a device behaviour works because the code compiles or a fake passed. Say what was and was not
  checked, and add an item to [`docs/testing.md`](docs/testing.md) for what was not.
- Every shell command goes through `ShellExecutor`, so the logic around it is tested against a fake fed with
  captured output.
- When something is measured on a phone, record it in [`docs/device-notes.md`](docs/device-notes.md) in the same
  change as the code that relies on it.

## Rules that protect the phone

- **The command set is fixed.** The app builds its shell commands itself from constants and validated package
  names. It never runs text a user typed, and there is no "run a custom command" feature.
- **A package name is validated against `^[A-Za-z0-9_.]+$` before it reaches a command.** Any other value that
  goes into a command is wrapped in single quotes with embedded quotes escaped: the device shell re-parses the
  whole line.
- **`null` from `settings get` means "unset".** Never write it back.
- **Read device state before changing it and restore it afterwards**, including when a run fails or is cancelled.
- **Wireless debugging is switched off again after a run** when the app was the one that switched it on.
- **The ADB private key never leaves encrypted storage**: not in logs, not in exports, not in backups.
- **Nothing private goes into the repository**: no serial numbers, and no list of apps taken from a real phone.

## Commits and pull requests

- A commit subject is an imperative sentence saying what changed ("Add the pairing notification flow"). The body
  says why, with a short bullet list when several things changed.
- Keep a change scoped to one thing.
- Never commit `local.properties`, `keystore.properties`, keystores or build output.

---

## The stack

| Concern | Library |
|---|---|
| UI | Jetpack Compose, Material 3 Expressive |
| Theme colour | MaterialKolor, from a seed the user can change |
| State | Orbit MVI |
| Dependency injection | Metro (`metrox-android`, `metrox-viewmodel-compose`) |
| Navigation | Navigation 3 |
| Errors | Arrow `Either` |
| Settings storage | DataStore Preferences |
| Secret storage | KSafe |
| Logging | Kermit |
| Icons | Tabler outline (`com.composables:icons-tabler-outline-cmp`) |
| ADB client | `libadb-android` with Conscrypt |
| Serialization | kotlinx-serialization (routes, stored models) |
| Screenshots | Roborazzi through the Compose preview scanner, on Robolectric |
| Open-source licences | AboutLibraries: the Android Gradle plugin and `aboutlibraries-compose-m3` |
| Tests | JUnit 4, `kotlin-test`, `kotlinx-coroutines-test`, `orbit-test` |

Versions live only in `gradle/libs.versions.toml`; `./gradlew versionCatalogUpdate` refreshes them.

Metro is a compiler plugin tied to the Kotlin version: move Kotlin first, then Metro.

The Compose BOM 2026.09.00 selects material3 1.4.0, which keeps the Expressive components `internal`, so
`androidx.compose.material3:material3` is pinned to a 1.5.0 alpha, with a comment beside the pin. Drop the pin
once the BOM selects a material3 that has them.

---

## Project shape

One module, `:app`. Under the package root `io.github.barqallayl.burkan`:

```
<root>/
├── MainActivity.kt, BurkanApplication.kt, App.kt   Entry points and the composition root — nothing else here
├── designsystem/    BurkanTheme, theme options, component/, preview/
├── core/
│   ├── di/          AppGraph; bindings that belong to no feature
│   ├── model/       Types more than one feature needs: AppError, AppErrorType
│   ├── navigation/  Routes, Navigator, navigation entries
│   ├── shell/       ShellExecutor and its ADB implementation
│   ├── storage/     DataStore- and KSafe-backed storage
│   ├── ui/          Composables more than one feature uses
│   └── util/
└── feature/<feature>/
    ├── data/        Repositories, parsers, anything that touches the shell or the system
    ├── di/          The feature's own @BindingContainer
    ├── model/       Domain types and the feature's errors
    └── ui/          *Screen.kt, *ViewModel.kt
```

- **Split by feature, then by responsibility** — never by technical layer across the whole app.
- **`model/` is plain Kotlin.** No Android framework types, no serialization annotations. A type that needs one
  belongs in `data/`.
- **Raw shell output never leaves `data/`.** Screens and ViewModels see domain models.
- **Promote into `core/` when a second feature actually needs a type, never in anticipation.**
- **The design system is a `designsystem` package at the root**, beside `core/`. It holds the theme, the
  `Burkan*` components and the preview annotations. It has no feature logic and no user-facing text of its
  own: a component that needs text takes it as a parameter.
- **No dead code.** No unused state fields, side effects, parameters, strings or public API with no caller.
  Removing the last caller removes the API.

Services and broadcast receivers are entry points like an activity: they resolve what they need from the graph
and hand over to a class in a feature's `data/`. No logic lives in them.

---

## A screen

Three functions, in `feature/<feature>/ui/<Name>Screen.kt`:

1. **The public screen** takes route arguments and nothing else. It resolves its ViewModel in the body — never
   as a default parameter — collects state and side effects, reads composition locals, and passes plain values
   and callbacks down.
2. **A private stateless content composable** takes state and callbacks. No ViewModel, no composition-local
   reads: everything arrives as a parameter, so a preview can supply it.
3. **A preview** of the content composable with sample state.

```kotlin
@Composable
fun SettingsScreen() {
    val viewModel = metroViewModel<SettingsViewModel>()
    val state by viewModel.collectAsState()
    val navigator = LocalNavigator.current
    viewModel.collectSideEffect { effect ->
        when (effect) {
            SettingsSideEffect.OpenLicences -> navigator.push(LicencesRoute)
        }
    }
    SettingsContent(state = state, onLicencesClick = viewModel::openLicences)
}
```

**The ViewModel** is Orbit MVI, with its state and side effects in the same file. There are no `*Contract.kt`
files:

```kotlin
@Immutable
data class SettingsState(val applyOnBoot: Boolean = true, val isSaving: Boolean = false)

sealed interface SettingsSideEffect {
    data object OpenLicences : SettingsSideEffect
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class SettingsViewModel(
    private val settings: SettingsStorage,
) : OrbitContainerHost<SettingsState, SettingsState, SettingsSideEffect>, ViewModel() {
    override val container = orbitContainer<SettingsState, SettingsSideEffect>(SettingsState())
}
```

- **A screen that posts no side effects uses `Nothing`** as the type. No empty sealed interface.
- **No business logic in composables**, and no user-facing text in a ViewModel. It holds errors and keys; the UI
  resolves them to strings.
- **Nothing outside the composition navigates.** There is no app-scoped navigator. Background code that must move
  the user signals through state, and `App.kt` reacts.
- **`App.kt` decides the start destination** from whether setup is complete. No screen navigates on finishing
  setup or on losing it; a second authority over that decision races the first.

---

## Navigation

Navigation 3, used directly:

- Routes are `@Serializable` objects and classes implementing `NavKey`, all declared in `core/navigation/`.
- `Navigator` wraps the back stack and exposes `push`, `pop`, `replaceAll`. It is created in `App.kt` and provided
  through `LocalNavigator`.
- One `entryProvider` maps every route to its screen.
- **Every route round-trips through serialization in a test**, so a route that cannot be saved fails a test
  instead of crashing on the first rotation.
- **Destination changes use the two transitions** in `core/navigation/ScreenTransition.kt`, passed to
  `NavDisplay`. Going to a screen, and going back with the back button, is `slideTransition`: both screens slide
  the full width, nothing fades or scales. Going back with the back gesture is `predictiveBackTransition`: the
  screens slide a fifth of the width while the one being left shrinks to 90 % and fades, and the one underneath
  grows and fades in. Every part of both rides one spring. A screen does not set its own transition.
- **A Settings screen opens as Settings' own window** (`Context.openSettings`), not inside the app's entry under
  recent apps.

---

## Dependency injection

Metro, one graph:

- `core/di/AppGraph` is the `@DependencyGraph(AppScope::class)`, created in `BurkanApplication` with the
  `Application` passed to its factory.
- **Each feature contributes its own bindings** from `feature/<feature>/di/`, so adding a feature never edits a
  file another feature shares:

  ```kotlin
  @ContributesTo(AppScope::class)
  @BindingContainer
  object ApplyModule {
      @Provides
      @SingleIn(AppScope::class)
      fun provideApplyRepository(shell: ShellExecutor): ApplyRepository = DefaultApplyRepository(shell)
  }
  ```

- ViewModels are contributed with `@ContributesIntoMap` + `@ViewModelKey` and resolved with `metroViewModel()`.
  `LocalMetroViewModelFactory` is provided once, in `App.kt`.
- Activities, services and receivers are constructed through `metrox-android`'s component factory.
- **Never give a `@DependencyGraph` interface a companion object.** The compiler plugin generates one, and a
  hand-written one crashes the compiler.

---

## Errors

- **A function that can fail returns `Either<AppError, T>`.** It does not throw across a layer boundary.
- `AppError` is an interface in `core/model/`. Each feature adds its own errors as a sealed type that implements
  it, in the feature's `model/`. `AppError` itself cannot be sealed: Kotlin requires a sealed type's direct
  subtypes to be in its own package.
- **Cancellation is rethrown, always.** Catching `CancellationException` as a failure turns a closed screen into
  an error message.
- **Only the unexpected is logged as an error.** "No Wi-Fi", "not paired", "wrong pairing code" are known
  outcomes with their own error types.
- **Errors become text through `AppErrorType`**, in `core/model/`: an enum with one entry per user-facing message,
  each carrying its string resource, and one function mapping errors onto it:

  ```kotlin
  enum class AppErrorType(@StringRes val resource: Int) {
      NoWifi(R.string.error_no_wifi),
      NotPaired(R.string.error_not_paired),
      Unexpected(R.string.error_unexpected),
  }

  fun AppError.messageRes(): Int = when (this) {
      ConnectionError.NoWifi -> AppErrorType.NoWifi.resource
      ConnectionError.NotPaired -> AppErrorType.NotPaired.resource
      else -> AppErrorType.Unexpected.resource
  }
  ```

  State holds the `AppError`; the UI calls `stringResource(error.messageRes())`.

---

## Storage

- **DataStore holds settings**: apply on boot, the user's exclusions, the appearance preferences, the run log.
- **KSafe holds secrets**: the ADB private key and certificate. Encrypted, excluded from backup.
- **Reads that touch disk are flows**, collected by a store or a ViewModel. Never a blocking read in a composition or in a
  ViewModel constructor.

---

## Stores

- **What a screen shows on opening is already in memory.** A store is an app-scoped class that reads its data
  ahead of the screens and keeps it current in a `StateFlow`: `SettingsStore` (the settings and the phone's
  state), `RunLogStore`, `InstalledAppsStore` and `LibraryStore`. `StoreWarmUp`, called when the activity is
  created, starts them; the services do not.
- **A ViewModel takes the store's value as its first state** and then collects the store, so a screen opens with
  its content. Its state is nullable or `Loading` only for the case the store has nothing yet, which a screen
  still draws as loading.
- **A store only reads.** Writes go through the storage interface; the store sees the change like anyone else.
- **Data that can go stale is refreshed without being withdrawn**: the list of installed apps is listed again each
  time its screen opens, and the old list stays on screen until the new one replaces it.

## UI

- **Material 3 Expressive components**, Expressive variants where one exists (buttons, loading and progress
  indicators, app bars). Colours come from the scheme and type from the theme; nothing is styled at a call site.
  `ExperimentalMaterial3ExpressiveApi` is opted in module-wide.
- **Content sits in segmented groups.** Related rows form one group whose outer corners are large and whose inner
  corners are small, with a 2 dp gap between segments (`designsystem/component/BurkanSegment.kt`). A row of a list
  is a `BurkanSegmentItem`; anything else goes in a `BurkanSegment`. Pass each its index and the group's count.
  There is no free-standing card and no divider: a group of one is still a segment.
  - A group has a `BurkanSectionTitle` when the screen has more than one kind of group.
  - A segment that needs to stand out takes a container colour from the scheme: `primaryContainer` for the status
    headline and the current setup step, `secondaryContainer` for a notice.
  - A click or a toggle goes inside the segment (`onClick`, `interaction`), so its ripple follows the corners.
- **The look is tuned through preferences, not code.** `BurkanTheme` takes every appearance choice as a
  parameter: the scheme's inputs go to MaterialKolor's `DynamicMaterialExpressiveTheme`, and the font sets
  Material's own type scale (`burkanTypography`), leaving its sizes and weights alone. The parameters have no
  defaults, so the only defaults are those in `SettingsStorage.Defaults`.
- **The appearance preferences** are stored in `SettingsStorage` and edited in Settings:

  | Preference | Options | Default |
  |---|---|---|
  | Dark theme | Follow system, light, dark | Follow system |
  | Seed colour | The list in `SeedColors` | `SeedColors.Default` |
  | Palette style | The list in `PaletteStyles` (every MaterialKolor style) | Expressive |
  | Font | The phone's own, or one of the bundled typefaces in `AppFont` | Space Grotesk |
  | Text size | 85–130 %, in fives | 100 % |

  `SettingsStorage.appearance()` combines their flows into one `Appearance`; `App.kt` collects it through
  `AppViewModel` and passes the values to `BurkanTheme`. The scheme is always generated under Material's 2026
  colour spec, and a change of theme or colour animates from the old scheme to the new. `App` reports whether the
  app is dark through `onThemeChange`, and `MainActivity` colours the system bars' icons to match. Text size multiplies the system font scale and is applied once, in `App.kt`. Defaults live in one
  `Defaults` object in the storage class, so a preview and a fresh install agree.
- **A list row is `BurkanSegmentItem`**, Material's segmented list item: a pressed row and a selected or ticked one
  change shape as well as colour. A setting that is switched on is not a selected row; only its switch shows it.
- **A choice is made in a bottom sheet** (`BurkanBottomSheet`), which is open in full or closed, never half open.
  One option out of several is a `BurkanChoiceList` of radio segments and applies at once with the sheet left
  open; text size is chosen on a sample and applied on Save.
- **Fonts are bundled, not downloaded**: the files are in `res/font`, each under the SIL Open Font License, and
  each has an entry in `app/aboutlibraries/libraries/` so it appears on the licences screen. Adding one means the
  file, an `AppFont` entry and that licence entry, with a group of its own in `uniqueId` so the fonts are not
  merged into one.
- **English only.** One `res/values/strings.xml`. No user-facing string as a literal, in a composable or a
  ViewModel.
- Use `start`/`end`, never `left`/`right`.
- **Icons are Tabler outline.** Do not hand-roll a vector the set has. Anything that points — back, a chevron,
  an arrow — is `Icons.AutoMirrored.Outlined.*`.
- **Never colour alone** to carry meaning. Pair a status colour with an icon or a label: "Vulkan" and "OpenGL"
  are written out, not shown as green and red dots.
- **Status colours are fixed, not generated**, because the Material scheme follows a seed the user chooses. They
  live in the theme next to the scheme, with a light and a dark set.

---

## Previews and screenshots

- **`@BurkanPreview`** is the multipreview annotation. Paired with
  `@PreviewWrapper(BurkanPreviewWrapper::class)`, the preview renders in the app's theme with the default
  preferences, in Android Studio and in screenshot tests alike. `@PreviewWrapper` can only annotate a function,
  so it goes on each preview rather than inside `@BurkanPreview`. A preview that needs dark, large text or
  another seed leaves the wrapper off and wraps its body in `BurkanPreviewTheme(…)`.
- **Preview the stateless content composable with sample state.** A preview that resolves a ViewModel renders in
  neither Android Studio nor a screenshot test.
- **Sample data is pinned to a fixed date**, and nothing in a preview reads the real clock.
- **Every preview is a screenshot test.** Roborazzi's `generateComposePreviewRobolectricTests` renders each one
  under Robolectric, private previews included. There are no hand-written screenshot tests to keep in sync.

  ```kotlin
  roborazzi {
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
      outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
  }
  ```

  - **Baselines are committed**, under `app/src/test/screenshots`. Left in `build/`, a clean wipes them and
    `verify` compares against nothing and passes.
  - **`RoborazziTestApplication`** implements `MetroApplication` with empty provider maps. `metrox-android`'s
    component factory casts the application to it, and Robolectric's plain `Application` dies with a
    `ClassCastException` before rendering anything.
  - Robolectric may not start on the newest SDK; render at one it supports. The `apiLevel` in a preview annotation
    names the image and does not select the runtime.
  - `android.testOptions.unitTests.isIncludeAndroidResources = true`, or every screenshot is blank.
- **Look at the new images before keeping them.** Recording accepts whatever renders.
- **A state worth looking at deserves a preview**: loading, failed, not set up, dark, large text. They are the
  states awkward to reach on a real phone.
- **Every screen that loads has a loading, an empty and a failed preview**, where the screen has such a state. A
  list that is empty or could not be read shows `BurkanMessage` (`designsystem/component/`): an icon, a title,
  a sentence saying what will appear or what went wrong, and a way on where there is one. Loading keeps the layout
  of what is loading where it can, so nothing jumps when it arrives.

---

## Testing

- **Exercise the real class.** Instantiate the repository or ViewModel under test and give it fakes for its
  collaborators. A test that asserts on a fake's canned answer tests nothing.
- **Fakes, not mocking frameworks.** Extract an interface rather than making a class `open` for a test.
- **Parse captured device output, not hand-built strings.** The fixtures in `app/src/test/resources/fixtures/`
  follow the formats in `docs/device-notes.md`, with invented third-party package names. When the device shows a new shape, add it as a fixture.
- **Assert on the commands sent.** For every flow there is a test that runs it against a recording fake shell and
  compares the exact command list, in order.
- **Unit tests construct no Android framework types** (`Application`, `Context`). Code that needs one sits behind
  an interface.
- A fake that reimplements a production rule shares that rule's code, so an assertion against the fake means
  something.

---

## Build

- **JDK 25 toolchain, JVM 17 bytecode**, the Gradle wrapper, versions only in `gradle/libs.versions.toml`.
- **Compile with `-Xreturn-value-checker=check`.** Consume a returned value — bind it, `fold` it — or say in a
  comment why it is dropped.
- **Opt-ins go in `compilerOptions.optIn`** at module level, not as `@OptIn` scattered through the source.
- **Context parameters are not used.**
- Release builds are minified and resource-shrunk. Keep rules that a library needs go in
  `app/src/main/keepRules/`, as `.keep` files; resources that are only read by name at run time are kept in
  `res/raw/keep.xml`.
- **The licences list is generated at build time** by the AboutLibraries Android plugin, in offline mode, into
  `R.raw.aboutlibraries`; nothing is fetched at run time. Corrections to what a library's metadata says go in
  `app/aboutlibraries/`. Artifacts of one group under one licence are merged into one entry; the plugin is pinned
  to 15.1.0 because later versions stopped merging them. The plugin resolves the release classpaths while configuring, which Gradle reports as a
  performance note on a configuration cache miss; the cache entry is still stored and reused.
- JitPack is declared with a content filter so only `com.github.MuntashirAkon` (and its `spake2-java` group)
  resolves from it.
- Conscrypt's Android artifact is left off the unit-test runtime classpath: its native code is Android-only, and on
  the JVM it shadows the Conscrypt Robolectric installs for itself.
