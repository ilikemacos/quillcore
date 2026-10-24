# CSR add-on

Two optional features built on the small CSR networks from Universal-FrameFX (MIT, see
`third_party/universal-framefx/`). Both are off by default.

| Feature | Config key | What it does |
|---|---|---|
| CSR Flux | `csrFlux` | Renders the world at half width and height and upscales 2x with the Flux network. The HUD stays native. |
| CSR Frame Gen | `csrFrameGen` | Shows one interpolated frame between each pair of real frames. Adds latency. |

Toggle them in `config/quillcore.json` or under CSR in the Cloth Config screen.

## Layout

```
csr/                 plain Java, unit tested
  CsrWeightsLoader   reads and validates the weight files
  CsrFluxCpu         CPU version of the Flux network, used as the reference for the shader
  CsrFeature         per session on/off state, warns once
  CsrPacing          frame gen pacing
  CsrCost            MAC and fetch counts
csr/client/          GL side
  CsrGl              capability check, shader, texture and FBO helpers, state save and restore
  CsrPass, CsrConvNet
  CsrFlux, CsrFrameGen
  CsrController      hooks, fail safe, HUD note
```

Minecraft's `GpuTexture` only has 8 bit formats, so the passes use their own RGBA16F textures and FBOs through
LWJGL. Each conv layer packs 4 channels per attachment and uses up to 5 attachments. Weights sit in one R32F
texture per network and are read with `texelFetch`.

Every CSR section saves the GL state it touches and restores it afterwards. State cached by `GlStateManager`
is only changed through `GlStateManager`.

GL textures are bottom up and the reference kernels are top down. The shaders flip the conv row, the shuffle
row parity and the y component of flow vectors to match.

## Fail safe

A feature turns itself off for the session, with one warning, when:

- the weights are missing or invalid
- a required GL feature is missing
- a shader fails to compile or link, or an FBO is incomplete
- a GL error is raised inside a CSR section
- a hook throws

The frame then continues as vanilla.

## Flux

1. Before `renderLevel` the main target is resized to half size, so the world draws a quarter of the pixels.
2. Before the GUI: luma, conv 1→8, 8→8, 8→8, 8→4, then pixel shuffle plus a bilinear residual at 2x, then a
   CAS style sharpen. The target goes back to native size and the result is copied in.

Flux and FSR1 both resize the main target, so Flux won't start while `upscaleBackendId` is `fsr1`.

The network is 1512 MACs per low res pixel, about 0.78 GMAC for a 1080p frame.

## Frame Gen

The hook runs in `Minecraft.runTick` just before the vanilla swap. For each real frame N:

1. N is copied into a two frame history, plus a copy from before the GUI for the HUD mask.
2. If N-1 exists at the same size, motion is estimated, then PackA, netA at quarter res, PackB, netB at half
   res and Compose at t = 0.5 write the interpolated frame to the default framebuffer.
3. The interpolated frame is presented, pacing runs, then N is presented by the vanilla swap.

It doesn't create game frames, doesn't lower input latency and doesn't change the FPS counter or bench numbers.
Frame N reaches the screen about one real frame later than in vanilla. A note is drawn in the top right while
it is on.

Pacing:

| Setting | Behaviour |
|---|---|
| VSync on | No sleep. The driver spaces the two swaps evenly. Can cap the real frame rate at half the refresh rate. |
| VSync off with an FPS cap | Sleeps `min(period / 2, 20 ms)` between the two presents. |
| VSync off, unlimited | No pacing, the presents go back to back. A warning says so. |

Motion estimation is simpler than the FgV2 block matcher the networks were trained on: a luma pyramid,
3×3 SAD block match, forward and backward vectors, a consistency check for occlusion, a HUD mask from the pre
GUI copy and a scene cut test. Expect more warping on fast motion.

netA is 8262 MACs per quarter res pixel and netB 918 per half res pixel, about 1.5 GMAC per 1080p frame.

## Credit

`csr_ai_x2.weights.json` (Flux v1) and `csr_ai_fg.weights.json` (Frame Gen v3) are unmodified copies from
Universal-FrameFX under the MIT license. The GLSL passes and the Java reference are a port of its `CsrAi.hlsl`,
`CsrAiFg.hlsl` and `CSRAIFG_Pack*/Compose` kernels.
