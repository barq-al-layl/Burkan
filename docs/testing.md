# Testing on a phone

Unit tests and screenshot tests run on any machine (see `CONTRIBUTING.md`). What follows cannot: it needs a real
Galaxy S23, and an emulator does not stand in for it. A ticked item was observed on an S23 (SM-S911B, Android 16);
the results are recorded in [`device-notes.md`](device-notes.md). A tick marked "(reported in #…)" was reported by
a contributor in that pull request instead, and has not been observed by the maintainer. An open item has not been
checked yet. The last full pass was on One UI 8.5, with a debug build and the USB cable plugged in. The 1.0.0
release build was then tried on an S23 (SM-S911) and an S23 Ultra (SM-S918).

When you check one, tick it here and record what you saw in the device notes.

## Setup and connection

- [x] A fresh install opens on Welcome, and the phone in it redraws once, top to bottom, then rests. With Shizuku's
      app installed, Get started offers the choice of connection, and Back goes from it to Welcome. (Android 17,
      One UI, dark, 11 Nov 2026.)
- [x] Notifications and Finishing setup show their tick and move on by themselves; Developer options and Wireless
      debugging wait for Done. Pairing's two actions lead through pairing from the notification. Setup ends on
      First apply, and Apply now opens Home on the run. (Same pass.)
- [ ] Allowing the battery exemption in its dialog shows the tick and moves on, with no Done.
- [ ] Refusing notifications twice, then tapping Allow, opens Burkan's notification settings; switching them on
      there and coming back moves the step on.
- [ ] On a phone without Shizuku's app, Welcome's Wi-Fi line comes and goes as Wi-Fi is switched from the
      notification shade, without leaving the app.
- [ ] With Shizuku's app installed, backing out of the choice of connection, closing the app and opening it again
      opens on Welcome; Continue on the choice leaves Welcome for good.
- [ ] Setup's back arrow goes back to the choice of connection or to Welcome while getting started, and is not
      there after "Redo setup".
- [ ] Once the last step is done, the First apply has no back arrow, and Back from it does not reach the choice of
      connection or Welcome.
- [ ] After "Redo setup", Finishing setup connects again rather than passing at once, and the step from home to
      Setup and back to Home animates as a change of screen.
- [ ] With the pairing dialog closed while waiting for the code, Open Wireless debugging under the first action
      opens it again.
- [ ] First apply with System UI on OpenGL: the screen locks once, and Home shows System UI on Vulkan after the
      unlock. Later opens Home with nothing applied.
- [ ] Welcome with Android's animations removed: the phone is drawn redrawn, with no movement.
- [ ] Choosing Shizuku on the choice of connection leads to Shizuku's steps.
- [ ] An update from a version with Connect and Permission, part way through setup, lands on the right step.
- [ ] On a phone outside the S23 family, Setup shows the invitation, and Report a problem opens the issue form.
- [x] A fresh install on a Samsung phone opens in the One UI style, in blue.
- [x] A step is looked at only once it is the current one, and waits for Done: notifications already allowed,
      Developer options already on and wireless debugging already on each showed their tick and stayed put.
- [x] Allow raises the system's notification prompt, and the battery step raises "Stop optimising battery usage?".
- [x] Pairing succeeds from the notification, on the phone alone.
- [ ] The app connects on `127.0.0.1`. If only the Wi-Fi address works, record it in the device notes. (It
      connected; which address it used was not recorded.)
- [ ] `echo ok` returns through `ShellExecutor` with exit code 0, and a failing command returns its real code.
- [x] `pm grant … WRITE_SECURE_SETTINGS` succeeds on One UI.
- [x] With the permission, the app switches wireless debugging on and off.
- [ ] What happens on a Wi-Fi network not marked "Always allow", and with Wi-Fi off. Record both.
- [x] After a reboot, the earlier pairing still authorises the app.
- [ ] Pairing works on `127.0.0.1`. If only the Wi-Fi address works, record it.
- [ ] A wrong code shows the wrong-code message. (In the last pass it showed "Pairing failed" instead: the library
      words the failure differently on this phone. The app now knows that wording too; that fix has not been
      seen on the phone yet. Device notes, section 13.)
