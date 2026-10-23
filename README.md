# QuillCore

**One core. Smoother worlds.**

Open-source Fabric optimization suite for Minecraft Java Edition. QuillCore combines game-logic optimizations (Quill Logic) with a planned render path (Quill Render) and modular upscaling / frame-generation backends.

Inspired by the *goals* of projects like Sodium and Lithium. QuillCore is an **independent** implementation — it does **not** copy their source and does **not** imply endorsement.

## Status: 0.1.0 (milestone)

| Area | Status |
|------|--------|
| Fabric mod loads / builds | Implemented |
| Persistent config + `/quillcore` | Implemented |
| Idle distant-mob AI throttle (Quill Logic) | Implemented (unit-tested decision logic; mixin compile-verified) |
| Quill Render | Not implemented |
| FSR / XeSS / FrameFX / frame gen | Stub backends only — all report unavailable |
| Benchmarks vs vanilla / Sodium+Lithium | Harness exists; **no fabricated numbers** |

## Supported versions

- Minecraft **1.21.8**
- Fabric Loader **≥ 0.19.5**
- Fabric API **0.136.1+1.21.8** (or compatible)
- Java **21+** (Gradle/Loom on this project require a **Java 25** JVM to *build*; the mod bytecode targets 21)

## Install

1. Install Fabric Loader for Minecraft 1.21.8.
2. Install Fabric API.
3. Drop `quillcore-0.1.0.jar` from [Releases](https://github.com/ilikemacos/quillcore/releases) (or `build/libs/` after building) into your `mods/` folder.
4. Launch the game. Config appears at `config/quillcore.json`.

## Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64   # or any JDK 25+
./gradlew build
```

Jar output: `build/libs/quillcore-0.1.0.jar`

Unit tests (no Minecraft runtime required for IdleAiThrottle):

```bash
./gradlew test
```

## Commands (permission level 2)

- `/quillcore status` — config + backend availability
- `/quillcore reload` — reload `config/quillcore.json`
- `/quillcore set <key> <value>` — toggle real settings
- `/quillcore bench start|stop|report` — sampler (empty report until samples exist)

### Config keys

`masterEnabled`, `renderEnabled`, `logicEnabled`, `idleAiThrottleEnabled`, `idleAiThrottleMaxSkipTicks` (0–10), `idleAiThrottlePlayerRadiusBlocks`, `benchEnabled`

## What the 0.1.0 optimization does

**Idle AI throttle:** for server-side `Mob`s that are alive, not recently hurt, have no attack target, and have no player within the configured radius, QuillCore no-ops `GoalSelector.tick` / `tickRunningGoals` for up to N extra opportunities (vanilla already half-ticks AI). Sensing, navigation, and `customServerAiStep` still run.

## Upscaling / FrameFX

All backends are modular stubs that return `isAvailable() == false`. See [docs/FEASIBILITY.md](docs/FEASIBILITY.md).

**CSR is explicitly excluded** from QuillCore.

## License

[MIT](LICENSE)

## Roadmap

**Implemented (0.1.0):** project scaffold, config, idle AI throttle + tests, bench harness fields, upscale/framegen stubs, feasibility report.

**Planned:** Quill Render, real FSR/XeSS only if a legal/technical path exists, FrameFX adapter only if a valid integration appears, honest hardware benchmarks vs vanilla and Sodium+Lithium.
