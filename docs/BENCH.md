# QuillCore benchmark methodology

**Honesty rule:** never invent FPS, 1% lows, or Sodium/Lithium comparisons. Empty cells mean “not measured.”

## In-game (built-in harness)

1. Install QuillCore (+ Fabric API) on Minecraft 1.21.8.
2. Join a world. Run:
   ```
   /quillcore bench start
   ```
3. Play a fixed scenario for ≥60 seconds (same seed, same view, same settings).
4. Run:
   ```
   /quillcore bench report
   /quillcore bench stop
   ```
5. Record the printed tick/frame stats. Fields stay empty if no samples were collected (e.g. headless CI with no render loop).

Sampling is also enabled when `benchEnabled=true` in `config/quillcore.json`.

## Comparing vs vanilla / Sodium + Lithium (on a real PC)

Use identical hardware, Minecraft 1.21.8, Java 21, render distance, simulation distance, world seed, camera path, and VSync/FPS cap.

Suggested scenarios:

| ID | Setup |
|----|--------|
| `vanilla` | QuillCore off (`masterEnabled=false`) or unloaded |
| `quillcore` | QuillCore defaults |
| `sodium_lithium` | Sodium + Lithium only (no QuillCore) |
| `all` | Sodium + Lithium + QuillCore |

For each scenario, capture:

- Average FPS / 1% low (Spark, MSI Afterburner, or `/quillcore bench report` frame fields)
- Server tick ms (Spark or harness)
- Memory / alloc rate if available

Do **not** mix results across different GPUs, drivers, or Minecraft versions.

## Headless / CI

`./gradlew test` covers pure decision logic only. It does **not** produce FPS numbers. `runClient` under Xvfb can collect frame samples if the render loop runs; treat those as smoke, not published benchmarks.