- [x] After a wrong code the pairing dialog stays open, and the right code typed next pairs.
- [ ] Pairing with the dialog closed shows the dialog-closed message.
- [x] The code can be typed into the notification from the shade while the pairing dialog stays open.
- [ ] `dumpsys activity processes` (about 1 MB) comes back whole through `shell,v2` within its 20 seconds.
- [x] The Developer options button opens Developer options.
- [ ] The About phone button opens About phone on One UI.
- [x] Connect starts by itself when its step is reached, and so does Permission.
- [x] Reinstalled over a restored backup, the app starts setup from its first step, while the log and the
      settings come back: the pairing is not claimed on the strength of a backup.
- [ ] With setup complete, forget Burkan under Wireless debugging's paired devices, then open the app: it leaves
      Home for Setup at the Pair step, and pairing again leads through Connect, Permission and Battery back to
      Home. (Covered by tests only.)
- [ ] Type a code that is not six digits into the notification twice in a row: the reply field stops waiting both
      times. (It used to go on waiting the second time. Seen on an emulator with Android 17; not on a phone.)
- [ ] With wireless debugging off, tap Apply now and then Cancel within two seconds: wireless debugging is off
      again afterwards. (It used to stay on. Covered by a test only.)
- [ ] With wireless debugging off, open Home and force-stop Burkan from Settings within ten seconds, while the
      connection is still kept: wireless debugging stays on until Burkan is opened again, and is off once that
      connection has closed. (It used to stay on until the next restart. Covered by tests only.)
      On Android 17, force-stopped with `am force-stop` in the middle of Restart all apps instead: wireless
      debugging was off about five seconds after Burkan was opened again (device notes, section 12).
- [ ] `settings get global adb_allowed_connection_time` on the S23: with Developer options' "Disable adb
      authorization timeout" off, does Burkan's entry under Paired devices lapse after that long without a
      connection, and does the next run then send setup back to Pair? Home now warns while the timeout is on;
      check that the notice shows, that the switch in Developer options takes it away, and that Android lets the
      app read the key at all (if not, the notice never shows).
      On the S23 with Android 17 the key read 0 over adb: the timeout was switched off, so the lapse could not be
      tried there.
- [x] On Android 17, Start pairing asks for the nearby devices permission first, and allowing it goes on to
      Developer options. Pairing, Connect and Home then bring up no "choose a device" picker. (Device notes,
      section 15.)
- [x] On Android 17, with the permission revoked, Home says Burkan needs nearby devices and its Allow button
      raises the system dialog; allowing it reads the status.
- [ ] On Android 17, refuse the nearby devices permission twice: the third Allow opens Burkan's page in Settings.
- [x] On Android 17, after a restart, the run applies Vulkan with no picker, and switches wireless debugging off
      again at its end.
- [ ] On Wi-Fi that has no internet behind it, with mobile data on: the status reads and Apply now works, and
      neither says the phone is not on Wi-Fi.

## Shizuku as the way to a shell

