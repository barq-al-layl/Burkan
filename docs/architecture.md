# Architecture

What Burkan does, the constraints that shape it, and how it is put together. The shell commands themselves, and
what was measured on a real phone, are in [`device-notes.md`](device-notes.md).

## The problem

On a Galaxy S23, Android's UI toolkit can render with Vulkan instead of OpenGL, which a community of owners
reports as smoother and cooler. The switch is a system property, `debug.hwui.renderer=skiavk`. Two things make
it awkward:

- only the `shell` user can set it, so it has needed a computer with ADB, or Shizuku with Termux;
- it resets on every reboot, so it has to be done again each time.

Burkan does it on the phone, by itself, after every restart.

## What the app does

1. **Sets itself up once**, on the phone, with no computer: it pairs with the phone's own wireless debugging and
   grants itself the one permission it needs.
2. **After every reboot**, when the phone is on a Wi-Fi network it is allowed to use, it switches wireless
   debugging on, connects to `127.0.0.1`, applies Vulkan, restarts the three system surfaces that were already
   running, and switches wireless debugging off again.
3. **Shows the truth**: which renderer new apps will get, and which renderer SystemUI, the launcher and the
   keyboard are actually using right now.
4. **Applies on demand**, either lightly or by restarting every app.

### Not in scope

- Root, Shizuku or a companion desktop tool.
- Running arbitrary commands. The command set is fixed (see `CONTRIBUTING.md`).
- A per-app opt-out from Vulkan. The scripts' `game_driver_blacklist` feature does nothing on current Android
  (see `docs/device-notes.md`, section 10).
- Devices other than the S23 family. The app warns on other models and lets the user continue; it makes no
  claim about them.
- Any network use beyond the loopback connection. No analytics, no crash upload, no update check.

## Constraints that shape the design

All from `docs/device-notes.md`; read it for the detail and for which of these are measured.

- **Shell access means an ADB connection to the phone itself.** The app carries an ADB client
  (`libadb-android`), its own key pair, and connects over TLS to the daemon on loopback.
- **Wireless debugging needs Wi-Fi**, and on a network the user has not marked "Always allow", Android asks
  before enabling it. A reboot away from a trusted network cannot be handled silently; the app waits and
  explains.
- **The pairing dialog closes when the user leaves Settings**, so the pairing code is typed into a notification.
- **A running process keeps its renderer.** Setting the property is not enough; SystemUI, the launcher and the
  keyboard have to be restarted, each in its own way.
- **The boot work runs with no UI**, so it runs in a foreground service started from the boot receiver.

## Screens

Six destinations. `App.kt` chooses the start: `SetupRoute` until setup is complete, `HomeRoute` after.

### Setup (`SetupRoute`)

A checklist the user works down. Each step shows done, current or waiting, and the current step has one action.

| Step | Action | Done when |
|---|---|---|
| Notifications | Request `POST_NOTIFICATIONS` | Granted |
| Developer options | Open About phone and explain how to unlock Developer options there (the Developer options screen cannot open while they are locked) | `development_settings_enabled` is 1 |
| Wireless debugging | Open Developer options; ask the user to switch Wireless debugging on and tick "Always allow on this network" | `adb_wifi_enabled` is 1 |
| Pair | Start the pairing service and open Developer options; tell the user to tap "Pair device with pairing code" and type the code into Burkan's notification | Pairing succeeded |
| Connect | Automatic | A shell command returns |
| Permission | Automatic: the app grants itself `WRITE_SECURE_SETTINGS` over the connection | The permission is held |
| Battery | Ask to be exempt from battery optimisation, with one sentence saying why | Exempt, or skipped |

- Steps the app can detect are detected, never self-declared with a "Done" button.
- A failed step shows what failed and how to retry. Wrong code, pairing timed out, no Wi-Fi and "the pairing
  dialog was closed" are distinct messages.
- Leaving and returning resumes at the first incomplete step.
- If the device model is not SM-S911*, SM-S916* or SM-S918*, a notice at the top says the app is only tested on
  the S23 family. It does not block.

### Home (`HomeRoute`)

The status, and the two actions.

- **Headline**: one of *Vulkan is active*, *OpenGL — not applied since the last restart*, *Partly applied*,
  *Unknown — cannot connect*. With an icon and words, never colour alone.
- **Detail rows**: New apps (from the property), System UI, Launcher, Keyboard — each "Vulkan", "OpenGL" or
  "Unknown", from `dumpsys gfxinfo`, each with its own icon as well as the word. While the status is read the rows
  stay in place with placeholders; while a run is in progress the card says its values are being changed.
