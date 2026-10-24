# CSR add-on (optional, off by default)

QuillCore ships two optional features built on the small CSR networks from
[Universal-FrameFX](../third_party/universal-framefx/) (MIT; copyright as stated in its LICENSE):

| Feature | Config key | Default | What it does |
|---------|-----------|---------|--------------|
| **CSR Flux** | `csrFlux` | `false` | Renders the world at half width and height, then upscales 2x with the CSR Flux network. HUD stays native. |
| **CSR Frame Gen** | `csrFrameGen` | `false` | Shows one interpolated frame between each pair of real frames. Adds latency (see below). |

Both are toggled in the config file (`config/quillcore.json`, schema v6) or in the
ModMenu screen under **CSR (optional)**. Neither is labelled as FSR, and neither is related to the
`upscaleBackendId` / `fsrFrameGenEnabled` settings.

> **Verification status.** This environment has no GPU or display. The Java side is built and unit-tested.
> The GLSL has **not** been compiled or run on real hardware (no offline GLSL validator was available either),
> and no GameTests or client runs were done. Everything below about runtime cost is an *estimate from operation counts*,
> not a measurement.

## Architecture

The port uses plain GLSL 330 fragment passes on the game's own framebuffer. There is no ONNX Runtime,
no GPU→CPU readback, and no compute shaders.

```
src/main/java/.../csr/          pure Java, no Minecraft imports (unit-tested)
  CsrLayer, CsrFluxWeights,
  CsrFgWeights                  validated weight records
  CsrWeightsLoader              JSON → CsrResult (never throws)
  CsrFluxCpu                    CPU reference of the Flux network (spec for the GLSL)
  CsrFeature                    session on/disabled state; warns exactly once
  CsrPacing                     frame-gen pacing rule
  CsrCost                       MAC / fetch counts used in this document
src/client/java/.../csr/client/ GL side
  CsrGl                         caps gate, shader/texture/FBO helpers, GL state save/restore
  CsrPass, CsrConvNet           fullscreen program; conv3x3 stack via MRT
  CsrFlux, CsrFrameGen          pipelines
  CsrController                 hooks + fail-safe + HUD note
mixins: GameRendererCsrMixin (Flux), MinecraftCsrFgMixin (Frame Gen)
assets/quillcore/csr/           weights (unmodified copies) + glsl/
```

**Why raw GL.** Minecraft 1.21.8's `GpuTexture` only has 8-bit colour formats (`RGBA8`, `RED8`, `RED8I`).
That would quantise the activations between layers. So the CSR passes allocate their own `RGBA16F` textures and
FBOs through LWJGL, packing 4 channels per attachment and using up to 5 attachments (MRT) per conv layer.
Weights live in one `R32F` texture per network (1024 texels wide) and are read with `texelFetch`.

**State hygiene.** Every CSR section captures the GL state it touches and restores it afterwards:
read/draw FBO, program, VAO, viewport, active unit, 2D texture and sampler bindings on units 0–11, blend, depth,
cull, scissor, logic-op, polygon-offset, and colour mask. State that `GlStateManager` caches is changed only through
`GlStateManager`, so its cache stays in sync with the driver. Unpack state (PBO binding, alignment, row length, skips)
is reset around the one weight upload.

**Orientation.** The reference kernels index rows top-down, while GL textures are bottom-up. The GLSL compensates
in three places. The conv kernel row `ky` maps to `dy = 1 - ky`. The shuffle row parity is flipped (exact because
the shuffle runs at exactly 2× the low-res size). Flow vectors have their y component negated when packed for the
networks, and again when unpacked.

### Fail-safe

`CsrFeature` disables a feature for the session and logs **one** warning if any of these happens:
- weights are missing or invalid (wrong version, shape, or count; NaN or Inf; malformed JSON);
- GL 3.2 plus either GL 3.3 or `ARB_explicit_attrib_location` + `ARB_sampler_objects`, 5 colour attachments / draw buffers, or 12 fragment texture units are not available;
- a shader fails to compile or link, or an FBO is incomplete;
- any GL error is raised inside a CSR section (checked every frame);
- any other exception is thrown in a hook.

On failure, the hook restores GL state and the main target size, releases the GPU resources, and the frame
continues as vanilla. Unit tests cover the weights part (`CsrFailSafeTest`); the GL paths are unverified here.

## CSR Flux

**Path chosen: render-scale.** A post-upscale pass on an already-native frame would only add cost. The point is to
render fewer pixels. Flux uses the same two `GameRenderer#render` injection points as FSR1:

