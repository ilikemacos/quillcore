# QuillCore 0.2.1 / 0.3.0 plan

**Target:** Fabric 1.21.8 · **Base:** 0.2.0 (entity/BE render cutoffs, villager brain throttle, ModMenu, real bench — see [CHANGELOG.md](../CHANGELOG.md))
**Theme:** push Quill Render and Quill Logic further on the proven cutoff/throttle pattern; settle FSR1 honestly instead of re-spiking it blind; stand up the FrameFX external adapter as what it actually is (a launcher, not a backend); keep every upscale/frame-gen stub a stub.

> **Status (0.3.0 shipped):** Particle cutoff + item/XP merge throttle + FrameFX external launcher landed. FSR1 closed for this milestone — exact missing hook in FEASIBILITY.md. Chunk coalescing / cloud LOD / entity detail-LOD / redstone batching not shipped.


---

## 0. Priority order

1. Real spatial FSR1 — only if a Sodium-safe hook genuinely exists on Fabric 1.21.8's OpenGL pipeline; otherwise write down the exact missing hook and ship the next-best render optimization instead of a fake spike.
2. More real Quill Render: particle distance cutoff, cloud/entity LOD-style detail reduction, chunk-rebuild coalescing — each only if independently verifiable, same bar as the 0.2.0 cutoffs.
3. More real Quill Logic, same throttle/exemption pattern as `IdleAiThrottle`/`BrainIdleThrottle`.
4. FrameFX adapter as external launch + IPC only — never claims in-process integration. CSR stays banned.
5. XeSS, temporal FSR, frame generation: stay unavailable. No movement.
6. Version bump only if something in 1–4 actually ships; otherwise this stays 0.2.1 (docs/polish), not 0.3.0.

---

## 1. FSR1 — honest stance going into 0.2.1/0.3.0

**Do not re-run the 0.2.0 spike and hope for a different answer.** The 0.2.0 FEASIBILITY.md spike already concluded there is no clean, Sodium-safe insertion point for a post-process blit on vanilla Fabric 1.21.8 OpenGL. Nothing about the renderer changed between 0.2.0 and now. Before touching this again:

- **Re-verify, don't re-guess:** confirm against the actual installed Sodium/Fabric API versions in `gradle.properties` whether `SodiumGameOptionsPages`/`VertexFormat`/final-framebuffer-blit hooks in the *current* Sodium release still lack a public extension point for a post-AA, pre-GUI color-buffer intercept. If Sodium's own `OpenGlRenderDevice`/`BufferRenderer` layer doesn't publish a hook or event (compat module, mixin-plugin hook, or documented extension API), that's the missing piece — name it precisely in FEASIBILITY.md, don't gesture at "renderer complexity."
- **The one case worth shipping:** if (and only if) QuillCore can hook *after* Sodium/Iris finish compositing and *before* the GUI/HUD pass — e.g., a genuine Fabric-API-exposed `WorldRenderEvents.AFTER_TRANSLUCENT`-style or `BeforeFrameEvent`-style hook that hands over a real color FBO — and that hook works identically whether or not Sodium is present (verified by testing with Sodium absent, Sodium present+disabled-in-QuillCore, Sodium present+FSR1-enabled), only then does FSR1 move from stub to real.
- **If no such hook exists (the likely outcome):** update FEASIBILITY.md with the specific missing API (e.g., "Sodium 0.6.x's `SodiumWorldRenderer` has no public post-composite hook; Fabric API has no `WorldRenderEvents` member between final composite and GUI as of 1.21.8") and close FSR1 for 0.2.1/0.3.0. Do not ship a "spatial sharpen" or bilinear scale under the FSR1 name — that violates the honesty invariant from 0.1.0/0.2.0.
- **Next-best render option if FSR1 is blocked:** proceed straight to §2 below. A real cutoff/LOD feature beats a half-landed upscaler every time.

---

## 2. Quill Render candidates (0.2.1/0.3.0)

Same shape as 0.2.0: pure decision logic in `render/` (unit-testable, no MC imports), thin `@Inject`/`@Redirect` mixin, config-gated under `renderEnabled`, default-safe, imperceptible at default settings.

### 2.1 Particle distance cutoff (recommended primary pick)
- **What:** Skip spawning/rendering particles beyond a configurable distance from the camera. Vanilla already has `particlesMaxCount` but no distance-based cutoff; distant particles (campfire smoke, drip, ambient block particles) cost render time for pixels the player can't resolve.
- **Where:** `@Inject` cancel at `HEAD` of `ParticleManager.addParticle(...)` (and/or the per-particle `render` call), gated by `distSq(particle.pos, camera) > cutoffSq`.
- **Why it's real and safe:** pure early-exit on spawn/render, identical risk class to `EntityRenderCutoff`. Doesn't touch simulation — particles are already client-only, visual-only. Default cutoff generous (e.g., 48–64 blocks) so ambient effects near the player are untouched.
- **Unit-testable core:** `render/ParticleRenderCutoff.java` — pure function `(distSq, cutoffSq) -> boolean`, same shape as `EntityRenderCutoff`.

