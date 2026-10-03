# AGENTS.md

Notes for AI coding agents working in this repository. Everything a contributor needs is in the documents below;
read them first, in this order:

1. [`CONTRIBUTING.md`](CONTRIBUTING.md) — building, testing, the rules that protect the phone, and the code
   conventions.
2. [`docs/architecture.md`](docs/architecture.md) — what the app does and how it is put together.
3. [`docs/device-notes.md`](docs/device-notes.md) — the exact shell commands, and what was measured on a real
   Galaxy S23. Where this file and your memory of Android disagree, this file wins: it was checked on the device.
4. [`docs/testing.md`](docs/testing.md) — what has been verified on a phone and what has not.

## The project in one paragraph

**Burkan** keeps the Vulkan renderer active on a Samsung Galaxy S23 without a computer. Android's UI renderer is
chosen by the system property `debug.hwui.renderer`, which resets on every reboot and can only be set by the
`shell` user. The app pairs once with the phone's own wireless debugging, and from then on re-applies `skiavk`
after each restart by connecting to the ADB daemon on the phone itself. One Gradle module, `:app`; package root
`io.github.barqallayl.burkan`; licensed GPL-3.0-or-later.

## On top of the contributing guide

- Look an API up in its current documentation before using it. Several libraries here move fast, and a guessed
  signature compiles against nothing. Check that a version exists before editing `gradle/libs.versions.toml`.
- Check `git status` before editing and leave changes you did not make alone. Never `git stash`, `checkout` or
  `restore` to compare against the last commit; use a separate worktree.
- Before finishing, run `:app:testDebugUnitTest :app:verifyRoborazziDebug :app:assembleDebug`, and say plainly
  when one could not run.
- You cannot see the phone unless it is attached to this machine. Say "not verified on a device" for anything you
  could not check there.
