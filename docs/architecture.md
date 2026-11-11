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
3. **Shows the truth**: which renderer new apps will get, which renderer SystemUI, the launcher and the
   keyboard are actually using right now, and which open apps are still on the other one.
4. **Applies on demand**, either lightly, by restarting every app, or by restarting only the open apps that are
   behind.
5. **Goes back to OpenGL** when asked, without a restart of the phone.
6. **Runs without being opened**: an apply, or the full flow that also restarts every app, started from a
   shortcut on the app's icon or by another app such as Tasker.

Steps 1 and 2 describe the default: the shell comes from the phone's own wireless debugging. Settings has **Use
Shizuku**, off until chosen, for a user who keeps Shizuku running already, by a fork that starts it after a
restart for one. Burkan then asks Shizuku for its shell, needs neither Wi-Fi nor pairing, and starts nothing
itself. Everything else, the flows included, is the same. See Connection.

### Not in scope

- Root or a companion desktop tool. Shizuku is not needed, and Burkan never starts it: using it is a choice in
  Settings (see Connection).
- Running arbitrary commands. The command set is fixed (see `CONTRIBUTING.md`).
- A per-app opt-out from Vulkan. The scripts' `game_driver_blacklist` feature does nothing on current Android
  (see `docs/device-notes.md`, section 10).
- Devices other than the S23 family. The app warns on other models and lets the user continue; it makes no
  claim about them.
- Any network use beyond the connection to the phone itself and the local service discovery that finds its port.
  No analytics, no crash upload, no update check. A crash report stays on the phone until its user shares it.

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

Eight destinations. `App.kt` chooses the start once: while getting started, `WelcomeRoute` on a fresh install and
`SetupRoute` otherwise; `HomeRoute` once setup is complete and its First apply has been taken or put off. Getting
started is a one-way door, closed by replacing the back stack (`OpeningNavigation.kt`):

- While the steps are being done, the stack is the screens gone through, so Back goes from Setup to the choice of
  connection and to Welcome.
- Once setup is complete, Setup is left alone on the stack, so Back from the First apply cannot reach the choice
  and undo the setup just finished.
- When getting started ends, Home replaces it; when it begins again (Redo setup, another connection, a pairing the
  phone dropped), Setup replaces Home and what is above it. The screens taken off have their ViewModels cleared, so
  Setup run again starts afresh rather than with what the last run left in it.

### Welcome (`WelcomeRoute`)

Shown on a fresh install until it is left for good: not after an update, and not after "Redo setup", which marks it
seen. A phone without Shizuku's app leaves it with Get started; one with Shizuku's app leaves it when the connection
is chosen, so backing out of the choice and closing the app opens on Welcome again. A phone drawn
in the style's own colours, whose status bar, home-screen icons and keyboard redraw one after another in about a
second and a half, then rest; with Android's animations removed it is drawn redrawn from the start. Under it, what
Burkan does, that setup takes about two minutes and Developer options once. On a phone without Shizuku's app, which
can only use wireless debugging, a line comes first when the phone is not on Wi-Fi; it informs and does not block, and
follows Wi-Fi as it comes and goes, also when switched from the notification shade. Get started goes on to the choice
of connection when Shizuku's app is installed, and to Setup otherwise.

### Connection (`ConnectionRoute`)

Offered only when Shizuku's app is installed. Wireless debugging is chosen to begin with and marked recommended;
Shizuku's row says that it has to be running before Burkan after every restart. Continue makes the choice through
`ShellBackendSwitcher` and opens Setup.

### Setup (`SetupRoute`)

A checklist the user works down, one step at a time. The steps that are done collapse into one row, the current
step carries its instructions and its action, and the ones still to come wait under it.

| Step | Action | Its check passes when |
|---|---|---|
| Notifications | Request `POST_NOTIFICATIONS`. Once Android stops asking, after two refusals, open the app's notification settings instead | Granted |
| Developer options | Open About phone and explain how to unlock Developer options there (the Developer options screen cannot open while they are locked) | `development_settings_enabled` is 1 |
| Wireless debugging | Open Developer options; ask the user to switch Wireless debugging on and tick "Always allow on this network" | `adb_wifi_enabled` is 1 |
| Pair | On Android 17, ask for the nearby devices permission first if it is not held. Then start the pairing service and open Developer options. Shown as two numbered actions: tap "Pair device with pairing code", then type the code into Burkan's notification | Pairing succeeded |
| Finishing setup | Automatic: a shell command, then the app grants itself `WRITE_SECURE_SETTINGS` over the connection if it does not hold it | The command returned and the permission is held |
| Battery | Ask to be exempt from battery optimisation, with one sentence saying why. Marked recommended while it waits | Exempt. It can also be skipped |