### 2.2 Cloud render LOD / distance cutoff
- **What:** Vanilla clouds render as a fixed grid regardless of distance from the player's view center; reduce cloud draw distance or skip the farthest ring of cloud cells beyond a configurable radius when `fancyClouds`/cloud setting allows degradation anyway. Must preserve "clouds off" and "fast clouds" vanilla options untouched — this is a *third* tier, not a replacement.
- **Where:** `@Inject`/`@Redirect` in `LevelRenderer.renderClouds(...)`, gated by a new `cloudRenderCutoffBlocks` config field. Only trim the clouds grid radius; never change height, color, or motion.
- **Why it's plausible:** clouds are a single fixed-cost render pass independent of world content, so a radius cutoff is a clean, low-risk LOD lever with no gameplay signal (clouds carry no information players act on).
- **Caution:** verify visually that the cutoff edge doesn't produce a visible pop/hard edge at normal FOV before shipping default-on; if it does, ship default-off (`cloudRenderCutoffEnabled = false`) and document it as opt-in only.

### 2.3 Entity render LOD (detail reduction, not skip) — stretch
- **What:** Beyond `entityRenderCutoffEnabled`'s hard distance cutoff (0.2.0), add a nearer "LOD" tier that only reduces per-entity render cost (e.g., skip armor/item-in-hand sub-renders, skip glow/outline layers) for entities between a near and far threshold — without hiding the entity itself.
- **Where:** targeted `@Inject` cancels in `LivingEntityRenderer`'s sub-feature-renderer dispatch (armor layer, held-item layer), gated by distance.
- **Ship only if:** the 0.2.0 hard-cutoff pattern reviewed cleanly in the wild with no bug reports, and only with an allowlist of sub-renderers confirmed cosmetic-only (never collision, hitbox, or nameplate-affecting). This is explicitly a stretch item — cut it from 0.2.1 scope if time-boxed review doesn't land cleanly.

### 2.4 Chunk-rebuild coalescing — verifiable only
- **What:** When multiple adjacent chunk sections are marked dirty in the same tick (common during block-breaking sprees, explosions, world-edit-style changes), coalesce rebuild scheduling instead of issuing N independent rebuild tasks.
- **Where:** hook point depends entirely on whether Sodium is present — vanilla's `ChunkBuilder`/`LevelRenderer.setSectionDirty` path differs completely from Sodium's `RenderSectionManager`. **Do not attempt a Sodium-specific mixin without a compat gate** (`CompatStatus`) that no-ops cleanly when Sodium's internals don't match.
- **Verification bar:** this one only ships if there's an actual measurable before/after (via `BenchHarness`, same honesty bar as `docs/BENCH.md`) showing fewer rebuild dispatches for a reproducible test case (e.g., clearing a 16×16 flat area). If it can't be measured cleanly, it stays a documented idea in FEASIBILITY.md, not code.

---

## 3. Quill Logic candidates (0.2.1/0.3.0)

Same throttle pattern as `IdleAiThrottle`/`BrainIdleThrottle`: skip redundant per-tick work a bounded number of times, force a real tick periodically, always exempt anything with a target/recent damage/player interaction within radius.

### 3.1 Item-entity / experience-orb merge-check throttle (recommended)
- **What:** Vanilla re-scans for nearby mergeable `ItemEntity`/`ExperienceOrbEntity` instances every tick per entity. Throttle this scan (not pickup, not physics) when no player is within a configurable radius — same exemption shape as the mob AI throttle.
- **Why it's the right next pick:** large farms/storage areas can have hundreds of idle item/XP entities; the merge-scan is pure bookkeeping with no player-visible timing requirement at a distance, and the risk profile matches the already-shipped idle AI throttle exactly.
- **Must preserve:** merge/pickup correctness once a player is in range; never throttle while any player is within radius; never delay despawn-timer ticking (that must stay exact — item disappearance after 5 minutes is a real gameplay signal players rely on).
- **Unit-testable core:** `logic/ItemMergeThrottle.java`, mirroring `IdleAiThrottle`'s pure-function shape.

### 3.2 Redstone/block-update batching for far-from-player chunks — only if verifiable
- **What:** Vanilla processes block/fluid tick scheduling per-chunk uniformly regardless of player proximity. Investigate whether a safe throttle exists for *purely cosmetic* scheduled ticks (e.g., leaf decay checks, far-away fluid flow visual updates) without touching redstone logic, farms, or anything with observable timing.
- **Caution:** this is the highest-risk logic candidate — redstone timing is a core gameplay mechanic players build around. **Default to not shipping this** unless a narrowly-scoped, clearly-cosmetic-only subset can be isolated with full test coverage. If no safe subset exists, cut it and say so in FEASIBILITY.md rather than shipping something that breaks a far-away farm silently.

