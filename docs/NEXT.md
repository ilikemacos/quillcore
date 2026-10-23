# QuillCore — done for now (post-0.3.1)

**Shipped through 0.3.1 on Fabric 1.21.8.** Further headless work has hit diminishing returns: remaining high-value items need a **Windows GPU PC** and/or a **Sodium-compatible framebuffer API**.

## What 0.3.1 added

- Quill Logic: `PathNavigation` idle throttle (far, path-done, no target).
- Quill Render: opt-in cloud distance cap + weather column radius cap (default **off**).
- Polish: 128×128 icon, PORTING notes for 1.21.9–1.21.11, publishing metadata, README.

## Explicitly not faked / not shipped

| Item | Why blocked |
|------|-------------|
| Spatial FSR1 / EASU+RCAS | Need lower-res main color target **before** `renderLevel`, then upscale **before** GUI. `WorldRenderEvents.END` has no resizable color FBO; blit path is `GpuDevice.presentTexture`; Sodium replaces the path. See `docs/FEASIBILITY.md`. |
| Chunk rebuild coalescing | Vanilla already queues via `CompileTaskDynamicQueue`; Sodium uses a different graph. No honest bench on this box. |
| XeSS / temporal FSR / frame gen | Stubs only (`isAvailable() == false`). |
| CSR | Permanently excluded. |
| Fabricated Sodium/Lithium benches | Never. |

## Needs a Windows GPU PC (or equivalent client)

- Visual smoke: cloud/weather caps edge quality; particle/entity cutoffs at FOV extremes.
- Real FPS / chunk-rebuild benches (`docs/BENCH.md`) vs Sodium / Lithium.
- FrameFX external launch probe on a real Windows install.
- Optional: try bumping to 1.21.9–1.21.11 per `docs/PORTING.md` and fix mixins.

## Needs a Sodium-compatible framebuffer API (upstream or compat module)

- Public post-composite, pre-GUI color attachment (or documented Sodium extension) so FSR1-class spatial upscale can run without forking Sodium.
- Until that exists, keep FSR/XeSS stubs honest.

## Safe future candidates (only with measurement)

- Entity feature-renderer LOD (armor/held-item skip at mid distance) — stretch; cosmetic-only allowlist.
- Redstone/far-chunk cosmetic tick throttle — **default do not ship** without a narrow safe subset.
- Multi-version Loom matrix once 1.21.11 mixins are verified on GPU.

## Process

- Plan: keep this file short; code on box / low-cost model.
- No Cursor IDE required for releases from this repo.
- Bump versions only when meaningful code ships.
