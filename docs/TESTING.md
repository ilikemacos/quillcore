# QuillCore test engine

One command runs every layer and writes `build/quill-test-report/`:

```
./gradlew quillTest              # JUnit + GameTest (Logic ON and OFF)
./gradlew quillTest -PwithBench  # + automated client benchmark (needs a real GPU)
```

Gradle must run on JDK 25+ (Loom 1.18); the mod still targets Java 21.

| Layer | What it checks | Output |
|---|---|---|
| JUnit 5 (`src/test`) | Pure logic: config clamping/migration, throttle maths, bench metrics/report | `build/test-results/test` |
| Fabric GameTest (`src/gametest`) | Real server ticks with Logic throttles **ON** (aggressive, maxSkip=10) vs **OFF**: item merging, item age, hoppers, pathfinding, villager brains, gravity | `gametest-on.xml`, `gametest-off.xml` |
| Client bench (`-PwithBench`) | Creates a fixed-seed world, warms up 20 s, then runs ON/OFF/ON/OFF segments (`-PbenchSeconds`, default 30) while panning the camera | `bench.json`, `bench.csv` |

`index.html` is a self-contained dashboard (no network): per-layer cards, failed tests with messages,
FPS-over-time and frame-time charts (ON vs OFF overlaid), 1%-low/avg FPS and MSPT bars, and run metadata.
It opens in your browser automatically; pass `-PnoOpen` (or set `CI`) to skip. `summary.md` is the same in Markdown.
The task fails if any layer fails or a requested layer produced no report.

Note: with Logic ON, resting item stacks merge later (merge scans run every 40 ticks and can be skipped up to
maxSkip times, ≈440 ticks worst case). The end state matches vanilla; the merge test allows 1000 ticks.
