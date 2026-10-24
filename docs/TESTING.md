# Testing

```
./gradlew quillTest              # JUnit and GameTest, Logic on and off
./gradlew quillTest -PwithBench  # also runs the client bench, needs a real GPU
```

Gradle needs JDK 25 (Loom 1.18). Reports go to `build/quill-test-report/`.

Each run first moves the previous report to `build/quill-test-reports/<yyyyMMdd-HHmmss>-<commit>/`.

| Layer | Checks | Output |
|---|---|---|
| JUnit (`src/test`) | Config clamping and migration, throttle math, bench metrics and report | `build/test-results/test` |
| GameTest (`src/gametest`) | Real server ticks with the Logic throttles on (maxSkip=10) and off: item merging, item age, hoppers, pathfinding, villager brains, gravity | `gametest-on.xml`, `gametest-off.xml` |
| Client bench (`-PwithBench`) | Fixed seed world, 20 s warmup, then on/off/on/off segments (`-PbenchSeconds`, default 30) while panning the camera, fullscreen at native resolution | `bench.json`, `bench.csv` |

`index.html` is an offline dashboard with per-layer results, failed tests, FPS and frame time charts and run info. It opens fullscreen in Edge or Chrome. `-PnoOpen` (or `CI`) skips that. `summary.md` has the same content.

If `~/.quillcore/upload-token` exists, results are uploaded to the dashboard in `dashboard/` and the run page opens. `-PnoUpload` skips the upload and `-PquillDashboardUrl` changes the URL. Don't commit the token.

A bench run is marked INVALID when GL_RENDERER isn't the NVIDIA GPU (`-PallowAnyGpu` overrides), a segment sits at an FPS cap, or the window lost focus. A test-only mixin turns off Minecraft's AFK and unfocused frame limits during the bench. Use `-Dquillcore.autobench.windowed=true` to run windowed.

The task fails if a layer fails or a requested layer wrote no report.

With Logic on, resting item stacks merge later (scans every 40 ticks, up to maxSkip skips, about 440 ticks worst case). The end state matches vanilla and the merge test allows 1000 ticks.
