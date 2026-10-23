# QuillCore 0.2.0 plan

**Target:** Fabric 1.21.8 · **Base:** 0.1.0 (idle AI throttle + stub backends, see [FEASIBILITY.md](FEASIBILITY.md))
**Theme:** ship the first real Quill Render mixins, a config UI, and real benchmarks — without touching upscale/frame-gen honesty.

---

## 1. Prioritized now vs. later

### Now (0.2.0 scope)
1. **Quill Render: entity render-culling mixin** (see §2.1) — first real render optimization.
2. **Quill Render: block-entity / tile-entity render distance cutoff** (see §2.2).
3. **Logic: inactive-villager/brain throttle**, same pattern as idle AI throttle (see §2.3).
4. **Real BenchHarness metrics** — `BenchHarness`/`BenchMetrics` currently exist but are unwired to anything real; wire tick-time and frame-time sampling so `/quillcore bench report` prints actual numbers, not placeholders.
5. **Cloth Config / ModMenu integration** — expose existing `QuillConfig` fields in a real screen instead of `/quillcore set`.
6. Config versioning migration path (`configVersion` bump + migrator) now that a second real feature set exists.

### Later (0.3.0+, explicitly deferred)
- FSR1 spatial upscale (only after a renderer-hook spike proves a clean post-process insertion point — see §5).
- Any FrameFX IPC helper (OS-level, optional, outside the GL pipeline).
- XeSS, temporal FSR, any frame generation. Stay stubs.
- Sodium/Iris compatibility shims beyond `CompatStatus` detection.
- Per-dimension / per-biome tuning of throttle radius.

---

## 2. Concrete implementable items (gameplay-preserving)

All items below follow the 0.1.0 pattern: pure decision logic in `logic/` (unit-testable, no MC imports), mixin is a thin `@Redirect`/`@Inject` shim, config-gated, default-safe, never changes simulation outcomes a player can notice at the radius chosen.

### 2.1 Entity render-distance / frustum-skip mixin (recommended primary render opt)
- **What:** Vanilla already frustum-culls, but it re-evaluates visibility and re-submits render state for *every* loaded entity every frame, including ones far outside a sane render cutoff that the player cannot meaningfully perceive (e.g., tiny distant mobs behind foliage/fog). Add a **configurable max entity-render distance** (separate from simulation distance) that skips `EntityRenderDispatcher`/`shouldRender` work for entities beyond it, before the existing frustum test runs.
- **Where:** `@Inject` at `HEAD` of `LivingEntityRenderer`/`EntityRenderDispatcher.shouldRender(Entity, Frustum, double, double, double)` (client-side mixin), returning `false` early when `entity.distanceToSqr(camera) > cfg.renderCutoffSq` and the entity is not the player's own vehicle/passenger.
- **Why it's real and safe:** it's a pure early-exit on an already-existing boolean gate — identical class of change to the idle AI throttle. No entity state changes, no tick logic touched, purely client visual. Default cutoff should be generously large (e.g., 96–128 blocks) so it's imperceptible at default settings, and `renderEnabled=false` (already in `QuillConfig`) fully disables it.
- **Unit-testable core:** extract the distance/exemption decision into `logic/EntityRenderCutoff.java` (pure function: distSq, cutoffSq, isPassengerOrVehicle, isSpectator → boolean), same shape as `IdleAiThrottle`.

### 2.2 Block-entity (tile-entity) render skip for distant, non-animated BEs
- **What:** Skip `BlockEntityRenderer.render()` calls for block entities beyond a configurable distance that have no player-visible animation state changed this tick (chests not currently open, non-spinning, etc. — conservative: only skip ones with no active animation flag at all, e.g., signs, skulls, distant furnaces not lit-animated).
- **Where:** `@Inject` cancel at `HEAD` in the BE render dispatch path, gated by distance + a per-type allowlist (start with signs/skulls only — conservative first cut).
- **Why safe:** purely visual, vanilla already would render them at full detail at any distance; this only trims ones already far outside normal play relevance. Ship this *after* 2.1 and only if 2.1's pattern reviews cleanly — keep the allowlist small for 0.2.0.

### 2.3 Logic: idle-villager/brain goal throttle (second Quill Logic opt)
- **What:** Extend the exact `IdleAiThrottle` pattern to `Villager`/other `Brain`-driven mobs' POI/schedule re-evaluation when far from any player and not currently being interacted with (not trading, not pathing to a player-triggered task). Skip redundant `Brain.tick()` sensor re-runs the same bounded number of times, then force one, exactly like `MobServerAiThrottleMixin`.
- **Why it's the right second pick:** villagers/brain mobs are reliably the most expensive AI in populated bases (trading halls, villages) — bigger real-world win than a second generic-mob throttle — and the risk profile is identical to the already-shipped-and-tested 0.1.0 feature, so it reuses the same review/test playbook.
- **Must preserve:** trading, breeding, panic/damage response, schedule transitions near players — same exemption conditions as 2.1's mob throttle (has target / recently hurt / within radius → never skip).