### 3.3 Villager/brain throttle radius tuning — polish, not new logic
- **What:** 0.2.0 shipped `BrainIdleThrottle` with a fixed-shape radius/skip-tick config. Consider exposing it to per-type tuning (e.g., different default radius for `Villager` vs. other `Brain` mobs) only if real-world feedback shows the single shared radius is wrong for a specific mob type.
- **Not a new mixin** — config/tuning only, deferred until there's a concrete complaint to respond to.

---

## 4. FrameFX adapter — external launch/IPC only

0.2.0's `FrameFxBackend` is still a stub reporting unavailable. For 0.2.1/0.3.0, the only legitimate move is exactly what FEASIBILITY.md already scoped and explicitly did not build: an **external process adapter**, not an in-process upscale backend.

- **What ships (if built):** a QuillCore-side helper that can (a) detect whether the Universal-FrameFX companion app is installed/running on the host (Windows-only, PATH or configured path probe), and (b) launch it / signal it via whatever IPC mechanism Universal-FrameFX documents (named pipe, local socket, CLI flags — **do not invent a protocol**; if upstream hasn't published one, this stays unimplemented).
- **Hard constraint, restated:** `FrameFxBackend.isAvailable()` must keep returning `false` for the in-process `UpscaleBackend` contract regardless of whether the external adapter exists — the adapter is a *process-control convenience feature* (think "launch button"), never registered as a rendering backend, and never implies QuillCore is compositing or touching frames on FrameFX's behalf. Keep it in a clearly separate class/package (e.g., `framefx/FrameFxLauncher`, not under `upscale/`) so it can't be confused with a real backend in code review.
- **CSR stays banned.** Universal-FrameFX's own product may bundle CSR-adjacent options; QuillCore's launcher must not expose, forward, or default to any CSR-flavored mode. If the only documented IPC path requires selecting a mode that includes CSR variants, the launcher should only ever send non-CSR mode selectors, and this restriction needs a one-line comment at the call site, not just this doc.
- **Ship gate:** only attempt this if Universal-FrameFX has a real, documented IPC/CLI surface to target. If upstream still exposes nothing beyond "run the EXE, user manages it themselves," then the honest 0.2.1/0.3.0 move is a **"Launch FrameFX" button in the ModMenu screen that just starts the process** (`ProcessBuilder`, no IPC, no mode forwarding) — document that limitation plainly rather than inventing a richer integration that doesn't exist.

---

## 5. Explicitly not moving in 0.2.1/0.3.0

- **XeSS:** stays a stub. No supported OpenGL path on vanilla Fabric; Vulkan-only SDK target is unchanged. No re-spike without a new upstream development to react to.
- **Temporal FSR (FSR2/3):** stays unavailable — needs motion vectors/depth/reactive mask QuillCore's pipeline doesn't have, unrelated to the FSR1 spatial question in §1.
- **Frame generation:** `NoopFrameGenBackend` stays as-is. No motion vectors, no depth path, no HUD-safe compositing exist. `frameGenEnabled` stays forced `false` in `QuillConfig.sanitize()`.
- **CSR:** permanently excluded from QuillCore (restated in §4 for the FrameFX adapter specifically). Not revisited.

---

## 6. Success criteria

- [ ] §1: FEASIBILITY.md updated with either (a) a real FSR1 hook landed and gated behind Sodium/Iris detection with a passing three-way test (absent / present-disabled / present-enabled), or (b) the exact missing API/hook named precisely enough that a future contributor could act on it without re-deriving the investigation.
- [ ] At least one §2 Quill Render item ships (particle cutoff is the primary target) with a pure-logic unit test class and a manual in-game verification note, same honesty bar as 0.2.0's cutoffs.
- [ ] At least one §3 Quill Logic item ships (item/XP merge throttle is the primary target) with an `IdleAiThrottleTest`-style unit test matrix covering all exemption paths.
- [ ] §4: either a working external-launch/IPC adapter gated on a real documented protocol, or a plain "launch the EXE" button with no IPC claims — never an in-process backend flip.
- [ ] §5 items show zero code change; `XessBackend`, FSR2/3, `NoopFrameGenBackend` stay stubs/no-ops; `frameGenEnabled` still forced false.
- [ ] `./gradlew build test` green; all new logic/render classes have unit tests; every 0.2.0 test (`EntityRenderCutoffTest`, `BlockEntityRenderCutoffTest`, `BrainIdleThrottleTest`, `IdleAiThrottleTest`, `BenchHarnessTest`) still passes unmodified.
- [ ] No config default change is gameplay-visible out of the box; every new feature ships opt-out via `masterEnabled`/`renderEnabled`/`logicEnabled` plus its own toggle, following the existing `QuillConfig.sanitize()` pattern.
- [ ] CHANGELOG.md entry added only for what actually ships; version bump to 0.3.0 only if at least one of §1(a)/§2/§3/§4 lands as real, reviewed code — otherwise this is a 0.2.1 docs/polish release and the version stays 0.2.0 until something ships.