1. Before `renderLevel`, resize the main target to `ceil(W/2) × ceil(H/2)`, so the world renders 1/4 of the pixels.
2. After world and post-processing, before the GUI (`FogRenderer.endFrame`), run these passes:
   - luma (Rec.709 coefficients from the weights file);
   - conv 1→8, 8→8, 8→8 (leaky 0.1), then 8→4 (linear), all at low res;
   - pixel-shuffle plus bilinear residual at exactly 2× low res: `rgb = bilinear(LR)`, `out = saturate(rgb + yDelta)`.
     This is the reference's `rgb + (yNet - yUp)`, with the delta added to all three channels;
   - CAS-style sharpen (port of `CSRAI_Sharpen`, fixed strength `Sharpness = 2.0`). This pass is **not** part of the
     network.

   The main target is then resized back to native, and the result is blitted in (with linear scaling only when
   the native size is odd). The GUI draws at native resolution.

Flux and the FSR1 backend both resize the main target, so they cannot run together. If `upscaleBackendId` is
`fsr1` or `fsr`, Flux does not start, and a one-time warning is logged.

`CsrFluxCpu` is a straight CPU port of the same semantics. Tests check:
- the output is 2W×2H;
- with zeroed weights, the output equals the bilinear upscale exactly (residual identity);
- real weights keep a constant image in [0,1] and are deterministic;
- a last-layer bias on channel 0 shifts exactly the (even, even) HR pixels.

The GLSL itself has not been checked against the CPU version; that needs a manual run on a GPU.

## CSR Frame Gen

### What it does

The hook sits in `Minecraft.runTick`, just before the vanilla `Window.updateDisplay` swap. At that point the
main target holds the finished real frame N, world plus HUD, already blitted to the back buffer. For each
real frame N:

1. Copy N into a 2-entry history ring. A HUD-free copy of N, taken before the GUI pass, is kept for the HUD mask.
2. If N-1 exists at the same size:
   - run the FgV2 motion estimator (see below);
   - run PackA, then **netA** at 1/4 res (8→18→18→18→7);
   - run PackB, then **netB** at 1/2 res (12→6→5);
   - run Compose at t = 0.5 straight into the default framebuffer.

   These are ports of `CSRAIFG_PackA`, `CSRAIFG_PackB` and `CSRAIFG_Compose`, including the
   `maskA`/`maskB` logit, the `okA`/`okB` edge rules, the v3 sigmoid on `lp`, and the rgb residual.
   On a scene cut the nearest real frame is used; HUD pixels take the current frame.
3. `glfwSwapBuffers` presents interp(N-1, N), then pacing runs (below). Then N is blitted back into the back
   buffer, and the vanilla swap presents N.

So the display receives `… interp(N-1,N), N, interp(N,N+1), N+1 …`, which is two presents per real frame.

### What it does not do

- It does **not** insert frames into a separate swap chain or present asynchronously. Minecraft presents on its
  render thread, and the extra present is a second synchronous `glfwSwapBuffers` on that same thread.
- It does **not** create game frames. Nothing is simulated or rendered for the interpolated image; it is a blend
  of N-1 and N.
- It does **not** reduce input latency. It **adds** latency (below).
- It does **not** change Minecraft's FPS counter, tick rate or bench sampling. F3 and `/quillcore bench` keep
  reporting **real** frames only. QuillCore never reports the doubled present count as FPS.

### Latency

Frame N cannot be shown until interp(N-1, N) has been computed and presented. Compared with vanilla, N reaches
the screen later by the time for the whole Frame Gen pass plus the pacing gap. With VSync, it also waits one
extra refresh slot in the swap queue. That adds up to roughly one real frame of extra input-to-photon latency.
The interpolated image itself shows motion halfway between two frames that already happened.

When enabled, this is logged as a warning, and a small note is drawn on screen (top right):
**"CSR Frame Gen: +1 frame latency, interpolated frames"**.

### Pacing (what is genuinely possible)

| Setting | Behaviour |
|---------|-----------|
| VSync on | No sleep. The driver queues both swaps and shows them at successive vblanks. This is the only mode where the extra frames are evenly spaced. If the swap queue is full, the second swap blocks, which can cap the real frame rate at refresh/2. |
| VSync off, FPS cap < Unlimited | After the interpolated present, sleep `min(period/2, 20 ms)`, where `period = 1/cap`, then present N. If the cap was not already the bottleneck, this **lowers the real frame rate**. |
| VSync off, Unlimited | No pacing is possible. The two presents go back to back (tearing, no smoothness gain). A warning says so. |

### Motion estimation

The flow inputs come from a GLSL port of the FgV2 block-match (`me_*.fsh`, `gm_*.fsh`, `fg_packa.fsh`):

- **Pyramid:** 4-level R16F luma pyramid, full res down to 1/8, ping-ponged so the previous frame's levels are
  reused.
- **Global motion:** 64×36 grid, 25×13 cost surface, 1×1 pick. Fed to the tile search as a candidate.
- **Tile search (per direction, bwd then fwd):** block consensus, P8 on L3 (±4 SAD + census, history and
  global candidates), propagation, P4 on L2, then a Lucas-Kanade refine on L1. The previous frame's result is
  the history candidate.