---

## 3. Risks

- **Render mixins are riskier than logic mixins.** A bad frustum/cutoff mixin can cause pop-in, invisible hostile mobs, or crashes on mod conflicts (Sodium/Iris patch the same classes). Mitigate: feature-flag (`renderEnabled`), ship 2.1 behind a conservative default cutoff, and gate loading on `CompatStatus` detection — disable/no-op if Sodium/Iris signature doesn't match expectations rather than crash.
- **Perceptibility creep.** "Imperceptible" cutoffs can become perceptible in caves, long corridors, or with high FOV/mods that zoom. Default values must be tuned conservatively and documented as configurable, not just fast.
- **Villager throttle (2.3) touches trading/economy-sensitive logic.** More scrutiny needed than mob combat AI; a missed exemption (e.g., mid-trade GUI open) is a worse bug than a slow zombie. Needs its own unit test matrix mirroring `IdleAiThrottleTest`.
- **Cloth Config / ModMenu adds a new dependency surface.** Must stay optional (soft dependency) so QuillCore still loads and functions via `/quillcore set` if Cloth Config isn't installed — don't hard-require it.
- **BenchHarness going from stub to real sampling** risks overhead from the measurement itself; sampling must be cheap/opt-in (`benchEnabled`) and never run by default in shipped builds.
- **Scope creep toward upscale/frame-gen.** The explicit ask is "do everything" — resist the temptation to flip FSR/XeSS/FrameFX stubs to "available" without a real integration; that would violate the honesty invariant 0.1.0 established and documented in FEASIBILITY.md.

---

## 4. Success criteria for 0.2.0

- [ ] At least one real Quill Render mixin (§2.1) ships, is config-gated, defaults to imperceptible, and has a pure-logic unit test class alongside a manual in-game verification note (same honesty bar as the existing throttle mixin's doc comment).
- [ ] Villager/brain throttle (§2.3) ships with its own `IdleAiThrottleTest`-style unit tests; trading/breeding/panic paths manually verified in-game.
- [ ] `/quillcore bench report` prints real sampled tick/frame numbers, not placeholder text — and FEASIBILITY.md or a new BENCH.md records actual before/after numbers from a real test world, not invented ones.
- [ ] Cloth Config screen (if Cloth Config present) can toggle every `QuillConfig` field that `/quillcore set` can; mod still loads and works with Cloth Config absent.
- [ ] `./gradlew build test` green; all new logic classes have unit tests; existing `IdleAiThrottleTest` still passes unmodified.
- [ ] FSR/XeSS/FrameFX/frame-gen backends remain stubs reporting unavailable — zero behavior change, zero new claims. FEASIBILITY.md updated only if a genuine new finding emerges (e.g., an FSR1 spike result), not to soften the "not implemented" language.
- [ ] No config default changes gameplay-visible behavior out of the box; every new feature ships opt-out via existing `masterEnabled`/`renderEnabled`/`logicEnabled` gates.

---

## 5. Honest upscale/frame-gen stance for 0.2.0

No change to the 0.1.0 position; 0.2.0 does not flip any backend to "available."

- **FSR1 (spatial):** The historically-open spatial FSR1 algorithm is small enough to be plausible as an open-source, in-process post-process pass (it only needs a color buffer at render resolution, no motion vectors). This is the one upscale path worth a **time-boxed spike** in 0.2.0 or 0.3.0 — not a commitment to ship — to determine whether a Fabric client mixin can cleanly intercept the final framebuffer blit on vanilla OpenGL without fighting Sodium/Iris. If the spike doesn't land a clean hook point, it stays a stub.
- **XeSS:** Still blocked. No supported OpenGL path; Vulkan-only SDK target doesn't match vanilla Fabric's renderer. Stays a stub in 0.2.0.
- **FrameFX:** Still blocked as an in-process backend — closed engine, no plugin API, explicitly non-injecting by design. The only legitimate 0.2.0-adjacent move is the already-documented **optional external OS helper** (launch/IPC to the existing Universal-FrameFX overlay app), which is a process-control feature, not a "backend" inside QuillCore's upscale pipeline — and it's *later*, not now.
- **Frame generation:** Off. No motion vectors, no depth path, no HUD-safe compositing exist in this pipeline. `NoopFrameGenBackend` stays as-is.
- **CSR:** Permanently excluded from QuillCore, as established in FEASIBILITY.md. Not revisited.
