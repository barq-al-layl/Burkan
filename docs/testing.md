# Testing on a phone

Unit tests and screenshot tests run on any machine (see `CONTRIBUTING.md`). What follows cannot: it needs a real
Galaxy S23, and an emulator does not stand in for it. A ticked item was observed on an S23 (SM-S911B, Android 16);
the results are recorded in [`device-notes.md`](device-notes.md). An open item has not been checked yet.

When you check one, tick it here and record what you saw in the device notes.

## Setup and connection

- [x] Pairing succeeds from the notification, on the phone alone.
- [ ] The app connects on `127.0.0.1`. If only the Wi-Fi address works, record it in the device notes. (It
      connected on 2 October 2026; which address it used was not recorded.)
- [ ] `echo ok` returns through `ShellExecutor` with exit code 0, and a failing command returns its real code.
- [x] `pm grant … WRITE_SECURE_SETTINGS` succeeds on One UI.
- [x] With the permission, the app switches wireless debugging on and off.
- [ ] What happens on a Wi-Fi network not marked "Always allow", and with Wi-Fi off. Record both.
- [x] After a reboot, the earlier pairing still authorises the app.
- [ ] Pairing works on `127.0.0.1`. If only the Wi-Fi address works, record it.
- [ ] A wrong code shows the wrong-code message, and pairing with the dialog closed shows the dialog-closed message.
      Both come from how libadb-android words its exceptions; see `docs/device-notes.md`, section 9.
- [x] The code can be typed into the notification from the shade while the pairing dialog stays open.
- [ ] `dumpsys activity processes` (about 1 MB) comes back whole through `shell,v2` within its 20 seconds.
- [ ] The Developer options button opens Developer options, and About phone opens on One UI.

## Applying and status

- [x] Light apply: SystemUI, launcher and keyboard all report `Skia (Vulkan)` afterwards.
- [x] `am crash` on Samsung Keyboard leaves it as the default keyboard.
- [ ] Typing still works after the keyboard restart.
- [ ] Full apply: live wallpaper still selected; auto-rotation, accessibility services and Edge panel settings
      unchanged; Wi-Fi calling still registered.
- [ ] Cancelling a full apply midway restores the settings.
- [ ] Burkan itself survives the full apply and reports the result.
- [ ] Home-screen widgets work after a full apply. The app reads the widget providers and hosts before stopping
      anything, where the script read them after; check that nothing is missed.
- [ ] What `am crash` exits with for a package that is not running (the keyboard, right after boot). A non-zero
      exit fails the step. Record it.
- [ ] What `pm list packages` exits with on a phone with Secure Folder. The app ignores it and reads stdout.
- [ ] During a full apply, with other apps relaunched over Burkan, the run carries on to the restore steps and
      the log records it.
- [ ] Cancel, from Home and from the notification, stops the run and the settings are put back.
- [ ] Opening Home with wireless debugging off refreshes the status, and wireless debugging is off again after.

## After a restart

- [x] Reboot on a trusted Wi-Fi network: Vulkan is applied with no interaction, and wireless debugging is off
      again afterwards.
- [ ] Reboot with Wi-Fi off, then switch Wi-Fi on later: it applies then.
- [ ] Reboot on an untrusted network: the app explains instead of failing silently.
- [x] How long after unlock it finishes. Record it. (`BOOT_COMPLETED` 36 seconds after the first unlock, then
      9 seconds for the first phase; System UI at the next lock. Device notes, section 12.)
- [ ] Battery optimisation left on: does the boot run still happen?
- [ ] Wi-Fi connecting after boot: does the network callback start the foreground service from the background, or
      does it fall back to the "Wi-Fi is connected" notification? Record which, with and without the battery
      exemption.
- [ ] On a refused network, the notification explains and the Home card shows; connecting to a trusted network
      afterwards applies by itself.
- [ ] An automatic run when Vulkan is already active logs "Already applied" and restarts nothing.
- [ ] The exclusions picker lists the installed apps, and a ticked app survives Restart all apps.
- [ ] Redo setup returns to Setup, and pairing again works.

## Release build and appearance

- [ ] A minified release build installs, pairs, connects and applies: R8 has removed nothing that libadb,
      Conscrypt or spake2 look up at run time.
- [ ] The launcher icon looks right on the One UI home screen, and as a themed icon.
- [ ] Every screen at the largest text size and in the dark theme on the phone itself.

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
- [ ] Locking with the screen-off timeout rather than the power button, and with a lock delay set: does the wait
      still catch the lock?
- [ ] The foreground notification while waiting for the lock, and its Cancel.
- [ ] Lock, then unlock within a few seconds: System UI is not restarted in front of the user, the log shows
      "System UI at lock · Left for the next lock", wireless debugging is off again, and the next lock restarts it.
- [ ] The time from the lock to `am crash` at the lock. Record it (about 7 seconds before this change).
- [ ] With "keep wireless debugging on" set, the run at the lock connects after the 3-second settle without failing.
- [ ] The log shows the run after a restart as "After restart", then the run at the lock as "System UI at lock".
- [ ] The run at the lock lists only "Restart System UI": the property, set by the first part, is not set again.
- [ ] Install from Android Studio with no reboot: no run starts, and the log gains no "After restart" entry.
- [ ] `Settings.Global.BOOT_COUNT` reads on the phone and goes up by one across a reboot.
- [ ] Opening Home twice within 30 seconds raises "Wireless debugging connected" once.
- [ ] After a run, Home shows the result without a new "Wireless debugging connected" notification.
- [ ] `cmd package resolve-activity --brief` and `cmd role get-role-holders` print the formats in the device notes,
      section 2. Record the real output as fixtures.
- [ ] With another home app set as the default, Home reports that app's renderer and the apply restarts it.
- [ ] Restarting the launcher with `am start … HOME` brings it back on Vulkan, and the home screen comes to the
      front.
- [ ] If restarting the keyboard ever changes the default keyboard, `ime set` puts it back.
- [ ] Apply now, force-stop Burkan, open it and Apply now again within a minute: System UI is left alone.
- [ ] In a minified release build, Settings › Open-source licences lists the libraries, and opening one shows its
      licence.
- [ ] App icons and labels in the exclusions picker, including for the fixed exclusions; scrolling the list stays
      smooth with every app's icon.
- [ ] Moving between screens slides and fades on the shared X axis with no flash of the background, and the
      predictive back gesture follows the same motion.
