# QuillCore technical feasibility — FSR, XeSS, FrameFX, frame generation

**Minecraft target:** 1.21.8 (Fabric)  
**Milestone:** 0.3.0  
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

### Exact missing hook (0.3.0 close-out)

> **Missing:** a Sodium-safe, Fabric-documented interception point that (a) lets a mod set main-pass render resolution independently of GUI resolution, and (b) provides the completed world color `GpuTextureView` (or legacy FBO id) after composite and **before** hand/GUI for a spatial upscale pass.  
> Neither `WorldRenderEvents.END` / `LAST` nor `GpuDevice.presentTexture` supplies (a)+(b) together. Sodium does not publish a post-composite extension for this on 1.21.8.

**0.3.0 status:** **Not implemented.** `FsrBackend` remains a stub labeled **"AMD FSR1 spatial (not implemented)"**. Do **not** bilinear-scale and label it FSR. Temporal FSR stays unavailable (separate motion-vector blocker).

**Next-best render work shipped instead:** particle spawn-distance cutoff (see CHANGELOG / Quill Render).

**Chunk-rebuild coalescing:** investigated as a candidate; vanilla `LevelRenderer` / `SectionRenderDispatcher` vs Sodium `RenderSectionManager` diverge completely. No Sodium-safe coalescing mixin shipped; no fabricated bench. Remains a future idea only if a measurable rebuild-dispatch reduction can be shown with `BenchHarness`.

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

## 5. Summary for 0.3.0

| Backend / helper | Implemented? | Available at runtime? |
|------------------|--------------|------------------------|
| FSR1 spatial | Stub (exact hook documented above) | No |
| Temporal FSR / XeSS | Stub | No |
| FrameFX in-process (`FrameFxBackend`) | Stub adapter | No |
| FrameFX external launcher | **Yes** (`FrameFxLauncher`) | Probe/launch on Windows only; never claims compositing |
| Frame gen | Stub | No |

QuillCore loads successfully with all **upscale** backends unavailable. Selecting them must not crash the game. CSR remains permanently excluded.