- A step is looked at only once it is the current one. Nothing further down is checked, or done, ahead of time:
  Finishing setup connects and grants when the user reaches it.
- What passes the check is detected, never self-declared. A step done in Settings is finished only when the user
  taps Done, and one the phone already satisfies shows that it does and still waits. Notifications, Finishing setup,
  Shizuku's permission and Battery end in a system dialog or the app's own work, so they show their tick for a moment
  and move on by themselves.
- How far the user has got is stored as a count of finished steps, with the phone's own state, which is never
  backed up, and the version of the step list it counts in. A count from version 1, which had Connect and
  Permission as two steps, is translated once: someone who had connected lands on Finishing setup. Redoing setup
  puts the count back to none.
- A failed step shows what failed and how to retry. Wrong code, pairing timed out, no Wi-Fi and "the pairing
  dialog was closed" are distinct messages.
- On the two steps done in the Wireless debugging screen, a phone that is not on Wi-Fi is told so before it goes
  there: Android offers that switch only on Wi-Fi.
- Leaving and returning resumes at the current step and checks it again.
- A pairing the phone no longer accepts sends setup back to the Pair step, and the steps after it are gone
  through again. That holds once setup is complete as well: setup is then no longer complete, so the app leaves
  Home for Setup, and nothing is applied after a restart until it has been finished again.
- If the device model is not SM-S911*, SM-S916* or SM-S918*, a notice at the top invites the owner to try it: it
  says the app is tested on the S23 family and should work on this phone too, with a link to report a problem. It
  is shown before the first step is finished, with the intro, and does not block.
- Setup has a back arrow while there is a screen behind it, the choice of connection or Welcome; after "Redo setup"
  there is none.
- Once every step is done, setup is complete, and the apply after a restart is on, but the screen stays for the
  First apply: Apply now, after saying that the screen locks once, or Later. Either one ends getting started and
  Home opens. It is offered when setup is completed, and not again.

### Home (`HomeRoute`)

The status, and the two actions. The two styles lay it out differently; what it says is the same.

- **Headline**: one of *Vulkan is active*, *Vulkan is not applied*, *Partly applied*, *Waiting for the lock*, or,
  when the status cannot be read, the cause by name where it is one the user can put right (*Not connected to
  Wi-Fi*, *Wireless debugging is off*) and *Unknown — cannot connect* otherwise. In words, never colour alone. A
  pairing the phone has dropped reads *Burkan is no longer paired* only for as long as Home takes to give way to
  Setup. *Waiting for the lock* is shown, calm rather than amber, when the run after a restart left System UI for
  the next lock and System UI is the one thing still on OpenGL; with anything else on OpenGL too, it is *Partly
  applied*. The headline and its note are read out by TalkBack once, together, when either changes.
- **When the status cannot be read**, the status card says why under the headline, with Try again (or Allow, where
  a permission is missing) beneath it; nothing else on the screen repeats the reason. The rows say "Unknown", and
  Apply now and Restart all apps are unavailable until the phone can be asked.
- **Detail rows**: New apps (from the property), System UI, Launcher, Keyboard — each "Vulkan", "OpenGL",
  "Not drawn yet" (running, with nothing drawn, as the keyboard is from its restart until it is shown) or "Unknown",
  from `dumpsys gfxinfo`, each with its own icon as well as the word. While the status is read the rows
  stay in place and say "Checking"; while a run is in progress they say "Changing", rather than show the values
  the run is changing.
- **Last run**: when, what triggered it (boot or manual), and its result.
- **Open apps**: a fifth row, under the surfaces. A process keeps the renderer it started with, so after an apply
  on a phone that has been in use, what was open is still on OpenGL. The row says how many apps that is, or that
  all of them are on what new apps get. With some behind it opens a sheet that names them and offers to restart
  only those: the machinery of Restart all apps, limited to them, with the property and the surfaces left as they are.
  Each app's row restarts that app alone, or puts it among the apps never restarted, which takes it off the list.
  After going back to OpenGL the same row counts the apps still on Vulkan. An app no run would restart (a
  keyboard, the wallpaper, one the user set aside, the fixed exclusions) is not counted, nor is the system's own
  process, `system`, which no restart moves. The apps are read after the status is shown, from one
  `dumpsys gfxinfo` of every process, and a run reads them again at its end, the run at the lock included.