- **Last run**: when, what triggered it (boot or manual), and its result.
- **Apply now** — the light apply. The primary button. When System UI is to be restarted, a confirmation says
  first that the screen will lock for a moment.
- **Restart all apps** — the full apply. Behind a confirmation that says what it does: closes every app, takes
  about a minute, the screen will flash, and it locks for a moment at the end.
- While a run is in progress: a progress indicator, the current step in words, a Cancel button, and both apply
  buttons disabled. The run's notification carries Cancel too.
- If automatic apply is waiting on something (no Wi-Fi, network not trusted), a card says what it is waiting for.
  When the run after a restart left System UI for the next lock, a card says so.
- After a run, Home shows the status the run read at its end, without connecting again. It reads the status when
  the screen resumes, unless it read it in the last 30 seconds. Reading needs a connection; if wireless debugging
  is off and the app holds the permission, it may switch it on to read and off again after.

### Settings (`SettingsRoute`)

- Apply automatically after restart (on by default).
- Turn wireless debugging off after applying (on by default).
- Never restart these apps: the user's own exclusions for the full apply, chosen from the installed apps on their
  own screen (`ExclusionsRoute`). The fixed exclusions are listed read-only beneath, with the app's label and icon
  where it is installed. Listing the apps has its own loading, empty and failed states.
- Appearance: dark theme, seed colour, palette style, colour spec and text size. Each change applies at once.
  The options and defaults are in `CONTRIBUTING.md`.
- Redo setup: forgets the pairing and returns to Setup, after a confirmation.
- About: version, source link, the app's licence (GPL-3.0-or-later, with its no-warranty notice), and the
  libraries' licences on their own screen (`LicencesRoute`). That list is generated at build time from the
  dependencies (AboutLibraries) and read from the app's resources: no network at run time.

### Log (`LogRoute`)

- The last 50 runs, newest first: time, trigger, result, duration. With no runs, a centred message says what will
  appear and leads back to apply; a stored log that cannot be read says so.
- Opening a run shows its steps in words, each with success or the error.
- **Share** exports the log as text. It never contains the ADB key, and no package names: steps are recorded with
  counts ("Stop 612 apps"), so there is nothing to warn about before sharing.

## How it is built

The code structure and conventions are in `CONTRIBUTING.md`. These are the parts specific to this app.

### Features

| Feature | Holds |
|---|---|
| `setup` | The checklist screen, pairing notification service, step detection |
| `connection` | Key pair, pairing, connecting, enabling wireless debugging; the ADB `ShellExecutor` |
| `apply` | The light and full flows, the output parsers, state capture and restore, the boot receiver and service |
| `status` | Reading renderer state; the Home screen |
| `settings` | The Settings screen and its storage |
| `log` | Run history and the Log screen |

`ShellExecutor` itself is in `core/shell/` because `apply` and `status` both use it.

### The shell boundary

```kotlin
interface ShellExecutor {
    /** Runs one command line as the shell user. A non-zero exit is a [ShellResult], not an error. */
    suspend fun run(command: ShellCommand): Either<ShellError, ShellResult>
}

data class ShellCommand internal constructor(val line: String, val timeout: Duration)

data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)
```

- Only `ShellCommands` builds a `ShellCommand`, and each carries its own timeout. (`internal` keeps it inside the
  module; within the module, this is a rule rather than something the compiler enforces.)
- Everything above this interface is plain Kotlin and unit-tested with a fake.
- `ShellError` covers what stops a command from running at all: not connected, connection lost, timed out.
- The ADB implementation uses the shell protocol that returns the exit code and separates stderr. libadb-android
  names only the legacy `shell:` service, but `openStream(String)` takes any destination, so the app opens
  `shell,v2,raw:<command>` and decodes the v2 packets itself (`ShellProtocolDecoder`).
- Every command has a timeout. A `dumpsys` gets 20 seconds; the bulk stop gets three minutes.
- `ShellCommands` is the one place commands are built: a function per command, taking validated types
  (`PackageName`, not `String`). Quoting lives there and nowhere else.

### Flows are plans

A flow is a function that returns the list of steps, and a runner that executes them:

- `LightApplyPlan` and `FullApplyPlan` produce `List<ApplyStep>` from inputs (package list, exclusions, wallpaper
  package, keyboard package).
- `ApplyRunner` executes steps through `ShellExecutor`, records each result in the log, and guarantees the
  restore step runs when a full apply fails or is cancelled.
- Tests assert on the plan (exact commands, in order) without running anything, and on the runner with a fake
  shell that fails at a chosen step.

### Connection

- **Key pair**: 2048-bit RSA and a self-signed certificate, generated on first use, stored in KSafe.
- **Pairing**: a foreground service posts a notification with a text-reply action. It discovers
  `_adb-tls-pairing._tcp` on the local host, pairs with the code the user types, and reports the result in the
  notification and to the Setup screen.
