# QuillCore

Fabric performance mod for Minecraft Java 1.21.8. It throttles server-side work for mobs and items far from players and trims client rendering that you can't see anyway.

It is written from scratch. Projects like Sodium and Lithium were an inspiration, but no code is taken from them.

## Features

Server (Quill Logic):

- Idle AI throttle: far idle mobs skip a few goal selector ticks. Sensing and navigation still run.

Defaults are conservative. `masterEnabled`, `renderEnabled` and `logicEnabled` turn whole groups off.

## Install

1. Install Fabric Loader 0.19.5+ and [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.8.
2. Put the QuillCore jar from [Releases](https://github.com/ilikemacos/quillcore/releases) in `mods/`.

Settings live in `config/quillcore.json`.

## Commands

Permission level 2.

| Command | |
|---|---|
| `/quillcore status` | Current settings |
| `/quillcore reload` | Reload the config file |
| `/quillcore set <key> <value>` | Change a setting |
| `/quillcore bench start\|stop\|report` | Sample tick and frame times |

## Building

Gradle needs JDK 25. The mod targets Java 21.

```
./gradlew build
```

## License

MIT, see [LICENSE](LICENSE).