- **Back to OpenGL**: a row in the details, shown while anything is on Vulkan. Behind a confirmation, it clears
  the property and restarts the surfaces that are on Vulkan, System UI last and checked on OpenGL. It does not
  switch "Apply after restart" off: the confirmation says Vulkan is applied again after the next restart if that
  is on.
- **The main action follows what there is to do**: Apply now, filled, while anything is not on Vulkan; Restart N
  apps, filled, when Vulkan is active and open apps are left behind, which opens the sheet of those apps; Apply now,
  quiet, when nothing is left to do or what is left happens at the next lock; Cancel during a run.
- **Apply now**: sets new apps to Vulkan and restarts System UI, the launcher and the keyboard. When System UI is
  to be restarted, a confirmation says first that the screen will lock for a moment.
- **Restart all apps**: Apply now, and every app closed and reopened too. Behind a confirmation that says what it does: closes every app and opens
  again those that were open or show a widget, takes
  about a minute, the screen will flash, and, when System UI is to be restarted, it locks for a moment at the end.
  The confirmation also asks how far to go: every app, which it starts from each time, or only the 30 or 70 used
  most recently. Fewer is quicker; an app left out keeps its old renderer until it next starts. Android
  says which apps are recent only to an app with usage access, which the app allows itself over its own connection
  the first time a limited run needs it, and the confirmation says so. If Android still names none, the run fails
  rather than restart everything.
  At its end Restart all apps brings Burkan back to the front.
  An app is closed with `am stop-app`, which keeps its alarms and scheduled jobs: `am force-stop` cancels them, and
  a force-stopped Clock app loses the alarm it was to ring. A phone whose `am` has no `stop-app` gets
  `force-stop` for each app instead.
- **In the Material style** the screen reads from the top as the answer, then lists: the status card, the four
  surfaces as a list with each renderer at the end of its row, then the last run, Apply after restart, and
  Restart all apps as a row that opens its confirmation. The main action is the one button, docked at the bottom.
  While a run is in progress the card shows a progress indicator and what the run is doing in words, that button
  becomes Cancel, and the Restart all apps row is dimmed and does not react.
- **In the One UI style** nothing is docked: a summary card at the top carries the state in large words, the main
  action and Restart all apps side by side, the main action in the accent when there is something to do and grey
  when not. When the status cannot be read, the card's one button is Try again. During a run the card's one button is Cancel, and the surfaces give way to the run's
  steps as a checklist, each ticked as it ends; a step that failed is marked. When the run has ended the button
  is Done, which puts the surfaces back.
- The run's notification carries Cancel too.
- If automatic apply is waiting on something (no Wi-Fi, network not trusted), a card says what it is waiting for,
  unless the status card already says the same. When the run after a restart left System UI for the next lock and
  something else is still on OpenGL, a card says why System UI waits; when System UI is all that is left, the
  headline says it instead.
  Restarting the open apps changes neither: it applies nothing, so whatever the run after a restart waits for, it
  still waits for.
