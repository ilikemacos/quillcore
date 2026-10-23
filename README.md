# QuillCore

**One core. Smoother worlds.**

Open-source Fabric optimization suite for Minecraft Java Edition. QuillCore combines Quill Logic (game-logic optimizations) and Quill Render (client visual cutoffs) with modular upscaling / frame-generation backends.

Inspired by the *goals* of projects like Sodium and Lithium. QuillCore is an **independent** implementation, it does **not** copy their source and does **not** imply endorsement.

## Status: 0.2.0

| Area | Status |
|------|--------|
| Fabric mod loads / builds | Implemented |
| Persistent config + `/quillcore` | Implemented |
| ModMenu + Cloth Config screen | Implemented (soft deps, optional) |
| Idle distant-mob AI throttle (Quill Logic) | Implemented |
| Villager Brain.tick throttle (Quill Logic) | Implemented |
| Entity render-distance cutoff (Quill Render) | Implemented |
| Sign/skull BE render cutoff (Quill Render) | Implemented |
| Bench tick/frame sampling | Implemented (no fabricated Sodium numbers) |
| FSR / XeSS / FrameFX / frame gen | Stub backends only, all report unavailable |
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
4. Drop `quillcore-0.2.0.jar` from [Releases](https://github.com/ilikemacos/quillcore/releases) (or `build/libs/` after building) into your `mods/` folder.
5. Launch the game. Config: `config/quillcore.json` or Mod Menu → QuillCore.

## Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64   # or any JDK 25+
./gradlew build test
```

Jar output: `build/libs/quillcore-0.2.0.jar`

## Commands (permission level 2)

| Command | Effect |
|---------|--------|
| `/quillcore status` | Config + backend availability |
| `/quillcore reload` | Reload `quillcore.json` |
| `/quillcore reset` | Restore defaults |
| `/quillcore set <key> <value>` | Change a config field |
| `/quillcore bench start\|stop\|report` | Sample tick/frame times (empty if unmeasured) |

## What the optimizations do

- **Idle AI throttle:** far idle mobs skip `GoalSelector` ticks for a few ticks; sensing and navigation still run.
- **Villager brain throttle:** far idle villagers skip `Brain.tick` the same way; never while trading, panicking, or hurt.
- **Entity render cutoff:** client skips drawing entities beyond 128 blocks by default (camera/vehicle exempt).
- **BE render cutoff:** client skips distant **signs and skulls** only (chests/furnaces never skipped).

Defaults are conservative. Flip `masterEnabled`, `renderEnabled`, or `logicEnabled` to disable families of features.

## Upscaling / frame generation

All backends currently report **unavailable**. See the README. Do not expect FSR/XeSS/FrameFX visual changes in 0.2.0.

## Benchmarks

Run `/quillcore bench start`, play a fixed scenario for at least 60 seconds, then `/quillcore bench report`. Compare runs only on the same hardware, driver, Minecraft version, seed, render distance and FPS cap. `./gradlew quillTest` runs the automated suite (see docs/TESTING.md). `scripts/bench-compare.sh` diffs two reports.

## Porting and publishing

Built against Minecraft 1.21.8 only. To try a newer 1.21.x, bump the versions in `gradle.properties`, fix any renamed mixin targets, check ModMenu and Cloth Config versions, and rerun `./gradlew build test`. Releases ship the non-sources jar from `build/libs/`.

## License

MIT, see [LICENSE](LICENSE).
