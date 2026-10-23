# QuillCore technical feasibility — FSR, XeSS, FrameFX, frame generation

**Minecraft target:** 1.21.8 (Fabric)  
**Milestone:** 0.1.0  
**Date:** 2026-10-22 (America/Vancouver)

This document records what was investigated. **Nothing here is claimed as a working integration.** Stub backends in `upscale/` and `framegen/` always report unavailable.

---

## 1. AMD FidelityFX Super Resolution (FSR)

| Topic | Finding |
|-------|---------|
| Spatial FSR 1 | Open license historically; quality is limited; still needs a clean color buffer at render resolution. |
| Temporal FSR 2/3 | GPUOpen SDK; requires motion vectors, depth, and reactive/exposure masks for good results. |
| MC pipeline | Vanilla + Fabric use OpenGL. Iris/Sodium alter the deferred/forward path. Hooking temporal upscaling means custom framebuffer management and passing per-pixel motion/depth — non-trivial and renderer-mod specific. |
| 0.1.0 status | **Not implemented.** `FsrBackend` is a stub. Do not bilinear-scale and label it FSR. |

**Blockers:** motion-vector generation in the active renderer; licensing/SDK packaging for a redistributed Fabric mod; GPU feature detection and fallbacks.

**Graceful fallback:** backend remains registered, `isAvailable() == false`, config can select it without crashing.

---

## 2. Intel XeSS

| Topic | Finding |
|-------|---------|
| Official SDK | Intel XeSS SDK targets DirectX 12 and Vulkan primarily. |
| MC default API | OpenGL (LWJGL). Vulkan exists via third-party renderers, not vanilla Fabric. |
| Hardware | Best quality on Intel Arc with XMX; DP4a fallback on other GPUs where supported. |
| 0.1.0 status | **Not implemented.** `XessBackend` is a stub. |

**Blockers:** no supported OpenGL integration path for full XeSS; Vulkan-only mods would be a different architecture; must not mislabel a generic scaler as XeSS.

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
| Launch/control FrameFX externally (process IPC) while MC runs | Possible later as an **optional OS helper**, not an upscale “backend” inside the GL pipeline. Not built in 0.1.0. |
| Reimplement FrameFX algorithms inside QuillCore | Out of scope; would not be “FrameFX”. |

**0.1.0 status:** `FrameFxBackend` adapter stub only — `isAvailable() == false`, reason documents the overlay architecture.

**Designed future adapter (NOT IMPLEMENTED):**

```text
FrameFxBackend
  id = "framefx"
  probe(): check OS == Windows && optional companion EXE present
  connect(): IPC to companion (undefined protocol — do not invent)
  setMode(upscale|framegen): forward intent only
  isAvailable(): false until a real protocol + redistribution terms exist
```

---

## 4. Frame generation

Temporal frame generation (FSR FG, XeSS FG, Optical-flow style, etc.) needs:

- Motion vectors (or a reliable optical-flow substitute)
- Depth / HUD-safe presentation path
- UI overlay handling (hotbar, hand, menus must not duplicate wrongly)
- Pacing / tear control with MC’s frame loop (SDL-era windowing on newer versions; 1.21.8 still GLFW-era)

**0.1.0 status:** `NoopFrameGenBackend` only. No FG claims.

---

## 5. Summary for 0.1.0

| Backend | Implemented? | Available at runtime? |
|---------|--------------|------------------------|
| FSR | Stub | No |
| XeSS | Stub | No |
| FrameFX | Stub adapter | No |
| Frame gen | Stub | No |

QuillCore loads successfully with all backends unavailable. Selecting them must not crash the game.