- While Android would forget a paired device that has not connected for a while (`adb_allowed_connection_time`,
  7 days unless Developer options' "Disable adb authorization timeout" is on), a card says so and opens Developer
  options. A phone neither restarted nor opened in Burkan for that long would otherwise need pairing again before
  the next run after a restart could apply anything.
- After a run, Home shows the status the run read at its end, without connecting again. It reads the status when
  the screen resumes, unless it read it in the last 30 seconds or the user is only back from Log or Settings:
  while the user stays in the app nothing else starts, so the status cannot have moved, and each new connection
  drops the system's notification over Home's top bar. Reading needs a connection; if wireless debugging is off
  and the app holds the permission, it may switch it on to read and off again after.

### Settings (`SettingsRoute`)

Four groups: Applying, Connection, Appearance and About.

- **Applying**
  - Apply after restart (on by default).
  - Never restart these apps: the user's own exclusions for Restart all apps, chosen from the installed apps on
    their own screen (`ExclusionsRoute`), which can be searched by name or package name and filtered to the
    selected apps, the user's own or the system's. The fixed exclusions are listed read-only beneath, with the
    app's label and icon where it is installed. Listing the apps has its own loading, empty and failed states.
    TalkBack reads an app by its name and whether it is ticked; its package name is read only when it has no name.
- **Connection**
  - Connection: wireless debugging or Shizuku, changed after a confirmation, which starts setup again.
  - Turn off wireless debugging after applying (on by default), with wireless debugging only.
  - Let other apps start runs (on by default): Tasker and other automation apps can start Apply now or Restart all
    apps (see Permissions and manifest, below).
  - Redo setup: returns to Setup at its first step, after a confirmation. With wireless debugging it forgets the
    pairing; with Shizuku there is none, and the row and the confirmation say so.
- **Appearance**: style (Material or One UI), dark theme, colour and, in the Material style, palette style, each
  chosen in a bottom sheet. The palette styles keep Material's names, but for CMF, which reads "Material 2026". A
  Samsung phone starts in One UI, and that style starts in blue; Material starts from the wallpaper's colour. A
  change applies at once, in a circle that spreads from the tap across the screen and the sheet alike, and leaves
  the user on the screen they were on. The options and defaults are in `CONTRIBUTING.md`.
- **About**: Report a problem, Source code (on GitHub), the app's licence (Apache-2.0), the libraries' licences on
  their own screen (`LicencesRoute`), and the version last. That list is generated at build time from the
  dependencies (AboutLibraries) and read from the app's resources: no network at run time. Each licence is named by
  its short name where it has one ("Apache 2.0"), and its text is shown in the library's sheet. The texts are kept in
  `app/aboutlibraries/licenses/`, since the plugin runs offline and fetches none: a dependency under a licence not
  there yet shows a link to it instead, until its text is added.
  Report a problem opens the browser on a new GitHub issue, started from the form in
  `.github/ISSUE_TEMPLATE/bug_report.yml`. Nothing is filled in for the user and nothing is sent by the app.
- In Material a row's value is at its end; above a font scale of 1.15 it goes under the row's name instead, as One
  UI always has it, so it is never cut short.

### Log (`LogRoute`)

- The last 50 runs, newest first: time, trigger, result, duration. With no runs, a centred message says what will
  appear and leads back to apply; a stored log that cannot be read says so.
- Opening a run shows its steps in words, each with success or the error and how long it took. Under them, for
  whoever is looking for a fault: how long it took to connect and read the phone, what the four surfaces ran on
  before and after, and how many open apps were left on the other renderer. A run that changed nothing can be
  opened too, for what it found.
- A command that failed leaves its own words in the log: its exit status and the first line of its error
  output, cut to 200 characters.
- **Share** exports the log as a `.log` file: a header with the app version, the phone's model and Android
  version, then each run on a line with its steps marked done or failed beneath, and the same details as the
  screen. It never contains the ADB key, and none of the user's apps by name: steps are recorded with counts
  ("Stop 612 apps"), and the commands that go through the user's apps never fail by their status, so a failed
  command's words can only name System UI, the home app or the keyboard. There is nothing to warn about before
  sharing.
- Runs logged by an earlier version have none of these details and show as they did.

### After a crash (`CrashActivity`)

- When the app crashes with one of its screens showing, it shows a screen of its own in place of Android's
  "Burkan keeps stopping": that it stopped, the report, **Share the report** and **Report a problem**.
- The report is text: the app's version, the phone's maker, model and Android version, the time, the thread, and
  the error with its causes. It is shared as text through Android's share sheet, to be pasted into an issue.
- A crash with no screen showing, such as during a run after a restart, is left to Android, which handles it
  without a dialog. An app may not open a screen from the background. The report is still written.
- The screen runs in a process of its own (`:crash`), which starts after the app's has died. It is built by its
  own constructor, not by the graph, and reads no setting, so it opens even when what crashed is the graph or the
  settings. It is therefore drawn in the look a fresh install has on that phone, not the one the user chose. The
  handler is not installed in that process: a crash there goes to Android.
- One report is kept, the newest, in the cache (`feature/crash/CrashReport.kt`).

## How it is built

The code structure and conventions are in `CONTRIBUTING.md`. These are the parts specific to this app.

### Features

| Feature | Holds |
|---|---|
| `welcome` | The Welcome screen and its drawing of a phone redrawing |
| `setup` | The choice of connection, the checklist screen and its step checks, and the First apply |
| `connection` | Key pair, the pairing notification service, pairing, connecting, enabling wireless debugging; the ADB client; the Shizuku client and its shell service; choosing between the two |
| `apply` | The light and full flows, the output parsers, state capture and restore, the boot receiver and service |
| `status` | Reading renderer state; the Home screen |
| `settings` | The Settings screen and its storage |
| `log` | Run history and the Log screen |
| `crash` | The uncaught-exception handler, the report and the screen that shows it |

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
- Every command has a timeout. A `dumpsys` gets 20 seconds; the bulk stop gets three minutes. Opening the stream
  counts towards it: libadb waits for the daemon to answer an open with no limit of its own.
- `ShellCommands` is the one place commands are built: a function per command, taking validated types
  (`PackageName`, not `String`). Quoting lives there and nowhere else.

### Flows are plans

A flow is a function that returns the list of steps, and a runner that executes them:

- `LightApplyPlan` and `FullApplyPlan` produce `List<ApplyStep>` from inputs (package list, exclusions, wallpaper
  package, keyboard package).
- A plan has a target, the renderer it leaves the phone on. Towards OpenGL the light plan is the way back. Given
  the set of open apps that are behind (`OpenAppsReader`), the full plan restarts those alone.
- `ApplyRunner` executes steps through `ShellExecutor`, records each result in the log, and guarantees the
  restore step runs when Restart all apps fails or is cancelled.
- What Restart all apps has to put back is kept in the phone's state file from before its first step until it is back
  (`CapturedSettings`). Where the shell no longer answers, the app writes a value back itself, which
  `WRITE_SECURE_SETTINGS` lets it do. A run whose process died under it is undone the same way, the next time a run
  starts or Home opens, if that is within an hour of when the values were kept. Later than that, what the phone
  holds is taken as the user's own, and the kept values are dropped unwritten.
- Tests assert on the plan (exact commands, in order) without running anything, and on the runner with a fake
  shell that fails at a chosen step.
- A process that has to be crashed to restart, System UI and the keyboard, is named by its process ID, found by
  its exact name. Named by package, `am crash` takes whichever of the package's processes it comes to first,
  and System UI has helpers (`docs/device-notes.md`, section 2).

### Connection

- **Key pair**: 2048-bit RSA and a self-signed certificate, generated on first use, stored in KSafe.
- **Pairing**: a foreground service posts a notification with a text-reply action. It discovers
  `_adb-tls-pairing._tcp` on the local host, pairs with the code the user types, and reports the result in the
  notification and to the Setup screen.
- **Connecting**: discover `_adb-tls-connect._tcp`, connect to `127.0.0.1` on that port, with the device's Wi-Fi
  address as fallback. Retry discovery for up to 30 seconds after wireless debugging is switched on.
- **Local network access**: on Android 17, an app that targets it may look for services on the network only with
  `ACCESS_LOCAL_NETWORK`, a runtime permission in the Nearby devices group. Without it, the system answers a
  discovery with its own device picker, over whatever is on screen, and opening the picker closes Settings' pairing
  dialog. So nothing is looked for without the permission: a connection or a pairing fails at once with its own
  error instead. Setup asks for it when Pair is started; Home, which on a phone set up before Android 17 is where
  the missing permission first shows, offers Allow in place of Try again.
- **Enabling wireless debugging**: write `Settings.Global` `adb_wifi_enabled` through the content resolver. That the
  app switched it on is written to the phone's state file first, and cleared once it is off again or the phone
  restarts: a process that dies while the switch is on leaves it to the next connection, which switches it off
  when it closes, rather than taking it for the user's. Read
  it back after two seconds: if it is 0 again, the system refused (no Wi-Fi, or network not trusted) and the
  result is a specific error, not a retry loop. A run cancelled in those two seconds switches it off again.
  "On Wi-Fi" means connected to a Wi-Fi network, whether or not the phone reaches the internet through it: for a
  moment after Wi-Fi connects, and for as long as it has no internet behind it, that is still mobile data.
- **One connection at a time**, owned by a single class, behind a mutex. A run holds it from start to finish.
- **A lost connection is picked up again.** adbd restarts on every keyguard change, on a new port. The shell a block
  gets waits a second, finds the port again and reconnects, for up to 30 seconds. A command that never reached the
  daemon is sent again; one that lost its connection part way is sent again only if repeating it is harmless, which
  is every command except `am crash`.
- **A connection is kept for 10 seconds** after a block, and a block in that time reuses it: every new connection
  raises the system's "Wireless debugging connected" notification. A run releases it as soon as it ends, and so
  does Home when it is left.

### Shizuku

`SettingsStorage.shellBackend` is `WirelessDebugging`, the default, or `Shizuku`. `BackendShellAccess` is the one
`ShellAccess` the rest of the app sees: it reads the choice each time a block starts and hands the block to
`AdbShellAccess` or `ShizukuShellAccess`. A run keeps the one it began with; `release()` lets go of both.

- **A shell is a user service.** `Shizuku#newProcess` is private and going away, so `ShizukuClient` binds
  `ShellService`, a class of this app that Shizuku starts in a process of its own, as the user Shizuku runs as. It
  is not an app's process: no application, no graph, nothing of the rest of the app (`IShellService.aidl` is the
  whole interface). It runs `sh -c <line>`, kills a command that outlives its timeout, and copies stdout and stderr
  into two pipes the app passes in, since a `dumpsys` can print more than a Binder transaction carries. It
  answers with the exit code only. `ShizukuClient` is a `ShellExecutor`, so everything above it is the code that
  already ran over ADB, `ReconnectingShellExecutor` included.