- **Connecting**: discover `_adb-tls-connect._tcp`, connect to `127.0.0.1` on that port, with the device's Wi-Fi
  address as fallback. Retry discovery for up to 30 seconds after wireless debugging is switched on.
- **Enabling wireless debugging**: write `Settings.Global` `adb_wifi_enabled` through the content resolver. Read
  it back after two seconds: if it is 0 again, the system refused (no Wi-Fi, or network not trusted) and the
  result is a specific error, not a retry loop.
- **One connection at a time**, owned by a single class, behind a mutex. A run holds it from start to finish.
- **A lost connection is picked up again.** adbd restarts on every keyguard change, on a new port. The shell a block
  gets waits a second, finds the port again and reconnects, for up to 30 seconds. A command that never reached the
  daemon is sent again; one that lost its connection part way is sent again only if repeating it is harmless, which
  is every command except `am crash`.
- **A connection is kept for 10 seconds** after a block, and a block in that time reuses it: every new connection
  raises the system's "Wireless debugging connected" notification. A run releases it as soon as it ends, and so
  does Home when it is left.

### After boot

1. `BootReceiver` (`BOOT_COMPLETED`) starts `ApplyService` if "apply automatically" is on and setup is complete.
2. `ApplyService` is a foreground service (`specialUse`) with a low-importance notification: "Applying Vulkan".
   Manual runs use it too: a full apply relaunches other apps over Burkan, and a cached process can be frozen
   before its restore steps run.
3. If the property is already `skiavk` and SystemUI reports Vulkan, stop: nothing to do.
4. Without Wi-Fi, do not start the service: leave the Home card saying it is waiting, and register a network
   callback (with a `PendingIntent`, so it outlives the process) that starts the run when Wi-Fi connects later in
   the same boot. The callback covers the rest of the boot without holding a
   foreground service. A network the system refused wireless debugging on
   is remembered and not tried again until a different network connects. If Android does not let the callback
   start the service from the background, a notification with an "Apply" action starts it instead.
5. Enable wireless debugging, connect, run the light apply without System UI, verify, disable wireless debugging
   if the app enabled it, write the log entry. The user has just unlocked the phone, and restarting System UI would
   lock it again, so if System UI is not on Vulkan the service stays in the foreground, with a notification saying
   so, until the phone next locks. Then it runs again, logged as "System UI at lock", with a wake lock held while
   the screen is off. Locking restarts adbd too, so the run connects no earlier than 3 seconds after the lock, a
   wait that overlaps the one for switching wireless debugging on. It reads only the property and System UI.
   Immediately before `am crash` it checks that the keyguard is still locked. If the user has unlocked meanwhile,
   it leaves System UI alone, disconnects, switches wireless debugging back, logs "Left for the next lock" and waits
   for the next lock. Home says System UI will switch at the next lock. A manual apply meanwhile replaces the wait.
6. On failure, post a notification that opens the app. Never retry in a tight loop.

`BOOT_COMPLETED` arrives after the first unlock, which is also when encrypted storage becomes readable. The app
is not direct-boot aware and does nothing before that.

`BOOT_COMPLETED` also arrives without a restart. Since Android 15 the system sends it to an app leaving the stopped
state, and every force-stop leaves an app there, including the one an install from Android Studio does. So
`BootReceiver` compares `Settings.Global.BOOT_COUNT` with the count it last handled. The same count is no restart:
nothing is applied, and only what this boot is still waiting for, Wi-Fi or the lock, is set up again. The
force-stop cancelled the network callback. An unknown count is taken as a restart.

### Permissions and manifest

`INTERNET` (loopback sockets), `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE` (mDNS),
`RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` (System UI's restart once the screen is off), `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`,
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `QUERY_ALL_PACKAGES` (the exclusions picker), and
`WRITE_SECURE_SETTINGS` (declared; granted over ADB).

- `minSdk` 33: the S23 shipped with Android 13, and it removes every pre-13 branch. `targetSdk` and `compileSdk`
  37.
- Backup: exclude the ADB key and the pairing state from cloud backup and device transfer.

## Decisions

- **`minSdk` 33.** The S23 shipped with Android 13, and it removes every pre-13 branch.
- **English only**, and stock Material 3 Expressive, tuned through the appearance preferences rather than custom
  styling.
- **GPL-3.0-or-later.** `libadb-android` is used under its GPL-3.0-or-later option.
- **Distributed outside the Play Store.** `QUERY_ALL_PACKAGES`, the `specialUse` foreground service and a
  permission granted over ADB are a poor fit for it.
