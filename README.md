# QuillCore

**One core. Smoother worlds.**

Open-source Fabric optimization suite for Minecraft Java Edition. QuillCore combines Quill Logic (game-logic optimizations) and Quill Render (client visual cutoffs) with modular upscaling / frame-generation backends.

QuillCore is an **independent implementation**. Quill Render is being built as its own clean-room chunk renderer that **replaces** Sodium, QuillCore declares Sodium incompatible (`breaks`) and will not launch alongside it. It does **not** use Sodium's source and does **not** imply endorsement by the Sodium or Lithium projects. Coming from Sodium? Chunk rendering stays vanilla until the new renderer ships.

## Status: 0.4.0

| Area | Status |
|------|--------|
| Fabric mod loads / builds | Implemented |
| Persistent config + `/quillcore` | Implemented |
| ModMenu + Cloth Config screen | Implemented (soft deps, optional) |
| Idle distant-mob AI throttle (Quill Logic) | Implemented |
| Villager Brain.tick throttle (Quill Logic) | Implemented |
| Item/XP merge-scan throttle (Quill Logic) | Implemented |
| Path navigation idle throttle (Quill Logic) | Implemented |
| Entity render-distance cutoff (Quill Render) | Implemented |
| Sign/skull BE render cutoff (Quill Render) | Implemented |
| Particle spawn-distance cutoff (Quill Render) | Implemented |
| Cloud distance cap (Quill Render, opt-in) | Implemented (default off) |
| Weather radius cap (Quill Render, opt-in) | Implemented (default off) |
| Bench tick/frame sampling | Implemented (no fabricated Sodium numbers) |
| FrameFX external launcher | Implemented (Windows probe/launch only; not in-process) |
| FSR1 spatial (EASU+RCAS) | **Implemented**, main-RT scale + AMD MIT shaders |
| CSR | Permanently excluded |

## Supported versions

- Minecraft **1.21.8** (primary)
- Fabric Loader **≥ 0.19.5**
- Fabric API **0.136.1+1.21.8** (or compatible)
- Java **21+** (Gradle/Loom on this project require a **Java 25** JVM to *build*; mod bytecode targets 21)

## Install

1. Install Fabric Loader for Minecraft 1.21.8.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api).
3. (Optional) Install [ModMenu](https://modrinth.com/mod/modmenu) + [Cloth Config](https://modrinth.com/mod/cloth-config) for the in-game settings screen.
4. Drop `quillcore-0.4.0.jar` from [Releases](https://github.com/ilikemacos/quillcore/releases) (or `build/libs/` after building) into your `mods/` folder.
5. Launch the game. Config: `config/quillcore.json` or Mod Menu → QuillCore.

## Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64   # or any JDK 25+
./gradlew build test
```

Jar output: `build/libs/quillcore-0.4.0.jar`

## Settings screen

Options → Video Settings opens the QuillCore video settings screen (also via ModMenu). It covers the vanilla
video options plus QuillCore render cutoffs, caps, logic throttles and FSR 1. Changes are staged until
**Apply**/**Done**; **Undo** reverts pending edits. The original vanilla screen is under **Advanced**.

## Commands (permission level 2)

| Command | Effect |
|---------|--------|
| `/quillcore status` | Config + backend availability + FrameFX probe |
| `/quillcore reload` | Reload `quillcore.json` |
| `/quillcore reset` | Restore defaults |
| `/quillcore set <key> <value>` | Change a config field |
| `/quillcore framefx probe\|launch` | Probe or launch Universal-FrameFX externally (Windows; no CSR flags) |
| `/quillcore bench start\|stop\|report` | Sample tick/frame times (empty if unmeasured) |

## What the optimizations do

- **Idle AI throttle:** far idle mobs skip `GoalSelector` ticks for a few ticks; sensing and navigation still run.
- **Villager brain throttle:** far idle villagers skip `Brain.tick` the same way; never while trading, panicking, or hurt.
- **Item/XP merge throttle:** far item entities / XP orbs skip neighbour-merge scans; pickup and age/despawn stay exact.
- **Path navigation throttle:** far idle mobs with no active path skip `PathNavigation.tick`; active pathing never interrupted.
- **Entity render cutoff:** client skips drawing entities beyond 128 blocks by default (camera/vehicle exempt).
- **BE render cutoff:** client skips distant **signs and skulls** only (chests/furnaces never skipped).
- **Particle spawn cutoff:** client skips creating particles beyond 64 blocks from the camera.
- **Cloud distance cap (opt-in):** further caps vanilla Cloud Distance; default off.
- **Weather radius cap (opt-in):** caps rain/snow column gather radius; default off (visible edge if too low).

Defaults are conservative. Flip `masterEnabled`, `renderEnabled`, or `logicEnabled` to disable families of features.

## Upscaling / frame generation

| Backend | Status |
|---------|--------|
| **AMD FSR1** (`fsr1`) | **Working path:** shrinks the vanilla main `RenderTarget` before `renderLevel`, runs **EASU + RCAS** (AMD FidelityFX FSR1, MIT), restores native resolution before GUI/HUD. Quality presets map to AMD render scales. |
| CSR | Permanently excluded, never labelled as FSR. |

**Sodium:** not supported, QuillCore replaces it and Fabric will refuse to load both. FSR1 uses the vanilla main target.

**License:** FSR1 algorithm © AMD, MIT, see `third_party/amd-fsr1/`. FSR2/3 SDK licenses are separate; QuillCore does not ship those SDKs.


## Benchmarks

Run `/quillcore bench start`, play a fixed scenario for at least 60 seconds, then `/quillcore bench report`. Compare runs only on the same hardware, driver, Minecraft version, seed, render distance and FPS cap. `./gradlew quillTest` runs the automated suite (see docs/TESTING.md). `scripts/bench-compare.sh` diffs two reports.

## Porting and publishing

Built against Minecraft 1.21.8 only. To try a newer 1.21.x, bump the versions in `gradle.properties`, fix any renamed mixin targets, check ModMenu and Cloth Config versions, and rerun `./gradlew build test`. Releases ship the non-sources jar from `build/libs/`.

## License

MIT, see [LICENSE](LICENSE).

## Mesh cache in system RAM

`meshRamCacheGb` in `config/quillcore.json` (0 to 16, off by default) sets how much system RAM QuillCore may use to keep chunk meshes that were pushed out of video memory. When a section scrolls back into view it re-uploads from RAM instead of being rebuilt.

This does **not** give your GPU more VRAM. No mod can do that: the graphics driver owns video memory, and on laptops with shared memory the BIOS/driver decides the split. QuillCore only tracks its own mesh uploads against a budget (evicting at 95%, back down to 85%) and throttles re-uploads per frame (8 MB) to avoid stutter. The budget is half of the dedicated VRAM the driver reports (NVIDIA and AMD), or 1 GB if it reports nothing. Only sections that already have a RAM copy are evicted, so nothing has to be rebuilt. A section coming back can skip one frame if that frame's re-upload quota is spent. Translucent layers are not cached.