- **Root is allowed.** Shizuku started with root, or Sui, runs the shell as root rather than `shell`. The commands
  are the same fixed set either way, so Burkan uses it as it is and logs the uid when the shell starts. Only a
  Shizuku running as `shell` has been tried on a phone.
- **Nothing to switch on or off.** `ShizukuShellAccess` checks that Shizuku is running and has allowed Burkan,
  binds the shell, and keeps it for 10 seconds after a block as `AdbShellAccess` keeps its connection; Shizuku
  then ends the shell's process. There is no Wi-Fi, no discovery, no `adb_wifi_enabled` and no notification.
- **Shizuku is waited for after a restart, and only then.** A run takes a `patience`: the run after a restart is
  given two minutes for Shizuku to start itself, polling for its binder; every other run, the Setup steps and
  Home included, finds out at once that it is not running. Without Shizuku the run fails with its own error, not
  with the wait for Wi-Fi: `AutoApply` does not wait for Wi-Fi when Shizuku is the way to a shell.
- **Shizuku has to be there before the app's process.** Shizuku 13.6 hands its binder to an app only when the
  app's uid becomes active: its process starts, or it comes to the front. A server started while the app's process
  already runs, as during the wait above, never reaches it, so the wait ends in "Shizuku is not running" (device
  notes, section 16). Ways round it were tried and dropped: a process of the app's own started while waiting is not
  seen, since the uid is already active, and Shizuku's app guards the binder it holds with
  `INTERACT_ACROSS_USERS_FULL`. The wait is kept for a Shizuku that sends its binder to running apps when it starts,
  as its source says it should. The README tells the user that Shizuku has to be up first.
