# QuillCore

Fabric performance mod for Minecraft Java 1.21.8. It throttles server-side work for mobs and items far from players and trims client rendering that you can't see anyway.

It is written from scratch and uses no code from Sodium or Lithium. Quill Render is growing its own chunk renderer, so QuillCore won't load next to Sodium. Until that renderer lands, chunks are drawn by vanilla.

## Features

Server (Quill Logic):

- Idle AI throttle: far idle mobs skip a few goal selector ticks. Sensing and navigation still run.
- Villager brain throttle: far idle villagers skip `Brain.tick` the same way, but never while trading, panicking or hurt.
- Item and XP merge throttle: far items and orbs scan for merges less often. Pickup, age and despawn are unchanged.
- Path navigation throttle: far idle mobs with no path skip `PathNavigation.tick`. Active paths are never interrupted.

Client (Quill Render):

- Entity cutoff: entities past 128 blocks aren't drawn. The camera entity and vehicle are exempt.
- Block entity cutoff: distant signs and skulls aren't drawn. Chests and other animated block entities are left alone.
- Particle cutoff: particles aren't spawned past 64 blocks from the camera.
- Cloud distance cap (off by default).
- Weather radius cap (off by default). Low values show a visible edge.
- FSR1 upscaling (off by default): renders the world at a lower scale and upscales it with AMD FSR1 (EASU + RCAS) before the HUD is drawn.

There is also a launcher for [Universal-FrameFX](https://github.com/ilikemacos/universal-framefx) on Windows. It runs as a separate app.

Defaults are conservative. `masterEnabled`, `renderEnabled` and `logicEnabled` turn whole groups off.

## Install

1. Install Fabric Loader 0.19.5+ and [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.8.
2. Optional: [ModMenu](https://modrinth.com/mod/modmenu) and [Cloth Config](https://modrinth.com/mod/cloth-config) for the settings screen.
3. Put the QuillCore jar from [Releases](https://github.com/ilikemacos/quillcore/releases) in `mods/`.

Settings live in `config/quillcore.json`.

## Settings screen

Options → Video Settings opens the QuillCore settings screen. ModMenu opens it too. It has the vanilla video options plus the QuillCore cutoffs, caps, throttles and FSR1. Changes wait until you press Apply or Done, and Undo drops them. The vanilla screen is still under Advanced.

## Commands

Permission level 2.

| Command | |
|---|---|
| `/quillcore status` | Current settings |
| `/quillcore reload` | Reload the config file |
| `/quillcore reset` | Restore defaults |
| `/quillcore set <key> <value>` | Change a setting |
| `/quillcore framefx probe\|launch` | Find or start Universal-FrameFX |
| `/quillcore bench start\|stop\|report` | Sample tick and frame times |

## Building

Gradle needs JDK 25. The mod targets Java 21.

```
./gradlew build
```

See [docs/TESTING.md](docs/TESTING.md) for the test engine and benchmark.

## License

MIT, see [LICENSE](LICENSE).
FSR1 is © AMD, MIT licensed, see `third_party/amd-fsr1/`.
