# QuillCore

Fabric performance mod for Minecraft Java 1.21.8. It throttles server-side work for mobs and items far from players and trims client rendering that you can't see anyway.

It is written from scratch. Projects like Sodium and Lithium were an inspiration, but no code is taken from them.

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

There is also a launcher for [Universal-FrameFX](https://github.com/ilikemacos/universal-framefx) on Windows. It runs as a separate app.

Defaults are conservative. `masterEnabled`, `renderEnabled` and `logicEnabled` turn whole groups off.

## Install

1. Install Fabric Loader 0.19.5+ and [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.8.
2. Optional: [ModMenu](https://modrinth.com/mod/modmenu) and [Cloth Config](https://modrinth.com/mod/cloth-config) for the settings screen.
3. Put the QuillCore jar from [Releases](https://github.com/ilikemacos/quillcore/releases) in `mods/`.

Settings live in `config/quillcore.json`.

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

## License

MIT, see [LICENSE](LICENSE).