- **A lost shell is started again.** Shizuku restarting, or ending the shell, makes the next command fail; the
  reconnection waits up to 30 seconds for Shizuku, and binds the shell afresh. For the reason above, a restarted
  Shizuku is seen only if its binder reaches the app. One reconnection that fails ends the shell for the rest of
  the run: `ReconnectingShellExecutor` fails every later command at once, so the settings go back from the app
  without each waiting out another 30 seconds.
- **Shizuku's own app is never force-stopped** by a run while Shizuku is the way to a shell: a stopped app gets
  no `BOOT_COMPLETED`, and its start on boot would not run. It is kept like an app the user excluded. The app is
  the package that defines `moe.shizuku.manager.permission.API_V23`, so a fork under another name is found too;
  Sui has no app, and nothing extra is kept.
- **Setup has its own steps.** Notifications, Allow Shizuku (running, and Burkan allowed), Finishing setup,
  Battery: no Developer options, wireless debugging or pairing. The permission is still granted, so that a value a
  Restart all apps changes can be put back when the shell is gone. Changing the choice in Settings asks first, keeps the
  steps the two lists start with (the notifications) and marks setup incomplete, so that Setup comes back for the
  rest. The pairing is kept for the way back. Setup's own screen has a button under the checklist, "Use Shizuku
  instead" or, on Shizuku's steps, "Use wireless debugging instead", which does the same without asking: setup
  does not reach Settings, so a choice made there by mistake could not be undone. `ShellBackendSwitcher` is the
  one place the change is made.
