# Porting notes — Minecraft 1.21.8 and higher

**Primary build:** Minecraft **1.21.8** (Fabric Loader 0.19.5, Fabric API 0.136.1+1.21.8).

## Higher 1.21.x

QuillCore is not multi-version yet. To try a newer 1.21.x patch:

1. Bump `minecraft_version` / `fabric_api_version` in `gradle.properties` to versions from https://fabricmc.net/develop
2. Rebuild and fix mixin targets if Mojang renamed methods (`EntityRenderDispatcher.shouldRender`, `Villager.customServerAiStep`, `Mob.serverAiStep`, `BlockEntityRenderDispatcher.render`).
3. Re-verify ModMenu / Cloth Config artifact versions for that game version.
4. Re-run unit tests + `runClient` smoke.

Mixin-heavy render code is the highest breakage risk across patches.
