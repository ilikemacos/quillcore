# Changelog

## 0.2.0 — 2026-10-22

### Added
- Quill Render: configurable entity render-distance cutoff (default 128 blocks; camera/vehicle exempt).
- Quill Render: distant sign/skull block-entity render skip (chests/animated BEs never skipped).
- Quill Logic: villager `Brain.tick` idle throttle (never while trading/panicking/hurt).
- ModMenu + Cloth Config settings screen (soft dependencies).
- `/quillcore reset` and expanded `/quillcore set` keys.
- Real bench tick + frame sampling wired to Fabric server/client events.
- `docs/BENCH.md`, `docs/PORTING.md`, `docs/NEXT.md`, `scripts/bench-compare.sh`.
- GitHub Actions CI (`build` + `test`).
- Unit tests for render cutoffs, brain throttle, and bench harness.

### Changed
- Version bump to 0.2.0; config schema version 2 with migration.
- README / FEASIBILITY updated for honest upscale stance (FSR1 spike did not land a safe hook).

### Notes
- FSR / XeSS / FrameFX / frame gen remain stubs (`isAvailable() == false`).
- CSR excluded.
- No fabricated Sodium/Lithium benchmark numbers.

## 0.1.0 — 2026-10-22

### Added
- Fabric mod for Minecraft **1.21.8** (Loader 0.19.5, Fabric API 0.136.1+1.21.8).
- Persistent `config/quillcore.json` with master / render / logic toggles.
- `/quillcore` command (status, reload, set, bench).
- Quill Logic: idle distant-mob `GoalSelector` throttle (`Mob#serverAiStep` redirects).
- Unit tests for `IdleAiThrottle` decision logic.
- Bench harness metric fields (no fabricated results).
- Stub upscale backends: FSR, XeSS, FrameFX — all unavailable.
- Stub frame-generation backend.
- `docs/FEASIBILITY.md` for FSR / XeSS / FrameFX / frame generation.
- MIT license.