- **The binder comes through a provider.** `ShizukuProvider` is in the manifest, always enabled: Shizuku hands the
  binder to an app through it when the app's process starts, and when Shizuku starts. With wireless debugging
  chosen it does nothing.

### After boot

1. `BootReceiver` (`BOOT_COMPLETED`) starts `ApplyService` if "apply automatically" is on and setup is complete.
2. `ApplyService` is a foreground service (`specialUse`) with a low-importance notification: "Applying Vulkan".
   Manual runs use it too: Restart all apps relaunches other apps over Burkan, and a cached process can be frozen
   before its restore steps run.
3. If the property is already `skiavk` and SystemUI reports Vulkan, stop: nothing to do.
4. Without Wi-Fi, do not start the service: leave the Home card saying it is waiting, and register a network
   callback (with a `PendingIntent`, so it outlives the process) that starts the run when Wi-Fi connects later in
   the same boot. The callback covers the rest of the boot without holding a
   foreground service. A network the system refused wireless debugging on
   is remembered and not tried again until a different network connects. If Android does not let the callback
   start the service from the background, a notification with an "Apply" action starts it instead. Android tells
   such a callback of one network, the one already up when there is one, and then lets go of it. So while the app
   waits, an alarm registers the callback again about every quarter of an hour; it does not wake the phone, and
   waits for it to be awake. A different network is noticed at once when the phone was off Wi-Fi in between, and
   when the callback is next registered otherwise. With Shizuku as the shell this step is skipped: the service
   starts at once, and the run waits for Shizuku itself.
