# Testing on a phone

Unit tests and screenshot tests run on any machine (see `CONTRIBUTING.md`). What follows cannot: it needs a real
Galaxy S23, and an emulator does not stand in for it. A ticked item was observed on an S23 (SM-S911B, Android 16);
the results are recorded in [`device-notes.md`](device-notes.md). An open item has not been checked yet. The last
full pass was on One UI 8.5, with a debug build and the USB cable plugged in. The 1.0.0 release build was then
tried on an S23 (SM-S911) and an S23 Ultra (SM-S918).

When you check one, tick it here and record what you saw in the device notes.

## Setup and connection

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
- [ ] On Wi-Fi that has no internet behind it, with mobile data on: the status reads and Apply now works, and
      neither says the phone is not on Wi-Fi.

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
- [ ] What `am crash` exits with for a package that is not running (the keyboard, right after boot). A non-zero
      exit fails the step. Record it.
- [x] What `pm list packages` exits with on a phone with Secure Folder. (0, with an error about the other user
      printed besides the list. Device notes, section 13.)
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
- [ ] Opening Home with wireless debugging off refreshes the status, and wireless debugging is off again after.
- [x] In One UI, a run shows as a list of steps in place of the surfaces, each ticked as it ends, and Done puts
      the surfaces back.
- [ ] A run that fails marks the step that failed. (Seen only on an emulator.)

## After a restart

- [x] Reboot on a trusted Wi-Fi network: Vulkan is applied with no interaction, and wireless debugging is off
      again afterwards.
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
- [ ] The same for a theme and a style, and all three in the One UI style. The row already chosen still ripples.
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
- [ ] Settings › Report a problem opens the issue form in the browser. (Not reachable on the emulator, where
      setup cannot finish; covered by a view model test only.)
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

## The lock screen, the launcher and the screens

- [ ] Apply now with System UI on OpenGL: the warning shows, the screen locks, and after unlocking the run has
      finished and Home shows System UI on Vulkan.
- [ ] With the USB cable unplugged (with it, locking does not restart adbd), the run reconnects after the lock
      screen restarted adbd: the log shows the System UI step as succeeded.
- [ ] mDNS reports the new port after adbd restarts, rather than the old one, within the 30 seconds.
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
- [ ] After a run, Home shows the result without a new "Wireless debugging connected" notification.
- [x] `cmd package resolve-activity --brief` and `cmd role get-role-holders` print the formats in the device notes,
      section 2.
- [ ] Their real output is recorded as fixtures.
- [ ] With another home app set as the default, Home reports that app's renderer and the apply restarts it.
- [ ] Restarting the launcher with `am start … HOME` brings it back on Vulkan, and the home screen comes to the
      front.
- [ ] If restarting the keyboard ever changes the default keyboard, `ime set` puts it back.
- [ ] Apply now, force-stop Burkan, open it and Apply now again within a minute: System UI is left alone.
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
  only connect less often.
- In One UI, Apply after restart is below the fold on Home: it is reached by scrolling.
- Not covered at all in this pass, because each needs a system setting changed or the phone unlocked by hand:
  Wi-Fi off, a network not marked "Always allow", a reboot, and anything that restarts System UI.
