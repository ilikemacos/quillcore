# Changelog

## 0.1.0, 2026-10-22

### Added
- Fabric mod for Minecraft **1.21.8** (Loader 0.19.5, Fabric API 0.136.1+1.21.8).
- Persistent `config/quillcore.json` with master / render / logic toggles.
- `/quillcore` command (status, reload, set, bench).
- Quill Logic: idle distant-mob `GoalSelector` throttle (`Mob#serverAiStep` redirects).
- Unit tests for `IdleAiThrottle` decision logic.
- Bench harness metric fields (no fabricated results).
- MIT license.

### Notes
- Quill Render not implemented.
- CSR excluded.
- `runClient` / Sodium+Lithium comparisons not claimed on headless CI.