The code is covered by unit tests with fakes, and none of the items below has been checked on the maintainer's S23
yet. A tick marked "(reported in #6)" was reported working by the contributor of pull request #6 on a Galaxy S23
(SM-S911B, One UI 9) with Shizuku 13.6.0.r1318-thedjchi; device notes, section 16, records it with what is
expected. Use a Shizuku that starts itself after a restart (a fork such as thedjchi's, or Sui) for the restart
checks.

- [x] Settings › Use Shizuku asks first; confirming goes to Setup, with notifications still done and the steps
      Allow Shizuku, Connect, Permission and Battery after it. Wireless debugging's own steps are not listed.
      (reported in #6)
- [x] With Shizuku stopped, the Allow Shizuku step offers Open Shizuku, which opens its app. With it running, the
      step offers Allow, which raises Shizuku's dialog; allowing it ticks the step when Burkan is next in front.
      (reported in #6)
- [ ] After a "deny and don't ask again", the step's own text says where to allow Burkan, and allowing it there
      ticks the step.
- [x] Connect returns from `echo ok` through Shizuku, and Permission grants `WRITE_SECURE_SETTINGS` through it.
      (reported in #6)
- [x] A light and a full apply run end to end through Shizuku, and leave Shizuku's own app running: it is not in
      the list of stopped apps. `dumpsys activity processes` (about 1 MB) comes back whole, through the pipes.
      (reported in #6)
- [x] `ps -A` shows `io.github.barqallayl.burkan:shell` as `shell` while a run is on and for about ten seconds
      after it, and not once it is over. (reported in #6)
      On Android 17 it was gone about four seconds after the run.
- [ ] A command that outlives its timeout is killed in Shizuku's process and the run reports it timed out.
- [ ] A command Shizuku cannot run fails at once with "Shizuku could not run a command", with no 30-second wait
      for a new shell.
- [ ] After a restart, with Shizuku starting itself: the run waits for it (up to two minutes), applies Vulkan, and
      needs no Wi-Fi of its own. Record how long Shizuku took to be there after the first unlock.
      On Android 17, with Shizuku started from its app right after the unlock, it was there 30 seconds before
      `BOOT_COMPLETED` reached Burkan, and the run went through it (device notes, section 16). Shizuku started
      while the run already waits fails: on two reboots the run waited its two minutes and failed with "Shizuku
      is not running". This is now documented as a limit rather than fixed (README, architecture).
- [x] After a restart with Shizuku not starting: the run fails with "Shizuku is not running" after two minutes,
      and the notification opens the app. (reported in #6)
- [x] Shizuku survives System UI's restart and the lock that follows (adbd restarts on every keyguard change,
      device notes, section 11); the run at the lock finishes through Shizuku, or reconnects within 30 seconds.
      (reported in #6)
- [x] Switching back to wireless debugging brings Setup back with the pairing still counted, and the next run goes
      through wireless debugging as before. (reported in #6)
- [x] A release build: R8 keeps `ShellService` and its stub, and the shell starts. (reported in #6)
- [x] With wireless debugging chosen, which is the default, nothing about Shizuku shows anywhere but the Settings
      row, and an app with Shizuku not installed behaves as before. (reported in #6)
- [ ] A full apply with a Shizuku fork under another package name, or with Sui, keeps the right app running:
      `dumpsys package <app> | grep stopped` shows `stopped=false` afterwards, and with Sui nothing extra is kept.
- [ ] `adb logcat` shows "Burkan's shell in Shizuku is up, as uid …" when the shell starts: 2000 when Shizuku
      was started over adb, 0 with root.
      2000 seen on Android 17, with Shizuku started over adb; root not tried.

## Applying and status

- [x] Light apply: SystemUI, launcher and keyboard all report `Skia (Vulkan)` afterwards.
- [x] Apply now with everything already on Vulkan starts at once, without the question about the lock, lists
      its two steps and ends "Already applied".
- [x] `am crash` on Samsung Keyboard leaves it as the default keyboard.
- [ ] Typing still works after the keyboard restart.
- [x] Full apply: auto-rotation, accessibility services, the Edge panel setting and the default keyboard are
      unchanged afterwards. (42 seconds for 605 apps stopped and 106 reopened.)
- [ ] Full apply: the live wallpaper is still selected, and Wi-Fi calling is still registered.
- [x] Restart all apps limited to the 30 most recent stops 30 apps and takes about 7 seconds.
- [x] Cancelling a full apply midway restores the settings: cancelled while stopping apps, the three "put back"
      steps still ran and the settings read as before.
- [x] Burkan itself survives the full apply and reports the result.
- [x] After Restart all apps, Burkan is the app left in front, with one copy of its screen and the whole list of
      the run's steps.
- [x] The question before Restart all apps mentions the lock only when System UI is to be restarted.
- [ ] Home-screen widgets work after a full apply. The app reads the widget providers and hosts before stopping
      anything, where the script read them after; check that nothing is missed.
- [ ] What `am crash` exits with for a package that is not running. (0 on an Android 14 emulator, which also
      answers `dumpsys gfxinfo` for it with "No process found". A keyboard that reports nothing is no longer
      restarted at all; check that a keyboard not yet shown after a restart comes up on Vulkan.)
- [ ] Full apply with `am stop-app`: every app it stops is reopened on Vulkan, and an alarm set in Clock beforehand
      is still listed by `dumpsys alarm` afterwards, and rings. (Seen on an Android 14 emulator only: `stop-app`
      kept Clock's alarm where `force-stop` lost it. Device notes, section 2.) Also whether the apps it stops keep
      their notifications, and whether accessibility services still need putting back.
      On Android 17: Clock, stopped on its own with `stop-app`, kept its alarm, and so did every app with alarms
      that a full apply stopped; all open apps but the live wallpaper and `system` ended on Vulkan. Whether the
      alarm rings, and the notifications, are still to check.
- [ ] The confirmation before Restart all apps reads true to what happens: every app closed, and those that were
      open or show a widget opened again.
- [x] What `pm list packages` exits with on a phone with Secure Folder. (0, with an error about the other user
      printed besides the list. Device notes, section 13.)
- [x] After an apply, locking Secure Folder and unlocking it again brings its screen and its apps up on Vulkan.
      (Android 17, 12 Nov 2026, My Files. Device notes, section 17.)
- [x] During a full apply, with other apps relaunched over Burkan, the run carries on to the restore steps and
      the log records it.
- [x] Cancel from Home stops the run and the settings are put back.
- [ ] Cancel from the notification does the same.
- [ ] Switch Wi-Fi off in the middle of Restart all apps: auto-rotation, the accessibility services and the Edge
      panel setting are still put back, by the app itself, and the log shows their steps as done. Check
      `edge_enable` in particular: it is Samsung's own key, and whether the app may write it is expected, not
      measured (device notes, section 4).
- [ ] Force-stop Burkan from Settings in the middle of Restart all apps, then open it again: the same settings
      are put back as Home opens.
      On Android 17 the kept settings were put back and cleared, but none had been changed on that phone, so
      the values themselves showed nothing.
- [ ] Opening Home with wireless debugging off refreshes the status, and wireless debugging is off again after.
- [x] In One UI, a run shows as a list of steps in place of the surfaces, each ticked as it ends, and Done puts
      the surfaces back.
- [ ] A run that fails marks the step that failed. (Seen only on an emulator.)

- [ ] With Vulkan active and apps left behind, the docked button (and One UI's first button) reads "Restart N
      apps" and opens the sheet of those apps; with nothing left behind, Apply now is tonal (grey in One UI).
- [ ] With wireless debugging off, or no Wi-Fi, the status card says why and has Try again; the rows say Unknown,
      Apply now and Restart all apps are unavailable, and no other card repeats the reason.
- [ ] After a restart that left System UI for the next lock, the headline reads "Waiting for the lock" with a lock
      symbol, the notice about the next lock is not shown, and locking then unlocking turns it into "Vulkan is
      active".
- [ ] During Restart all apps, the status card says "Closing N apps" and "Opening N apps again", the notification
      says the same, and the Material rows say "Changing".
- [ ] TalkBack reads a change of status once, headline and note together.

## After a restart

- [x] Reboot on a trusted Wi-Fi network: Vulkan is applied with no interaction, and wireless debugging is off
      again afterwards.
- [x] Turn Developer options off after setup: opening the app reads the status, and a reboot applies Vulkan.
      (S23, 24 Oct 2026; device notes, section 9.)
- [ ] Reboot with Wi-Fi off, then switch Wi-Fi on later: it applies then.
- [ ] The same with mobile data on: it applies as Wi-Fi connects, and does not fail with "not on Wi-Fi" in the
      moment before Wi-Fi becomes the phone's way to the internet.
- [ ] Reboot on an untrusted network: the app explains instead of failing silently.
- [x] How long after unlock it finishes. Record it. (`BOOT_COMPLETED` 36 seconds after the first unlock, then
      9 seconds for the first phase; System UI at the next lock. Device notes, section 12.)
- [ ] Battery optimisation left on: does the boot run still happen?
- [ ] Wi-Fi connecting after boot: does the network callback start the foreground service from the background, or
      does it fall back to the "Wi-Fi is connected" notification? Record which, with and without the battery
      exemption.
- [ ] On a refused network, the notification explains and the Home card shows; connecting to a trusted network
      afterwards applies by itself: at once when the phone was off Wi-Fi in between, and otherwise within about a
      quarter of an hour of the phone being awake. Record how long it took. (The wait used to end with the first
      network it was told of. Expected from Android's source, device notes section 12; not seen on a phone.)
- [ ] While it waits, `dumpsys alarm` lists one alarm of Burkan's that does not wake the phone, and none once it
      has applied or the automatic apply is switched off.
- [ ] An automatic run when Vulkan is already active logs "Already applied" and restarts nothing.
- [ ] After a restart, with System UI left for the lock, tap Restart them on Home's open apps: System UI is still
      restarted at the next lock. (It used not to be. Covered by tests only.)
- [ ] With the screen lock set to None: the run after a restart restarts System UI once the screen goes off, and
      the "waiting for the lock" notification goes. A tile, shortcut or Tasker run with the screen on leaves
      System UI for the screen going off. (It used to wait for ever. Expected from Android's documentation.)
- [ ] With the battery exemption refused, a Tasker broadcast or the tile shows "Burkan could not start", and its
      Start action starts the run.
- [ ] A shortcut on the app's icon starts its run with nothing shown, now that its activity's window is
      translucent rather than `Theme.NoDisplay`.
- [x] The exclusions picker lists the installed apps, with their icons.
- [ ] An app ticked there survives Restart all apps.
- [x] Redo setup returns to Setup at its first step, and pairing again works.

## Release build and appearance

- [x] A minified release build installs, pairs, connects and applies: R8 has removed nothing that libadb,
      Conscrypt or spake2 look up at run time. (The 1.0.0 release APK, on an S23 and an S23 Ultra.)
- [ ] The launcher icon looks right on the One UI home screen, and as a themed icon. (Seen so far only in the
      system's App info, where it sits correctly inside Samsung's icon shape.)
- [x] Every screen in the dark theme on the phone itself, in the One UI style.
- [ ] Every screen at the largest text size, and in the light theme.
- [x] Changing the style in Settings changes it in place. (It used to throw the user back to Home; fixed and
      seen working since.)
- [x] Tapping a colour in its sheet, in the Material style: the row draws no ripple and stays as it was, the new
      look spreads from the tap, and the row's mark turns on inside the circle. (Seen frame by frame on an S23,
      10 Oct 2026. The circle shows about an eighth of a second after the touch; there used to be a fixed wait
      of 120 ms on top of that, for the ripple and the mark to settle. What is left is the setting being saved.)
- [x] The same for a style in the Material style, and for a theme and a colour in the One UI style: each change
      spreads from the tap, with the sheet changing in the same circle. (Seen on an S23 with the 1.1.0 release
      build, 11 Oct 2026; the change of style frame by frame.)
- [ ] The row already chosen still ripples.
- [ ] The palette style sheet shows, ahead of each style, a disc of that style's primary, secondary and tertiary
      colours, each at a tone of its own (80, 90 and 60), the same in the dark theme and the light. (This
      version seen in a preview only. Two earlier ones were seen on an S23 on 11 Oct 2026 and looked wrong: the
      scheme's own roles were pastel and all alike in the dark theme, and one middle tone for all three let
      close hues run together. The sheet opened without a hitch both times.)
- [ ] With no font of its own, the Material style is in the phone's font, whichever the user has chosen in the
      phone's settings, and the log's steps and the crash report are in the phone's fixed-width font, with their
      columns still in line. (The build installs and Home opens in the One UI style on an S23, 12 Oct 2026; the
      Material style and the fixed-width text were not looked at.)
- [ ] Home's "Open apps" row on a phone with apps still on OpenGL: the count, the sheet that names them, and
      "Restart them" restarting only those, with the settings put back after. (The dump it is read from was
      taken from an S23 by hand, 12 Oct 2026, and showed two such apps. The row, the sheet and the run were
      seen in previews and tests only.)
- [ ] "Back to OpenGL": the confirmation, the screen locking once, then Home reading "Vulkan is not applied"
      with every surface on OpenGL, and "Open apps" counting what is still on Vulkan. Then "Apply now" bringing
      Vulkan back. (Clearing the property was tried on the Android 17 emulator only. Nothing of this has run
      on a phone.)
- [ ] The two shortcuts, "Apply now" and "Restart all apps", from a long press on the app's icon: each run starts, with System UI left for
      the next lock if the phone is in use. And whether Samsung's Modes and Routines offers them as actions
      of the app, which is how a restart of every app each night is meant to be set up. ("Apply now" was registered on the Android 17 emulator, and the shell is refused when it tries to
      start the shortcut's activity directly. It was not started from a launcher anywhere.)
- [x] A broadcast starts a run with the app not running: `am broadcast -a
      io.github.barqallayl.burkan.action.APPLY -n io.github.barqallayl.burkan/.feature.apply.TriggerReceiver`
      from a computer started the app's process, Android allowed the foreground service from the background, the
      run lasted under three seconds and wireless debugging was off again after. (S23, 12 Oct 2026, with Vulkan
      already active and another app on the screen, which was not disturbed.)
- [ ] The same from Tasker or another automation app on the phone itself.
- [ ] `RUN_FINISHED` reaching a listener, with its four extras. (The run above should have sent one; nothing was
      listening, and a broadcast nobody receives leaves no trace to check.)
- [ ] The Quick Settings tile on the phone: "Not applied" after a restart, a tap applying, "Active" afterwards,
      "Applying…" during a run, and a long press opening the app. (On the Android 17 emulator the tile binds,
      draws dimmed as "Finish setup first", and a tap does nothing, as it should there.)
- [ ] After a restart of the phone: the notification "Vulkan is active" once the apply is done, after the run
      at the lock when System UI was left for it, and not before. (Tests only.)
- [ ] The log after a run on the phone: each step with its time, the line for connecting, the surfaces before
      and after, and the open apps left behind; a run that was "Already applied" opens to show what it found;
      runs logged before the update still show. And the shared file carrying the same. (Previews and tests only.
      A step under half a second reads "0 s".)
- [ ] One UI's floating buttons in the light theme, barely scrolled and well scrolled: a light disc with a soft
      shadow around it, and nothing grey behind the icon. (Seen on an S23, 11 Oct 2026, which makes this one
      done for the light theme; the dark theme was not looked at again after the shadow was moved outside.)
- [x] Tapping one colour and then another straight away: the second circle starts from the second tap, inside
      the first, which is pictured as far as it had got. (Seen frame by frame on an S23, Material style, with
      150 ms between the taps. The order of events is covered by `ThemeRevealTest`.)
- [x] Going from Home to Settings in the Material style: the old screen fades out, then the new one fades in
      while it slides a short way. (Seen frame by frame on an S23.)
- [ ] The same in a right-to-left language: the slide runs the other way, in both styles. (Not seen on a device.)
- [ ] With TalkBack on: each screen's title, each group's title and each sheet's title is a heading; Home's
      status is read out when it changes, and a notice when it comes. (Not seen on a device.)
- [ ] At the largest text size the buttons at the bottom of Home, the crash screen and a sheet grow with their
      labels. (Not seen on a device.)
- [ ] In a window 600 dp wide or more, Material's side margins are 24 dp. (Not seen on a device; the previews
      draw 16 dp.)
- [x] Settings › Report a problem opens the issue form. (Seen on an S23, 11 Oct 2026: with the GitHub app
      installed the form opens there and not in a browser.)
- [x] The 1.1.0 release build, minified and for 64-bit ARM only, installs over 1.0.0 and keeps its pairing and
      settings. Home reads the status, Apply now runs and switches wireless debugging off again, and Settings,
      the exclusions list, the licences and the log open. Its APK is 7.7 MB and declares none of the phone or
      storage permissions. (S23, 11 Oct 2026. Restart all apps, the crash screen, a restart of the phone and
      pairing were not tried with this build.)
- [x] After a restart of the phone, with the build of 10 October that first carried the fixes to the wait for
      Wi-Fi: the automatic apply succeeded, and System UI was restarted at the next lock. (From the app's own
      log on an S23: 10 Oct 2026, 10:49 and 10:50.)
- [ ] With no browser on the phone, or every browser disabled, the links in Settings and in a library's sheet do
      nothing and the app goes on. (It used to stop.)
- [ ] The question before Restart all apps says that Burkan gives itself usage access. (Seen in previews only.)
- [x] A crash with the app in front shows the crash screen with the report; Share opens the share sheet and
      Report a problem the browser. (On an emulator, with `am crash`; not yet on a phone, nor in a minified
      release build.)
- [x] A crash with the app in the background shows nothing and is left to Android; the report is still written.
      (On an emulator.)
- [ ] The crash screen on a Galaxy, where it is drawn in the One UI style.
- [ ] In the Material style: buttons turn squarer while pressed, and Restart all apps turns a progress ring where
      Apply now turns the loading shape.
- [x] In the Material style the top bar is frosted glass: content scrolling under it shows through blurred. (On
      an emulator, on Setup and the crash screen.)
- [x] The bar of buttons at the bottom is frosted too, the frost coming in over its upper edge. (On an emulator,
      on the crash screen only.)
- [ ] The same on Home, Log, Settings and the two lists, on a phone: the bars stay readable over every card and
      the notice's amber, in both themes, and scrolling stays smooth with the blur on.
- [ ] Home in the Material style: the surfaces as a list, Restart all apps as its last row, and Apply now alone
      at the bottom, turning into Cancel during a run. A second tap on Apply now does not cancel the run it
      started. (Seen in previews only.)
- [ ] The phone held sideways: every screen's content is centred at a readable width, and the bars span the
      screen. (Seen in one preview of Home only.)
- [ ] In the Material style, going from one screen to the next slides a short way and fades, and the back gesture
      carries the same change with the finger; a notice, a step and a run's steps open and close on Material's
      spring without the content jumping. (Not seen on a device.)
- [x] The screens behind a sheet and behind a confirmation are blurred as well as dimmed. (On an S23, in the One
      UI style. Android's own blur behind a window was tried first and does nothing there:
      `ro.surface_flinger.supports_background_blur` is unset, so the app blurs its own screens.)
- [ ] The same in the Material style, behind its sheet and its dialog.
- [x] In One UI the bar's round buttons and the floating search are glass: what scrolls under them shows through
      blurred. (On an S23, on Home and the exclusions list.)
- [ ] In the Material style: a bottom sheet is as wide as the screen and docked to its bottom edge, its content
      clear of the navigation bar, and it still joins the circle a change of theme spreads in. (Seen only in
      previews, which draw a sheet without its window.)

- [ ] Settings shows four groups (Applying, Connection, Appearance, About); at the largest text size, Connection's
      value and the Appearance values sit under their names, uncut.
- [ ] With Shizuku, Redo setup reads "Starts setup again", and its confirmation says nothing about a pairing.
- [ ] A library's sheet under Open-source licences shows its licence text with no connection, scrolls to its end,
      and offers Website only.
- [ ] TalkBack on Never restart these apps reads "Chat, checked", not its package name; an app with no name is
      read by its package name.

- [ ] The Log groups runs under Today, Yesterday and dates in the phone's own format, and each row shows only the
      time; a failed run says why on its row, in the past tense, without being opened.
- [ ] An opened run's steps read in the body font, with the durations lined up; a step under a second says
      "Under 1 s".
- [ ] With a filter on, the Log's count reads "2 of 5 runs"; "Not finished" holds cancelled runs and those left for
      the lock.

- [ ] Home's drawn phone: each part in the accent or grey as the rows below say, outlines with a slow repeating
      line while the status is read or a run is under way, and one redraw when a watched run ends, not when Home
      only opens; with Android's animations removed, nothing moves.
- [ ] After a restart that left System UI for the lock, the drawn phone's status bar carries the padlock.
- [ ] Welcome's phone redraws exactly as it did before it moved onto the shared drawing.

- [ ] On the "Still on OpenGL" sheet, every app starts ticked; unticking one leaves it as it is after "Restart N
      apps", and the menu's "Never restart this app" takes it off the list. A list longer than the sheet scrolls
      under the buttons.
- [ ] The Restart all apps sheet mentions usage access only while 30 or 70 is chosen, and the row about the apps you
      chose opens Never restart these apps.
- [ ] The revert confirmation's button reads "Use OpenGL", and the Apply confirmation no longer mentions wireless
      debugging.

## The lock screen, the launcher and the screens

- [ ] Apply now with System UI on OpenGL: the warning shows, the screen locks, and after unlocking the run has
      finished and Home shows System UI on Vulkan.
- [x] With the USB cable unplugged (with it, locking does not restart adbd), the run reconnects after the lock
      screen restarted adbd: the log shows the System UI step as succeeded. (Android 17, 12 Nov 2026)
- [x] mDNS reports the new port after adbd restarts, rather than the old one, within the 30 seconds. (Android 17,
      12 Nov 2026: about a second after the restart)
- [x] With USB debugging off and wireless debugging on, Apply connects, and reconnects under the lock screen, by
      turning wireless debugging off and on; the log shows it as succeeded. (Android 17, 12 Nov 2026, #2)
- [ ] Apply now twice within a minute: the second run leaves System UI alone and says why.
- [x] Reboot: the launcher and keyboard switch at once, and the phone is not locked.
- [ ] After the reboot, Home says System UI switches at the next lock.
- [x] Lock the phone: System UI is on Vulkan when it is unlocked, and wireless debugging is off again.
- [x] The same with System UI's edge lighting helper process running. (The restart used to take the helper and
      leave System UI on OpenGL, and the run failed with "System UI came back, but not on Vulkan". The command
      now names the main process: after a reboot, with the helper running, the run at the lock restarted System
      UI itself and all three surfaces ended on Vulkan.)
- [ ] Locking with the screen-off timeout rather than the power button, and with a lock delay set: does the wait
      still catch the lock?
      On Android 17, through Shizuku, with the screen timing out after 15 seconds: with "Auto lock when screen
      turns off" at 5 seconds, 30 seconds and 5 minutes, the run at the lock started within a second of the phone
      locking and restarted System UI each time (device notes, section 11). Through wireless debugging, the
      power button and the lock set to None are still to try.
- [ ] The foreground notification while waiting for the lock, and its Cancel.
- [ ] Lock, then unlock within a few seconds: System UI is not restarted in front of the user, the log shows
      "System UI at lock · Left for the next lock", wireless debugging is off again, and the next lock restarts it.
- [x] The time from the lock to `am crash` at the lock. (5 seconds.)
- [ ] With "keep wireless debugging on" set, the run at the lock connects after the 3-second settle without failing.
- [ ] The log shows the run after a restart as "After restart", then the run at the lock as "System UI at lock".
- [ ] The run at the lock lists only "Restart System UI": the property, set by the first part, is not set again.
- [x] Install with no reboot: no run starts, and the log gains no "After restart" entry.
- [x] `Settings.Global.BOOT_COUNT` reads on the phone.
- [x] It goes up by one across a reboot.
- [ ] Opening Home twice within 30 seconds raises "Wireless debugging connected" once.
- [x] Home, then Log or Settings for more than 30 seconds, then back: Home does not connect again and no "Wireless
      debugging connected" notification drops. Leaving the app for as long and coming back does connect. (The
      1.1.1 release build on One UI 9, counted from adbd's log; device notes, section 15.)
- [ ] After a run, Home shows the result without a new "Wireless debugging connected" notification.
- [x] `cmd package resolve-activity --brief` and `cmd role get-role-holders` print the formats in the device notes,
      section 2.
- [ ] Their real output is recorded as fixtures.
- [ ] With another home app set as the default, Home reports that app's renderer and the apply restarts it.
- [ ] Restarting the launcher with `am start … HOME` brings it back on Vulkan, and the home screen comes to the
      front.
- [ ] If restarting the keyboard ever changes the default keyboard, `ime set` puts it back.
- [ ] Apply now, force-stop Burkan, open it and Apply now again within a minute: System UI is left alone.
- [ ] In the "Still on" sheet, an app's restart button restarts that app alone, and its keep button adds it to
      "Never restart these apps" and takes it off the list; keeping the last one closes the sheet.
- [ ] After a restart, the "Vulkan is active" notice counts the open apps still on OpenGL, and its "Restart them"
      button restarts those.
- [ ] Android System (`system`) is no longer listed as behind.
- [x] Settings opens at the row the user is sent for, scrolled to and marked out: Wireless debugging in Developer
      options, and Software information in About phone, which is where Samsung keeps the build number. Tried on
      the S23 by sending Settings the same request from a computer, not by tapping setup's own buttons, since setup
      was already done there.
- [x] In a minified release build, Settings › Open-source licences lists the libraries, and opening one shows its
      licence. (The 1.0.0 release APK, on an S23 and an S23 Ultra.)
- [ ] App icons and labels in the exclusions picker, including for the fixed exclusions; scrolling the list stays
      smooth with every app's icon.
- [ ] Moving between screens slides and fades on the shared X axis with no flash of the background, and the
      predictive back gesture follows the same motion.

## Seen on the phone and still open

These are not checks to tick but things the last pass turned up and left as they are.

- The system's "Wireless debugging connected" notification drops over Home's top bar each time Burkan connects,
  and for a few seconds a tap meant for Log or Settings lands on it and opens Developer options. The notification
  is the system's own, raised for every new wireless debugging connection, so the app cannot withhold it; it can
  only connect less often. It no longer connects on the way back from Log or Settings (see the check above); it
  still does when the app is opened.
- In One UI, Apply after restart is below the fold on Home: it is reached by scrolling.
- Not covered at all in this pass, because each needs a system setting changed or the phone unlocked by hand:
  Wi-Fi off, a network not marked "Always allow", a reboot, and anything that restarts System UI.