- **Occlusion:** forward/backward consistency, as in the reference.
- **PackA** picks per-tile vectors with `pickTileV`. Vectors are y-flipped and divided by `flowScale` (32).

Known differences from the reference:

- Fetches clamp to the level size, not the tile grid.
- Tiles and blocks are aligned from the bottom edge (GL orientation).
- Ties between equal-cost candidates may break in a different order.

Not ported:

- affine / RANSAC global snap;
- `StaticCount` / `HudMask` (the pre-GUI diff mask described below is used instead);
- `FG2_SceneCut` (scene cut is the mean |Δluma| over the coarsest pyramid level, above 0.15);
- the wide-search, ambiguity, temporal and edge-smooth modes.

HUD mask: pixels where the final frame differs from the pre-GUI frame by more than 2/255.

## GPU timers

When GL 3.3 or `ARB_timer_query` is present, every pass is wrapped in a `GL_TIME_ELAPSED` query. Each pass has two
queries used alternately, and a result is read only once `GL_QUERY_RESULT_AVAILABLE` says so, so the CPU never
waits. Samples that aren't ready are skipped. Averages are logged every ~5 s
(`CSR GPU ms/pass (5s avg): …`) and written to the bench report as `gpuPassMs`.

## Per-pass cost estimates

These counts come from the shaders (`CsrCost` / `CsrCostTest` check the MAC and conv-fetch numbers). "Fetches" are
texture reads of RGBA16F/RGBA8 data. On top of those, every MAC also reads its weight from the weight texture
(one `texelFetch` per weight, tiny and cache-resident). That is probably the dominant ALU/latency cost of the conv
passes. Real milliseconds depend on the GPU and driver, and **have not been measured**.

### Flux (per frame, 1920×1080 output → 960×540 internal)

| Pass | Res | MACs/px | Data fetches/px | Notes |
|------|-----|--------:|----------------:|-------|
| luma | LR | 3 | 1 | |
| conv 1→8 | LR | 72 | 9 | |
| conv 8→8 | LR | 576 | 18 | 2 slabs × 9 taps |
| conv 8→8 | LR | 576 | 18 | |
| conv 8→4 | LR | 288 | 18 | |
| shuffle + residual | 2×LR |, | 1 + 1 bilinear | |
| sharpen | 2×LR |, | 5 | |
| blit to main | native |, | 1 | |

The network total is **1512 MACs** and **63 conv fetches** per LR pixel, which is **≈0.78 GMAC (≈1.6 GFLOP)** per
1080p frame. That has to be weighed against the saving from rendering the world at 1/4 the pixels.

### Frame Gen (per real frame, 1920×1080; netA once per pair, netB once per generated frame)

| Pass | Res (1080p) | MACs/px | Data fetches/px |
|------|-------------|--------:|----------------:|
| history + HUD-free copies | 1920×1080 |, | 2–3 blits |
| luma pyramid | 1920×1080 … 240×135 |, | 1 / 4 / 4 / 4 |
| global motion | 64×36, 25×13, 1×1 |, | small |
| motion search ×2 dirs | 240×135 tiles |, | see `me_*.fsh`; measure with the GPU timers |
| PackA | 480×270 |, | 6 |
| **netA** 8→18→18→18→7 | 480×270 | **8262** | 153 |
| PackB | 960×540 |, | ~20 (7 bilinear + 9 texel + 4 bilinear) |
| **netB** 12→6→5 | 960×540 | **918** | 45 |
| Compose | 1920×1080 |, | ~12 (≈8 bilinear + HUD 2 + misc) |
| extra present + re-blit | 1920×1080 |, | 1 blit + 1 swap |

At 1080p that is netA ≈ **1.07 GMAC** and netB ≈ **0.48 GMAC** per real frame.

## Limits

- No GPU verification in this environment: the shaders are unverified, and so is any shader-vs-CPU parity.
- Flux and FSR1 are mutually exclusive.
- Frame Gen motion estimation leaves out parts of FgV2 (listed above). The HUD mask needs the pre-GUI capture, so on frames where it is missing
  the mask is empty and HUD ghosting is possible.
- Frame Gen only runs in a world (not on the title screen) and pauses while minimised. History is reset on
  resize, so no pair spans a discontinuity.
- Frame Gen pacing is only even with VSync. Without it, see the table above.
- A `glGetError` check runs after every CSR section. It can add a small driver sync.
- The CSR hooks are at frame level: main-target resize and the final present. They should not depend on the
  planned replace-mode chunk renderer (ADR-001), but that combination has not been tested.

## Credit

Model weights `csr_ai_x2.weights.json` (Flux v1) and `csr_ai_fg.weights.json` (Frame Gen v3) are unmodified copies
from Universal-FrameFX, MIT. See `third_party/universal-framefx/` for the license text. The GLSL passes and the
Java CPU reference are a new port derived from its `CsrAi.hlsl`, `CsrAiFg.hlsl` and the `CSRAIFG_Pack*/Compose`
kernels.