5. Enable wireless debugging, connect, run Apply now without System UI, verify, disable wireless debugging
   if the app enabled it, write the log entry. The user has just unlocked the phone, and restarting System UI would
   lock it again, so if System UI is not on Vulkan the service stays in the foreground, with a notification saying
   so, until the phone next locks. Then it runs again, logged as "System UI at lock", with a wake lock held while
   the screen is off. Locking restarts adbd too, so the run connects no earlier than 3 seconds after the lock, a
   wait that overlaps the one for switching wireless debugging on. It reads only the property and System UI.
   Immediately before `am crash` it checks that the keyguard is still locked. If the user has unlocked meanwhile,
   it leaves System UI alone, disconnects, switches wireless debugging back, logs "Left for the next lock" and waits
   for the next lock. Home says System UI will switch at the next lock. A manual apply meanwhile replaces the wait.
   A phone whose screen lock is None never shows the keyguard, so for it, and for one set to Swipe, the screen
   going off is the lock: the run starts then, and goes ahead while the screen stays off.
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
`ACCESS_LOCAL_NETWORK` (mDNS on Android 17; asked for at runtime, see Connection),
`RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` (System UI's restart once the screen is off), `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`,
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `QUERY_ALL_PACKAGES` (the exclusions picker),
`WRITE_SECURE_SETTINGS` (declared; granted over ADB) and `PACKAGE_USAGE_STATS` (declared; allowed over ADB when a
run is limited to recent apps). `moe.shizuku.manager.permission.API_V23` comes from Shizuku's provider library, and
the provider itself is declared, exported, behind `INTERACT_ACROSS_USERS_FULL`, which only Shizuku's server holds.

A `FileProvider` hands the exported log, written to the cache, to the app it is shared with; nothing else is
reachable through it.

- A run nobody confirmed on the screen (`RunTrigger.External`) restarts System UI only
  under a locked phone. Otherwise it leaves System UI for the next lock, as the run after a restart does.
- Another app starts a run with a broadcast to `TriggerReceiver`: an apply
  (`io.github.barqallayl.burkan.action.APPLY`), a restart of the open apps that are behind
  (`…action.RESTART_OPEN_APPS`) or of every app (`…action.RESTART_ALL`). It is how Tasker and the like automate
  the app. The receiver is exported: any app on the phone can send one, and it is acted on. That was decided
  knowingly, so that automation needs nothing switched on. The runs are these three and nothing else, they take
  no arguments, and none locks a phone in use. Going back to OpenGL is not offered this way. Two things bound
  it: Settings has a switch, "Let other apps start runs", on by default, that turns the broadcasts off; and a
  broadcast for a run that was started from outside and ended less than a minute ago is passed over, so no app
  can keep the phone's apps restarting. The wait is for the same run only: an apply followed by a restart of the
  open apps is not held up.
- The settings Restart all apps puts back afterwards (auto-rotation, the accessibility services, the two Edge
  settings) are read by the app itself through the settings provider, not over the shell. adb's TLS does not say
  who answered on the port, so what the shell says of them is not trusted with a write the app makes under its
  own `WRITE_SECURE_SETTINGS`. The shell is asked only when the app's read gives nothing, never for the
  accessibility services, and a value is written back by the app only if the setting could hold it.
- Every run that ends is announced with a broadcast, `io.github.barqallayl.burkan.action.RUN_FINISHED`, so an
  automation can react to how it went. Its extras are the names the log uses: `kind` (`Light`, `Full`,
  `OpenApps`, `Revert`), `trigger` (`Manual`, `Boot`, `AtLock`, `External`), `result` (`Succeeded`,
  `AlreadyApplied`, `Postponed`, `Failed`, `Cancelled`) and, when it failed, `error`. It names no app and says
  nothing of the phone, and reaches only receivers registered while their app runs. It is sent to no app in
  particular and needs no permission to hear, so any app that listens learns that Burkan is installed and when
  it ran.
- A Quick Settings tile, "Vulkan", says whether Vulkan has been applied since the phone started, and a tap
  applies it. Its state comes from the log, not from the phone: asking the phone needs a connection, too much
  for a tile redrawn whenever the shade opens. A run counts only if it began after the phone started, and going
  back to OpenGL is the last word until the next apply. A long press opens the app.
- A run asked for by the tile, a shortcut or another app that Android will not let start from the background,
  which it may refuse while the app is held to battery optimisation, is not dropped: a notification, "Burkan could
  not start", offers to start it.
- When the apply after a restart is done, System UI included, a notification says so, on a channel of its own
  that can be turned off in the phone's settings without losing the notice of a run that failed. When apps that
  started before the run are still on OpenGL, it says how many and offers to restart them, as Home's sheet does.
- The app has two shortcuts on its icon, "Apply now" and "Restart all apps", the second being the full flow:
  Vulkan applied and every app restarted. Each opens `ShortcutActivity`, which starts the run and closes without
  showing anything: its window is translucent and empty, and it finishes once it has read from the phone's state
  file, off the main thread, whether setup is complete. There is no schedule in the app: a routine that runs one of
  these at night does that. That activity is not exported: a launcher starts a shortcut in the app's own name.
- The main activity is `singleTask`: started again from the shell at the end of Restart all apps, it comes to the
  front as it is, where a second copy would have lost the run it was showing.
- `minSdk` 33: the S23 shipped with Android 13, and it removes every pre-13 branch. `targetSdk` and `compileSdk`
  37.
- Backup: exclude the ADB key and the pairing state from cloud backup and device transfer.

## Decisions

- **Shizuku is a choice, not the default.** On an unrooted phone Shizuku is itself started over wireless debugging
  after every restart, so it moves the dependency instead of removing it, and adds an app. It earns its place for
  a user who already keeps it running, or runs a fork that starts it by itself. The default, and everything that
  was measured, is wireless debugging.
- **`minSdk` 33.** The S23 shipped with Android 13, and it removes every pre-13 branch.
- **English only.** Two styles over the same shared components, Material 3 Expressive and One UI, both in the
  phone's own font. The style, the theme and the colour are chosen in Settings; the text size is the phone's.
- **Apache-2.0.** `libadb-android`, offered under Apache-2.0 or GPL-3.0-or-later, is used under Apache-2.0. Its
  pairing helper `spake2-java` is LGPL-3.0, which a differently licensed app may link to as long as the library
  can be replaced; the app being open source, anyone can rebuild it with their own copy.
- **Distributed outside the Play Store.** `QUERY_ALL_PACKAGES`, the `specialUse` foreground service and a
  permission granted over ADB are a poor fit for it.
- **Released from a tag, on GitHub and F-Droid.** `./gradlew :app:githubRelease` tags a commit, builds it and
  publishes the APK, from a computer or, once the tag is pushed by hand, from GitHub Actions; F-Droid rebuilds the
  same tag and ships that APK only if its own build is identical, so the build names its NDK and carries 64-bit ARM
  code only. [`f-droid.md`](f-droid.md) has the detail.
