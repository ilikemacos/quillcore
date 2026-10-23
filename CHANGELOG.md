# Changelog

## 0.4.0 — 2026-10-22

### Added
- **AMD FSR1 (EASU+RCAS)** as a real Quill Upscale backend (`fsr1` / legacy `fsr`):
  - Main `RenderTarget` render-scale before `GameRenderer#renderLevel`.
  - Portable GLSL EASU + RCAS (AMD FidelityFX FSR1 algorithm, MIT) via `RenderPipeline` fullscreen passes.
  - Upscale completes before GUI so HUD stays native resolution.
  - Quality presets: Ultra Quality / Quality / Balanced / Performance / Ultra Performance.
  - RCAS sharpness + optional render-scale override in config (schema v5) and Cloth Config UI.
- Honest **FSR2** / **FSR3** backend classes that refuse to enable with logged reasons (motion vectors / FG).
- `third_party/amd-fsr1/` NOTICE + MIT LICENSE (AMD attribution).
- Unit tests: `FsrQualityPresetTest`.

### Changed
- Version bump to **0.4.0**; config schema version **5**.
- `docs/FEASIBILITY.md`, README, NEXT updated for FSR1 ship + FSR2/3 stubs.
- Upscale registry lists `fsr1`, `fsr2`, `fsr3` (plus legacy `fsr` → FSR1).

### Known limits
- **Sodium:** may not honor vanilla main-RT resize — FSR1 can be limited/disabled with Sodium; log warns.
- FSR2/FSR3/XeSS/frame gen remain unavailable (no fabricated claims).
- No fabricated FPS benches.
- CSR permanently excluded.

### Not shipped
- FSR2 temporal upscale, FSR3 frame generation, XeSS, in-process FrameFX.


## 0.3.1 — 2026-10-22

### Added
- Quill Logic: PathNavigation idle throttle for far path-done mobs (no attack target / not hurt).
- Quill Render: opt-in cloud distance cap (`CloudRenderer` cloudRange min redirect). **Default off.**
- Quill Render: opt-in weather rain/snow column radius cap. **Default off.**
- 128×128 mod icon; `docs/PUBLISHING.md` (Modrinth/CurseForge field guide).
- Expanded `docs/PORTING.md` with verified Fabric Meta versions for 1.21.9–1.21.11 (not multi-built).
- Unit tests: `PathNavThrottleTest`, `CloudDistanceCapTest`, `WeatherRadiusCapTest`.

### Changed
- Version bump to 0.3.1; config schema version 4 with migration.
- `docs/NEXT.md` rewritten as post-0.3.1 “done for now”.
- `docs/FEASIBILITY.md`: chunk-coalescing skip rationale + 0.3.1 feature notes.

### Not shipped
- Chunk rebuild coalescing (Sodium divergence; unmeasurable here).
- Spatial FSR1 / XeSS / temporal FSR / frame gen / CSR.
- Second Loom target for 1.21.9–1.21.11 (notes only).
- No fabricated benches.

## 0.3.0 — 2026-10-22

### Added
- Quill Render: particle spawn-distance cutoff (default 64 blocks; client-visual only).
- Quill Logic: ItemEntity / ExperienceOrb neighbour-merge scan throttle when far from players (pickup/age untouched).
- FrameFX **external** launcher (`FrameFxLauncher`): PATH/config probe + `/quillcore framefx probe|launch`. Windows-only; **no CSR/backend/FG flags** forwarded; never registered as an in-process upscale backend.
- Cloth Config / ModMenu entries for particle cutoff, item-merge throttle, and FrameFX executable path.
- Unit tests: `ParticleRenderCutoffTest`, `ItemMergeThrottleTest`, `FrameFxLauncherTest`.

### Changed
- Version bump to 0.3.0; config schema version 3 with migration.
- `FsrBackend` / `FrameFxBackend` messaging updated for honest 0.3.0 status.
- `docs/FEASIBILITY.md` names the exact missing FSR1 hook (Sodium-safe post-composite resizable color target before GUI / `presentTexture`).

### Not shipped (documented)
- Spatial FSR1, temporal FSR, XeSS, frame generation, chunk-rebuild coalescing, in-process FrameFX.
- CSR remains permanently excluded.
- No fabricated benches.


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
