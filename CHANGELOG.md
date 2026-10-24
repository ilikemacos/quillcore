# Changelog

## Unreleased

- QuillCore no longer loads next to Sodium
- Compact 13 byte block vertex format, not used for rendering yet
- Bench reports vanilla vs compact vertex bytes for a sample section
- Bench reports 0.1% low FPS and runs with no frame cap

## 0.4.0 (2026-10-22)

- FSR1 upscaling (EASU + RCAS) with quality presets, sharpness and a render scale override
- HUD is drawn after the upscale, at native resolution
- Config version 5

## 0.3.1 (2026-10-22)

- Path navigation throttle for far idle mobs
- Optional cloud distance and weather radius caps
- Mod icon
- Config version 4

## 0.3.0 (2026-10-22)

- Particle spawn cutoff
- Item and XP orb merge throttle
- Universal-FrameFX launcher (`/quillcore framefx`)
- Config version 3

## 0.2.0 (2026-10-22)

- Entity and sign/skull render cutoffs
- Villager brain throttle
- ModMenu and Cloth Config screen
- `/quillcore reset`
- Tick and frame time sampling for `/quillcore bench`
- CI

## 0.1.0 (2026-10-22)

- Fabric 1.21.8 mod with config file and `/quillcore` command
- Idle AI throttle for far mobs
