# QuillCore technical feasibility — FSR, XeSS, FrameFX, frame generation

**Minecraft target:** 1.21.8 (Fabric)  
**Milestone:** 0.4.0  
**Date:** 2026-10-22 (America/Vancouver)

This document records what was investigated. **Nothing here is claimed as a working integration** unless explicitly marked. Stub backends in `upscale/` and `framegen/` report unavailable. External FrameFX launch is a process helper, not an upscale backend.

---

## 1. AMD FidelityFX Super Resolution (FSR)

| Topic | Finding |
|-------|---------|
| Spatial FSR 1 | Open-source (MIT) via GPUOpen FidelityFX-FSR. Algorithm is EASU + RCAS; needs a color buffer at **render** resolution, **no** motion vectors. |
| Temporal FSR 2/3 | GPUOpen SDK; requires motion vectors, depth, reactive/exposure masks. |
| MC pipeline (1.21.8) | Vanilla Fabric uses Mojang's new `GpuDevice` / `RenderPass` stack. Final present is `presentTexture(GpuTextureView)` — not a classic `blitToScreen` FBO bind you can wrap with a fragment pass. |
| Fabric API hooks | `WorldRenderEvents` order: START → … → AFTER_TRANSLUCENT → LAST → END. **END** fires after world rendering / before hand+GUI, but the context does **not** hand QuillCore a resizable main color FBO or a "render scale" knob. Events are for appending draws, not for substituting the main pass resolution. |
| Sodium | Sodium replaces / heavily rewrites the world render path. There is **no** public Sodium extension API (as of the Sodium builds QuillCore targets for 1.21.8) that exposes "post-composite, pre-GUI color buffer at controllable render scale." Mixing into Sodium internals without a compat gate would break Sodium and is out of policy. |
| What a real FSR1 path would need | (1) Resize / recreate the main color (+depth) target to `scale * window` **before** `GameRenderer#renderLevel` / `LevelRenderer#renderLevel`. (2) Run the full world pass into that lower-res target. (3) Run EASU+RCAS (or a ported fragment/compute shader) into a full-res texture. (4) Present that to the swapchain / `presentTexture` **before** GUI. (5) Draw GUI/HUD at native resolution. Steps 1–4 must behave identically with Sodium absent, Sodium present+disabled, and Sodium present — QuillCore does not have that hook. |

### 0.4.0 — FSR1 shipped (vanilla main-RT path)

**Implemented:** Mixin on `GameRenderer#render`:
1. Before `renderLevel` — resize main `RenderTarget` to preset render scale.
2. After world + vanity post, before `FogRenderer#endFrame` / GUI — run AMD FSR1 **EASU** then **RCAS** (MIT GLSL port) into scratch full-res targets, restore main to native size, blit result.
3. GUI/HUD draw at native resolution.

**Shaders:** `assets/quillcore/shaders/core/fsr_{easu,rcas,blit}.*` — algorithm © AMD, MIT (`third_party/amd-fsr1/`).

**Sodium:** Still **no** public Sodium post-composite API. FSR1 depends on vanilla main-RT resize. With Sodium loaded, QuillCore logs a warning; if Sodium ignores the resize, FSR1 is **limited or ineffective** — document as "disabled or limited with Sodium". Do not claim Sodium-certified FSR.

**FSR2 / FSR3:** Stub backends (`fsr2`, `fsr3`) with `isAvailable()==false`. Blockers unchanged: motion vectors, jitter, FG pacing. Frame gen config accepted but ignored.

**Honesty:** Never label bilinear/generic scaling as FSR. CSR permanently excluded.

**Chunk-rebuild coalescing:** still not shipped (Sodium divergence; unmeasurable here).

---

## 2. Intel XeSS

| Topic | Finding |
|-------|---------|
| Official SDK | Intel XeSS SDK targets DirectX 12 and Vulkan primarily. |
| MC default API | OpenGL / new GpuDevice abstraction (LWJGL). Vulkan exists via third-party renderers, not vanilla Fabric. |
| 0.3.0 status | **Not implemented.** `XessBackend` is a stub. |

**Blockers:** no supported OpenGL / GpuDevice integration path for full XeSS; must not mislabel a generic scaler as XeSS.

---

## 3. Universal-FrameFX (FrameFX)

**Upstream:** https://github.com/ilikemacos/universal-framefx  

| Topic | Finding |
|-------|---------|
| What it is | Windows 10/11 **external** upscaler / frame generator: captures a window via Windows Graphics Capture and draws a **click-through overlay**. |
| Licensing | Application code MIT; **image-processing engine is closed**. |
| Injection | Explicitly **does not inject** into the game process. |
| Minecraft OpenGL/Vulkan plugin API | **None exposed** by FrameFX for in-process hooks. |
| CSR | FrameFX’s own product may include CSR options; **CSR is explicitly excluded from QuillCore** and is **not** treated as a FrameFX component or integration path here. |

### Integration paths

| Path | Verdict |
|------|---------|
| In-process OpenGL interop calling FrameFX engine | **Blocked** — engine closed; no public Minecraft plugin API. `FrameFxBackend.isAvailable()` stays `false`. |
| External launch (`ProcessBuilder`) | **Shipped in 0.3.0** as `framefx/FrameFxLauncher` — PATH / config path probe + `/quillcore framefx launch`. **No mode / CSR / FG CLI flags** are forwarded; user configures inside FrameFX UI. Not registered as an `UpscaleBackend`. |
| Documented IPC protocol | **Still absent** upstream — no richer connect/setMode adapter. |

---

## 4. Frame generation

Temporal frame generation needs motion vectors (or optical flow), depth, HUD-safe presentation, and pacing with MC’s frame loop.

**0.3.0 status:** `NoopFrameGenBackend` only. Config forces `frameGenEnabled=false`. No FG claims.

---

## 5. Summary for 0.4.0

| Backend / helper | Implemented? | Available at runtime? |
|------------------|--------------|------------------------|
| FSR1 spatial (EASU+RCAS) | **Yes** (vanilla main-RT path) | Yes when `upscaleBackendId=fsr1` (Sodium may limit) |
| FSR2 / FSR3 | Stub (refuse + log) | No |
| XeSS | Stub | No |
| FrameFX in-process (`FrameFxBackend`) | Stub adapter | No |
| FrameFX external launcher | **Yes** (`FrameFxLauncher`) | Probe/launch on Windows only; never claims compositing |
| Frame gen | Stub | No |

QuillCore loads with FSR1 available and FSR2/FSR3/XeSS/FrameFX-in-process unavailable. Selecting an unavailable backend must not crash. CSR remains permanently excluded.

## Chunk rebuild coalescing (skipped — 0.3.1)

**Not shipped.** Vanilla `SectionRenderDispatcher` already serializes rebuilds through `CompileTaskDynamicQueue`. Sodium replaces that path with `RenderSectionManager` / its own task graph. A safe coalesce would need:

1. Fork-aware hooks (vanilla + Sodium), and
2. Honest FPS / rebuild-queue benches on a GPU client.

This headless box cannot measure either. Do not ship a no-op “coalesce” flag.

## Cloud distance / weather radius caps (shipped 0.3.1)

- **CloudDistanceCap** — Redirect of the single `Math.min(cloudRange, 128)` in `CloudRenderer.render`. Default **off**.
- **WeatherRadiusCap** — `@ModifyArg` on `collectColumnInstances` radius. Default **off** (hard edge if too low).

## Path navigation idle throttle (shipped 0.3.1)

Skips `PathNavigation.tick` only when path `isDone()`, no attack target, not hurt, far from players. Never interrupts active path following.

