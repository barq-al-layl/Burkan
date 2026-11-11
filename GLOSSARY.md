# Burkan

Burkan keeps a phone's UI drawing with the Vulkan renderer by setting it again after every restart, through a
shell it reaches on the phone itself. These are the words the app and its documents use.

## Connection

**Connection**:
How Burkan reaches a shell on the phone: Wireless debugging or Shizuku. The owner chooses one.
_Avoid_: backend, ADB, way to a shell

**Wireless debugging**:
The connection through the phone's own Android debugging over Wi-Fi, paired with Burkan once. The default.
_Avoid_: ADB, wireless ADB

**Shizuku**:
The connection through Shizuku, a separate app the owner keeps running, which hands Burkan a shell.

**Pairing**:
The one-time exchange that lets Burkan use Wireless debugging, done by typing the code the phone shows into Burkan's
notification.

## Getting started

**Welcome**:
The single screen a fresh install opens on, before Setup: what Burkan does and what it needs. Shown until the owner
moves on to Setup, never again after.
_Avoid_: onboarding, intro, tutorial

**Setup**:
The checklist the owner works down once to make Burkan able to apply on its own. Its steps differ with the
Connection.
_Avoid_: onboarding, wizard

**Finishing setup**:
The Setup step that connects and lets Burkan grant itself its permission, with nothing for the owner to do.

**First apply**:
The apply that ends Setup, which the owner may put off.

## Home

**Apply after restart**:
The setting that has Burkan apply Vulkan by itself each time the phone restarts. On by default.
_Avoid_: apply on boot, after a restart, auto-apply

**Waiting for the lock**:
The state after a restart in which everything is on Vulkan except System UI, which Burkan restarts the next time the
owner locks the phone rather than lock it in front of them. Nothing is wrong and nothing is asked of the owner.
_Avoid_: partly applied (for this state), pending

**Apply now**:
Sets new apps to Vulkan and restarts System UI, the launcher and the keyboard so they draw with it. Open apps keep
the renderer they started with.
_Avoid_: light apply, quick apply

**Restart all apps**:
Apply now, plus closing every app and opening again those that were open, so they too draw with Vulkan.
_Avoid_: full apply, deep apply

**Apps left behind**:
Open apps still drawing with the renderer they started on, after new apps have been moved to the other one.
_Avoid_: stale apps, old apps, outdated apps
