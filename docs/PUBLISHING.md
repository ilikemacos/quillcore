# Publishing — Modrinth / CurseForge

QuillCore is MIT. Publish the **non-sources** jar from `build/libs/quillcore-<version>.jar` (not `-sources` / `-dev`).

## Modrinth

| Field | Value |
|-------|-------|
| Project slug | `quillcore` (claim if free) |
| Title | QuillCore |
| Summary | One core. Smoother worlds. Fabric optimization suite for 1.21.8. |
| Categories | Optimization, Utility |
| Loaders | Fabric |
| Game versions | 1.21.8 (add 1.21.9+ only after verified port) |
| License | MIT |
| Source | https://github.com/ilikemacos/quillcore |
| Issues | https://github.com/ilikemacos/quillcore/issues |
| Discord / site | https://chopstickshq.com/ |
| Icon | `src/main/resources/assets/quillcore/icon.png` (128×128) |

Do **not** claim FSR / XeSS / frame-gen performance. Honest copy: Logic + Render cutoffs/throttles; upscale backends stubbed until a safe hook exists.

## CurseForge

| Field | Value |
|-------|-------|
| Name | QuillCore |
| Game | Minecraft |
| Game versions | 1.21.8 |
| Mod loaders | Fabric |
| Categories | Performance, Server Utility (and Client if listed) |
| License | MIT |
| Logo | same 128×128 icon |

## GitHub Release

Prefer `gh release create` with the built jar + CHANGELOG section for that tag. CI builds on push; release artifacts should match a green `./gradlew build test`.
