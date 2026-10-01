# Fixtures

Shell output the parsers and flows are tested against. Each file follows a format in `docs/device-notes.md`.

- The lines the device notes quote are copied from them. Everything else — the other lines in a dump, the process
  IDs, the third-party apps — is filler written to the same shape. Third-party package names are invented
  (`com.example.*`, `org.example.*`, `net.example.*`); none comes from a real phone's app list.
- `gfxinfo-*.txt`: the notes give only the `Pipeline=` line. The rest of the dump is filler.
- When a capture from the device shows a shape these files do not, add it as a new fixture rather than editing an
  old one.
