# Device notes

What the app has to do on the phone, command by command, and what was measured on a real device. This is the
reference for anything that runs as the `shell` user.

**Test device:** Galaxy S23 (SM-S911B), Android 16, One UI, Secure Folder set up, a live wallpaper selected.
Measured on 1 October 2026 with the macOS script from
[s23-vulkan-support](https://github.com/Ameen-Sha-Cheerangan/s23-vulkan-support), which this app replaces on the
phone itself, and on 2 October 2026 with Burkan on the phone alone, before and after a reboot.

Each statement below is marked:

- **Measured** — observed on the test device.
- **Expected** — from Android's source or documentation as remembered, or from another project's description.
  Not yet seen on the test device. Verify before relying on it, then change the mark.

When a device result comes in, update this file in the same change as the code.

---

## 1. The renderer property

| Fact | Status |
|---|---|
| `setprop debug.hwui.renderer skiavk` selects Vulkan for every process started afterwards | Measured |
| The property is empty after a reboot, so the device is back on OpenGL | Measured |
| Only the `shell` user (or root) can set it; an app cannot | Expected |
| A process keeps the renderer it started with. Setting the property changes nothing until the process restarts | Measured |
| `getprop debug.hwui.renderer` returns `skiavk` when set and an empty line when not | Measured |

**What a process is actually using** is reported by `dumpsys gfxinfo <package>`:

```
Pipeline=Skia (Vulkan)
Pipeline=Skia (OpenGL)
```

Take the first line containing `Pipeline=`. The property only says what *new* processes will get; this line says
what a running one has. The status screen must use this, not the property, for SystemUI, the launcher and the
keyboard. (Measured.)

---

## 2. Restarting processes

This is where the original scripts were wrong, so read it carefully.

| Command | What it did on the test device | Status |
|---|---|---|
| `am force-stop <package>` on an ordinary app | Process killed. It starts on Vulkan next time it is opened | Measured |
| `am force-stop com.android.systemui` | **Nothing.** Same process ID before and after; still `Skia (OpenGL)` | Measured |
| `am crash com.android.systemui` | Restarts SystemUI. On a phone with a secure lock, the lock screen appears (section 11) | Measured |
| `am force-stop com.sec.android.app.launcher`, then launching it | Launcher restarted on `Skia (Vulkan)` | Measured |
| Leaving the keyboard alone | Samsung Keyboard stayed on `Skia (OpenGL)` | Measured |
| `am crash <keyboard package>` | Restarts Samsung Keyboard, which stayed the default input method | Measured |

- **SystemUI is a persistent system process and `force-stop` skips it.** It must be crashed. The status bar,
  notification shade, quick settings and lock screen stay on OpenGL otherwise.
- **Never `force-stop` an input method.** The scripts exclude keyboards from the bulk stop to avoid resetting the
  default keyboard. Restart the current one with `am crash` instead, and verify that on the device.
- **Force-stopping an app disables its accessibility service** and can reset auto-rotation and Edge panel
  settings. Hence section 4.
- After a restart, check the result with `dumpsys gfxinfo` rather than assuming it.

**The launcher is whatever home app the user chose**, not necessarily Samsung's. Ask the system:

```
cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

prints a `priority=…` line and then the activity that answers, `package/class`:

```
priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true
com.sec.android.app.launcher/com.sec.android.app.launcher.activities.LauncherActivity
```

When no home app is set as the default, that last line is the system's chooser,
`android/com.android.internal.app.ResolverActivity`, which is no home app. Then ask for the home role's holder:

```
cmd role get-role-holders android.app.role.HOME
```

which prints the package (several are separated by `;`). (Both formats Expected, from Android's source.)

Restart it by stopping it, waiting about two seconds, and starting home:

```
am force-stop <home package>; sleep 2; am start -a android.intent.action.MAIN -c android.intent.category.HOME
```

Starting home brings the home screen to the front. (Expected.)

**Launching an app** (to bring a widget host or provider back):

```
c=$(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER <package> | tail -n 1)
case "$c" in */*) am start -n "$c" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER;; esac
```

The lookup answers `package/activity`, or a sentence when the package has no launcher activity, which the `case`
passes over. Send `am`'s output to `/dev/null`: it prints a line per package.

Not `am start -a … -c … -p <package>` on the bare intent. Starting an activity that way only matches launcher
activities that also declare `android.intent.category.DEFAULT`, and most do not: on the S23 it started Settings
and Chrome but answered "unable to resolve Intent" for Clock and for this app.

Not `monkey -p <package> -c android.intent.category.LAUNCHER 1`, which the original scripts used. `monkey` unlocks
the screen's rotation as it exits, and that writes `accelerometer_rotation=1`: auto-rotate comes on for a user who
had it off. Confirmed on Android 17 and on the S23: with the setting at 0, one `monkey` launch leaves it at 1 and
a start by name leaves it at 0. The app restored the setting after its run, but a relaunch that outlived its time
limit went on running `monkey` after the restore, and a run that lost its connection never restored at all.

---

## 3. The two flows

### Light apply — the default, and what runs after boot

Right after boot almost nothing with a UI is running, so restarting three things is enough. Everything opened
later starts on Vulkan by itself.

```
setprop debug.hwui.renderer skiavk
am force-stop com.sec.android.app.launcher
am crash <current keyboard package>
am crash com.android.systemui
```

Read each surface's `dumpsys gfxinfo` first and restart only those not on Vulkan yet. System UI goes **last**:
crashing it brings up the lock screen, which restarts adbd and drops the connection (section 11). Then ask
`dumpsys gfxinfo com.android.systemui` until it answers on Vulkan, connecting again as needed. Never crash System UI
twice within 60 seconds: One UI switches off Good Lock modules when System UI crashes repeatedly. (Reported by the
owner; not measured by the app.)

After a reboot the user has just unlocked the phone, so the run leaves System UI for the next time the phone locks,
when nobody sees the lock screen appear.

**Measured:** run by the app on the phone, this left System UI, the launcher and Samsung Keyboard all on
`Skia (Vulkan)`, and Samsung Keyboard stayed the default input method. Crashing System UI showed the lock screen,
which cut the app's own connection (section 11).

Used manually on a phone that has been running for a while, the light apply leaves already-open apps on OpenGL
until each is next cold-started. The status screen must say so rather than report "all on Vulkan".

### Full apply — on demand

The port of the script's full flow, for a phone that has been running on OpenGL for a while.

1. Capture the state in section 4.
2. List packages: `pm list packages` (strip the `package:` prefix).
3. Remove from the list, **matching whole package names**:
   - every installed input method: `ime list -s`, the part before `/`,
   - the current wallpaper's package (section 5),
   - the fixed exclusions below,
   - the user's own exclusions,
   - Burkan itself.
4. Work out which packages are running (section 6), and the widget providers and hosts (section 7), and remember
   them. All of it is read before anything is changed.
5. `setprop debug.hwui.renderer skiavk`.
6. `am force-stop <package>` for each remaining package.
7. `am force-stop com.sec.android.app.launcher`, wait about two seconds, launch it with `monkey`.
8. Relaunch the remembered running apps and the widget packages with `am start`.
9. `am crash <current keyboard package>`.
10. Restore the state from section 4.
11. `am crash com.android.systemui`, last of all, as in the light apply.

A surface already on Vulkan is skipped: the launcher is then also kept out of the bulk stop.

**Fixed exclusions.** These keep Wi-Fi calling and connectivity working; they come from the original script:

```
com.samsung.android.bluelightfilter
com.samsung.android.wcmurlsnetworkstack
com.sec.unifiedwfc
com.samsung.android.net.wifi.wifiguider
com.sec.imsservice
com.samsung.ims.smk
com.sec.epdg
com.samsung.android.networkstack
com.samsung.android.networkdiagnostic
com.samsung.android.ConnectivityOverlay
com.netflix.mediaclient
```

and any package whose name contains `providers.media.module` (the media provider module; the script matched it
with the substring `ia.mo`).

**Measured on the full flow:** about 600 packages are force-stopped and about 110 relaunched; the run took 73
seconds over USB. Sending all the `force-stop` commands as one shell line of roughly 30 KB worked.

**Errors that are expected.** On a phone with Secure Folder, `pm list packages` prints

```
Error: java.lang.SecurityException: Shell does not have permission to access user 150
```

on stderr and still lists user 0's packages on stdout. Treat stderr from `pm` and from individual `force-stop`
commands as noise. What matters is whether the connection survived the whole list: end the batch with `true` and
use the exit status of the batch, not of the last `force-stop`. (Measured.)

---

## 4. State to capture and restore

Read these before a full apply and write them back afterwards, **also when the run fails or is cancelled**:

| Namespace | Key | Why |
|---|---|---|
| `system` | `accelerometer_rotation` | Auto-rotation gets reset |
| `secure` | `enabled_accessibility_services` | Force-stopped apps lose their accessibility service |
| `secure` | `edge_enable` | Edge panels get reset |
| `secure` | `edge_panels_enabled` | Edge panels get reset |

```
settings get <namespace> <key>
settings put <namespace> <key> '<value>'
```

Rules, each of which was a real bug in the scripts:

- **`settings get` prints `null` for an unset key.** Writing that back stores the literal string `null`. Skip the
  restore when the captured value is `null` or empty. (Measured: the test device had `edge_panels_enabled=null`
  stored by earlier runs.)
- **The device shell re-parses the command line.** An accessibility service whose class name contains `$`
  (`com.example/com.example.Outer$Inner`) is silently truncated, and a `;` runs the rest as a command. Wrap the
  value in single quotes and escape embedded single quotes as `'\''`. (Measured with `echo`.)
- **An empty argument is dropped**, so `settings put global some_key ""` reaches the device with no value and
  fails. Use `settings delete` to clear a key. (Measured.)
- Read the value back after writing and retry a few times with a one-second pause; the scripts needed this for
  rotation and accessibility.

The accessibility value is a `:`-separated list of `package/class` components.

The light apply force-stops only the launcher, so it does not need the capture and restore.

---

## 5. Finding the current wallpaper

`dumpsys wallpaper` begins like this on the test device:

```
mDefaultWallpaperComponent=ComponentInfo{com.android.systemui/com.android.systemui.wallpapers.ImageWallpaper}
mImageWallpaper=ComponentInfo{com.android.systemui/com.android.systemui.wallpapers.ImageWallpaper}
mLastWallpaper state:
 User 0: id=485
    mInfo.component=ComponentInfo{com.samsung.android.wallpaper.live/com.samsung.android.wallpaper.live.layered.LayeredWallpaperService}
...
  mWallpaperComponent=ComponentInfo{com.samsung.android.wallpaper.live/com.samsung.android.wallpaper.live.layered.LayeredWallpaperService}
```

**Take the first line that contains `mWallpaperComponent=`**, and the package is the part between `{` and `/`.
Taking the first `ComponentInfo{` in the dump gives `mDefaultWallpaperComponent`, which is always SystemUI's
image wallpaper — that was a bug in the macOS script. (Measured.)

With the wallpaper package excluded, the live wallpaper stayed selected after a full apply. Its process still
restarted (it is killed as a dependant of something else) and came back on Vulkan. (Measured.)

---

## 6. Finding running apps

`dumpsys activity processes` is about 1 MB. A package counts as running when its full name appears in it as a
whole token. Lines look like:

```
  *APP* UID 10137 ProcessRecord{5420e3c 6845:com.sec.android.app.launcher/u0a137}
      - ConnectionRecord{99df603 u0 CR com.android.systemui/.keyguard.KeyguardService:@ac976b2 flags=0x1}
```

Split each line on anything that is not `[A-Za-z0-9_.]` and look the tokens up in the package set. A substring
search matches `com.android.settings` inside `com.android.settings.intelligence`, and needs one pass over the
dump per package instead of one in total. (Measured: 117 substring matches against 110 exact ones.)

---

## 7. Finding widget providers and hosts

`dumpsys appwidget` has top-level sections `Providers:`, `Widgets:`, `Hosts:`, `Grants:`, each starting in
column 0.

Providers in use — inside the `Widgets:` section, lines containing `provider=`:

```
Widgets:
  [0] id=190
    host=HostId{user:0, app:10137, hostId:1024, pkg:com.sec.android.app.launcher}
    provider=ProviderId{user:0, app:10231, cmp:ComponentInfo{com.google.android.googlequicksearchbox/com.google.android.googlequicksearchbox.SearchWidgetProvider}}
```

The package is between `ComponentInfo{` and `/`.

Hosts — inside the `Hosts:` section, lines containing `hostId=HostId`:

```
Hosts:
  [0] hostId=HostId{user:0, app:1000, hostId:2167, pkg:com.samsung.android.app.dressroom}
  [1] hostId=HostId{user:0, app:10137, hostId:1024, pkg:com.sec.android.app.launcher}
```

The package is between `pkg:` and `}`.

---

## 8. Input methods

`ime list -s` prints one component per line:

```
com.samsung.android.honeyboard/.service.HoneyBoardService
com.google.android.tts/com.google.android.apps.speech.tts.googletts.settings.asr.voiceime.VoiceInputMethodService
```

The package is the part before `/`. All of them are excluded from the bulk stop. The *current* one is
`settings get secure default_input_method` (Expected), again the part before `/`.

**Protecting the default keyboard.** Read `default_input_method` before restarting the keyboard and again a second
after. If it changed, put it back with `ime set '<component>'`, and if that does not take, with
`settings put secure default_input_method '<component>'`. The component can contain `$`, so it is quoted. On the
test device `am crash` left the default alone (section 2); this is the safety net.

---

## 9. Reaching the shell from the app

Partly measured on 2 October 2026: the app paired, connected and ran commands on the phone alone, and after a
reboot it switched wireless debugging on, connected and applied by itself. Which address the connection used was not
recorded.

| Fact | Status |
|---|---|
| Wireless debugging is switched off by every reboot | Measured: after the reboot the app had to switch it on |
| An app holding `WRITE_SECURE_SETTINGS` can switch it on by writing `Settings.Global` key `adb_wifi_enabled` to `1` | Measured: on a trusted Wi-Fi network, after a reboot, with no prompt; and off again after the run |
| The system refuses, and resets the key to `0`, when there is no Wi-Fi connection | Expected |
| On a Wi-Fi network the user has not ticked "Always allow on this network" for, the system shows a confirmation dialog instead of enabling | Expected |
| The connect port changes every time; it is advertised over mDNS as `_adb-tls-connect._tcp` | Expected |
| Pairing uses a six-digit code and a separate port advertised as `_adb-tls-pairing._tcp`, only while the system's pairing dialog is open | Measured: the app found the port and paired with the code typed into its notification |
| Once paired, the app's key stays authorised across reboots | Measured: the run after the reboot connected |
| The daemon is reachable on `127.0.0.1` | Expected — `adb-auto-enable` connects over loopback, with the LAN address as fallback |
| `pm grant <package> android.permission.WRITE_SECURE_SETTINGS` succeeds from the shell on One UI | Measured, over the app's own connection |
| A foreground service of type `specialUse` may be started from a `BOOT_COMPLETED` receiver on Android 15 and later | Measured on Android 16. The types Android 15 forbids there are `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection` and `microphone` |
| adbd accepts `shell,v2,raw:<command>` on a stream opened by libadb-android, and answers with v2 packets ending in the exit code | Measured: the grant and the light apply ran through it |
| libadb-android reports a wrong pairing code as an `IOException` with the message `Exchanging message wasn't successful.`, and a closed pairing dialog as a refused connection | Expected — read from the library's source, version 3.1.1 |
| libadb-android's `AbsAdbConnectionManager.close()` destroys the private key | Read from the library's source; the app only ever calls `disconnect()` |

**The pairing dialog closes when the user leaves the Settings screen**, so the code cannot be typed into the
app's own activity. (Measured: typing the code into the notification from the shade, with the dialog still open,
paired the app.) Shizuku solves this with a notification that has a text-reply action: the user opens the
pairing dialog, pulls down the shade, and types the code into the notification. Do the same.

**The app can grant itself `WRITE_SECURE_SETTINGS`** once it has a shell connection, so first-time setup needs no
computer: pair, connect, then run the `pm grant` above. (Measured.)

**Not yet known:** whether the app connected on `127.0.0.1` or fell back to the Wi-Fi address.

---

## 10. Things that were tried and dropped

- **`game_driver_blacklist`.** The scripts write package names to this global setting to stop certain apps
  crashing. On Android 16 the key does not exist; the live key is `updatable_driver_production_denylist`, and it
  governs the updatable game driver rather than the UI renderer. It is not part of this app. A user who has an
  app that misbehaves on Vulkan has no per-app opt-out; the remedy is a reboot. (Measured that the key is absent;
  the rest is Expected.)
- **Persisting the property.** There is no `persist.` equivalent an unrooted phone can write. Re-applying after
  boot is the only route.

---

## 11. The lock screen restarts adbd

All measured on 2 October 2026.

- **Restarting System UI shows the lock screen** on a phone with a secure lock (PIN, pattern, password or
  biometrics).
- **Every keyguard change restarts adbd**, locking and unlocking alike. `UsbDeviceManager` handles
  `MSG_UPDATE_SCREEN_LOCK` by resetting the USB functions (`setUsbConfig(none)`, then back), and that restarts the
  daemon: a new process, a new wireless debugging TLS port, and every connection dropped.
- **The daemon accepts connections again about one second later**, on the new port.
- **With a USB cable plugged in, locking did not restart adbd** (measured after a reboot, the same day). The
  restarts above were seen with wireless debugging alone. So whether the app's reconnection works on the phone is
  still open: it has to be tried with the cable unplugged.

So `am crash com.android.systemui` cuts the connection that sent it: the lock screen appears, adbd restarts, and
anything the run still had to send fails. When the user unlocks, adbd restarts a second time. The user sees the
phone lock, and the system's "Wireless debugging connected" notification go and come back.

What follows for the app:

- System UI is restarted last, and nothing that matters may depend on the connection after it.
- A lost connection is not the end of a run: wait, find the new port, connect again.
- Every new connection raises the "Wireless debugging connected" notification, so connections are reused rather
  than opened per operation.

---

## 12. After a reboot

Measured on 2 October 2026, rebooting with the USB cable plugged in, on a Wi-Fi network marked "Always allow".

| Fact | Status |
|---|---|
| `BOOT_COMPLETED` reached the app 36 seconds after the first unlock | Measured |
| The boot receiver was allowed to start the foreground service | Measured |
| The app switched wireless debugging on with no prompt | Measured |
| The first phase (set the property, restart the launcher and the keyboard, switch wireless debugging off) took 9 seconds, and Samsung Keyboard stayed the default input method | Measured |
| System UI, left for the next lock, was restarted then; all three surfaces ended on `Skia (Vulkan)`, and wireless debugging was off afterwards | Measured |
| `BOOT_COMPLETED` is also sent, with no reboot, to an app taken out of the stopped state. Since Android 15, a force-stopped app gets it the next time the user opens the app, so that it can register again the pending intents the force-stop cancelled. Installing from Android Studio force-stops the app | Expected — from the Android 15 behaviour changes. It matches a run labelled "After restart" that started twice after an install from Android Studio, with no reboot |
| `Settings.Global.BOOT_COUNT` (`boot_count`) can be read by an app, and goes up by one on every boot | Expected |

**The lock can come undone before System UI restarts.** The restart at the lock took about 7 seconds from the
lock event to `am crash`, made up of:

- waiting for the keyguard, polled once a second;
- 3 seconds for adbd to settle;
- then switching wireless debugging on, with its own 2-second wait;
- then discovery, connecting, the reads and `setprop`.

The user unlocked after 3 seconds, so System UI restarted in front of them and the lock screen came back. So the
app checks that the keyguard is still locked immediately before `am crash`, and otherwise goes back to waiting for
the next lock.

What follows for the app:

- A second `BOOT_COMPLETED` in the same boot is not a restart. The app compares the boot count with the one it last
  handled. If it is the same, it applies nothing and only resumes what this boot was waiting for.
- The run at the lock is logged as its own entry, "System UI at lock", after the "After restart" run it continues.
