# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Owners of a Samsung Galaxy S23, S23+ or S23 Ultra who are not technical. They have usually followed a guide or a
video saying that Android's Vulkan renderer makes the phone smoother, cooler and easier on the battery, and they
want it to stay on. They will switch on Developer options and pair once if every step leads them by the hand, but
they do not want a computer, root, a terminal or jargon, and they should not have to think about Burkan again after
setup.

Owners of other Android 13 and newer phones are welcome and growing as an audience, but they are not the default:
the S23 family is what is verified. They are invited to try it and report how it went, never warned off it.

A smaller group uses Shizuku, Tasker or routines, and the run log. They are served, but the product is not shaped
around them.

## Product Purpose

Android resets the UI renderer property, `debug.hwui.renderer`, to OpenGL on every reboot, and only the `shell`
user can set it. Burkan sets it back to Vulkan (`skiavk`) after each restart, on the phone itself, and checks that
it took.

Success is a phone that stays on Vulkan with nothing for the owner to do: the apply after a restart happens on its
own, System UI is restarted only when the phone locks so it never locks in front of them, and anything that goes
wrong is said plainly with what to do next.

## Positioning

Burkan pairs once with the phone's own Wireless Debugging and from then on connects to the phone's own adb daemon,
so no computer, root, terminal app or internet connection is needed after a restart. Shizuku can be chosen instead,
for a phone where Shizuku already starts itself. It checks the result rather than assuming it, and it never claims
performance gains it does not measure.

## Operating Context

- Setup happens once: notifications, Developer options, Wireless Debugging, pairing with a 6-digit code typed into
  a notification, then the secure settings permission and the battery exemption. Each step opens the right place
  in Settings.
- After a restart, Burkan works unseen: it waits for a trusted Wi-Fi network if needed, applies, and restarts
  System UI at the next lock. A notification says when it is done.
- By hand: Apply now, Restart all apps (all, or the 30 or 70 most recent), restarting open apps still on OpenGL,
  Back to OpenGL, and a list of apps never to restart.
- From outside the app: a Quick Settings tile, two app shortcuts, and broadcasts for Tasker and routines.
- A run log of the last 50 runs, which can be shared as a file that names none of the user's apps, and a crash
  report kept on the phone.

## Capabilities and Constraints

- Android 13 or newer; arm64. Verified on One UI 8.5 (Android 16) and One UI 9 (Android 17) on the S23 family.
- No internet use. The only network traffic is to the phone's own adb daemon and the local mDNS lookup that finds
  its port. No analytics, no crash uploading, no ads.
- No root, no bootloader unlock, no system partition change; the Knox warranty bit stays untouched.
- Distributed through GitHub Releases, F-Droid and Obtainium, signed with one key. Open source under Apache 2.0.
- With Shizuku chosen, Shizuku has to be running before Burkan wakes after a restart.
- Terms used in the app: Vulkan, OpenGL, System UI, launcher, keyboard, Wireless Debugging, Developer options,
  Shizuku, exclusions, run log.

## Brand Commitments

- The name is Burkan: "Vulkan" sounds like "volcano", which is *burkan* in Arabic. The logo is to be redesigned;
  until then nothing new is built on the current one.
- Two designs are kept, both first-class and kept in step: a One UI style that follows Samsung's own system apps,
  the default on Samsung phones, and Material 3 Expressive with dynamic colour elsewhere. Dark and light modes in
  both.
- Wording is plain and honest. Every message says what happened and why in everyday language, and what to do next
  where there is something to do. Burkan does not claim smoother animations, cooler running or longer battery life
  as its own results; it reports what it applied and what it checked.

## Evidence on Hand

- Screenshots of both styles, recorded by the screenshot tests: `app/src/test/screenshots/`.
- The README poster: `docs/images/poster.png`. Store listing text and images: `fastlane/metadata/android/en-US/`.
- Device measurements on a Galaxy S23: `docs/device-notes.md`; what has been checked on a phone:
  `docs/testing.md`.
- No measurements of smoothness, temperature or battery life exist, and no user testimonials or download figures
  are on hand. None may be invented.

## Product Principles

1. Nothing to do after setup. The default path asks nothing of the owner after a restart.
2. Never surprise the owner. Anything disruptive, such as the lock screen System UI's restart brings up or apps
   closing, happens when they are not looking or after they agree to it, and the app says beforehand what it will
   do.
3. Check, do not assume. Report what is actually running on each surface, and say plainly when something did not
   take.
4. Lead the less technical owner by the hand. One step at a time, the right place in Settings opened for them, and
   no jargon where an everyday word will do.
5. Leave the phone as it was found. What the app switches on for a run, it switches off; what a run changes, it
   puts back.

## Accessibility & Inclusion

- Text and controls meet WCAG 2.2 AA contrast in both styles and both themes.
- Every screen works with TalkBack, and at the largest system text size without cutting off text.
