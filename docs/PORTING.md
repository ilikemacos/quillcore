# Porting notes — Minecraft 1.21.8 and higher

**Primary build:** Minecraft **1.21.8** (Fabric Loader 0.19.5, Fabric API 0.136.1+1.21.8).

QuillCore is **not** multi-version / multi-loom yet. A second Loom source set was considered for 1.21.9–1.21.11 and deferred: mixin targets and Cloth/ModMenu artifacts still need per-version verification on a GPU client, and this box cannot run `runClient`.

## Verified-from-meta versions (2026-10-22)

Fetched from Fabric Meta / Maven — **not** built or playtested here:

| Minecraft | Loader (latest listed) | Fabric API (latest listed) |
|-----------|------------------------|----------------------------|
| 1.21.8 (primary) | 0.19.5 | 0.136.1+1.21.8 |
| 1.21.9 | 0.20.1 | 0.134.1+1.21.9 |
| 1.21.10 | 0.20.1 | 0.138.4+1.21.10 |
| 1.21.11 | 0.20.1 | 0.141.6+1.21.11 |

Always re-check https://fabricmc.net/develop before bumping.

## How to try a higher 1.21.x patch

1. Bump `minecraft_version` / `fabric_api_version` / `loader_version` in `gradle.properties`.
2. Rebuild; fix mixin targets if Mojang renamed methods. High-risk hooks in this mod:
   - `EntityRenderDispatcher.shouldRender`
   - `BlockEntityRenderDispatcher` render path
   - `ParticleEngine.createParticle`
   - `CloudRenderer.render` (`Math.min` cloud-range)
   - `WeatherEffectRenderer.collectColumnInstances`
   - `Mob.serverAiStep` / `GoalSelector`
   - `Brain.tick` / villager custom AI
   - `ItemEntity.mergeWithNeighbours` / `ExperienceOrb.scanForMerges`
   - `PathNavigation.tick`
3. Re-verify ModMenu + Cloth Config artifact versions for that game version.
4. Re-run `./gradlew build test` and a `runClient` smoke (GPU PC).

## Soft deps (optional UI)

- ModMenu **15.0.2** and Cloth Config **19.0.147** are pinned for 1.21.8.
- Higher patches often need newer ModMenu/Cloth — check Modrinth versions tagged for that MC version.

## Why no second Loom target yet

- Mixin bytecode differs across patches; a shared source set that only compiles against 1.21.8 can silently break on 1.21.11.
- Multi-project Loom is doable but needs CI matrix + client smoke. Prefer one solid primary until FSR/framebuffer work forces a version jump.
