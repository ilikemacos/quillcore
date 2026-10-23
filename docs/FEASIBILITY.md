# QuillCore technical feasibility — FSR, XeSS, FrameFX, frame generation

**Minecraft target:** 1.21.8 (Fabric)  
**Milestone:** 0.2.0  
**Date:** 2026-10-22 (America/Vancouver)

This document records what was investigated. **Nothing here is claimed as a working integration** unless explicitly marked. Stub backends in `upscale/` and `framegen/` report unavailable.

---

## 1. AMD FidelityFX Super Resolution (FSR)

| Topic | Finding |
|-------|---------|
| Spatial FSR 1 | Open-source (MIT) via GPUOpen FidelityFX-FSR. Algorithm is a single-pass EASU + RCAS style spatial upscaler; needs a color buffer at render resolution, **no** motion vectors. |
| Temporal FSR 2/3 | GPUOpen SDK; requires motion vectors, depth, reactive/exposure masks. |
| MC pipeline | Vanilla + Fabric use OpenGL. Iris/Sodium alter the deferred/forward path. |
| 0.2.0 spike | Explored inserting a post-process blit after the final scene framebuffer. **No clean, Sodium-safe hook landed in 0.2.0.** Vanilla `GameRenderer` / framebuffer blit points are renderer-mod-fragile; shipping a half-wired shader pass that fights Iris/Sodium is worse than a stub. |
| 0.2.0 status | **Not implemented.** `FsrBackend` remains a stub. Do **not** bilinear-scale and label it FSR. |

**Blockers (unchanged):** clean post-process insertion point that survives Sodium/Iris; redistributable shader packaging; GPU feature detection and fallbacks.

**Next spike candidate (0.3.0):** standalone OpenGL FSR1 compute/fragment shader behind an Iris/Sodium detection gate, disabled by default when those mods are present.

**Graceful fallback:** backend remains registered, `isAvailable() == false`, config can select it without crashing.

---

## 2. Intel XeSS

| Topic | Finding |
|-------|---------|
| Official SDK | Intel XeSS SDK targets DirectX 12 and Vulkan primarily. |
| MC default API | OpenGL (LWJGL). Vulkan exists via third-party renderers, not vanilla Fabric. |
| Hardware | Best quality on Intel Arc with XMX; DP4a fallback on other GPUs where supported. |
| 0.2.0 status | **Not implemented.** `XessBackend` is a stub. |

**Blockers:** no supported OpenGL integration path for full XeSS; must not mislabel a generic scaler as XeSS.

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

### Valid integration paths?

| Path | Verdict |
|------|---------|
| In-process OpenGL interop calling FrameFX engine | **Blocked** — engine closed; no public Minecraft plugin API. |
| Launch/control FrameFX externally (process IPC) while MC runs | Possible later as an **optional OS helper**, not an upscale “backend” inside the GL pipeline. Adapter interface sketched below; **not built in 0.2.0**. |
| Reimplement FrameFX algorithms inside QuillCore | Out of scope; would not be “FrameFX”. |

**0.2.0 status:** `FrameFxBackend` adapter stub only — `isAvailable() == false`.

**Designed future adapter (NOT IMPLEMENTED — do not invent a protocol):**

```text
FrameFxBackend
  id = "framefx"
  probe(): check OS == Windows && optional companion EXE present on PATH or configured path
  connect(): IPC to companion (undefined protocol — blocked until Universal-FrameFX documents one)
  setMode(upscale|framegen): forward intent only
  isAvailable(): false until a real protocol + redistribution terms exist
```

---

## 4. Frame generation

Temporal frame generation needs motion vectors (or optical flow), depth, HUD-safe presentation, and pacing with MC’s frame loop.

**0.2.0 status:** `NoopFrameGenBackend` only. Config forces `frameGenEnabled=false`. No FG claims.

---

## 5. Summary for 0.2.0

| Backend | Implemented? | Available at runtime? |
|---------|--------------|------------------------|
| FSR | Stub (FSR1 spike did not land a safe hook) | No |
| XeSS | Stub | No |
| FrameFX | Stub adapter | No |
| Frame gen | Stub | No |

QuillCore loads successfully with all backends unavailable. Selecting them must not crash the game. CSR remains permanently excluded.
