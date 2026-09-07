# Valuable Drops Party

A [RuneLite](https://runelite.net) plugin that broadcasts valuable drops to everyone in your RuneLite party.
When you or a party member receives a drop worth more than a configurable threshold, every party member
running this plugin sees a chat message, and the drop is added to a session history panel in the sidebar.

## Features

- **Party broadcasts:** notifies your party in game chat when someone receives a valuable drop.
- **Session history panel:** a sidebar panel listing the valuable drops the party has received this session,
  with item icons, values and who got them.
- **Configurable threshold:** minimum GP value to broadcast (default 1,000,000 GP). The higher of the Grand
  Exchange value and the High Alchemy value is used.
- **Untradeable overrides:** optionally broadcast valuable 0 GP untradeables such as champion scrolls,
  mutagens, jars, raid uniques and ornament kits.
- **Custom item IDs:** a comma-separated list of item IDs to always broadcast, regardless of value.

Drops are detected from NPC kills, PvP kills and Loot Tracker events (raids, Barrows, clue caskets, chests
and so on). Only party members who also have this plugin installed will receive the broadcasts.

## Requirements

- Java Development Kit 11 or newer. [Eclipse Temurin](https://adoptium.net/temurin/releases/) is recommended.
- An internet connection: the build downloads the RuneLite client from `repo.runelite.net`.

## Running the plugin in a development client

The easiest way to try the plugin is the Gradle `run` task, which starts a RuneLite developer client with
this plugin already loaded:

```bash
# macOS / Linux
./gradlew run

# Windows
gradlew.bat run
```

In IntelliJ IDEA you can also open `build.gradle` and run the `run` task from the Gradle tool window.
If you log in with a Jagex account, follow the
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) guide first.

Once the client is up, enable **Valuable Drops Party** in the plugin configuration sidebar and join a party
(Party plugin, or `::party` in chat). Get a drop worth more than your threshold and it will be broadcast.

## Building a jar

```bash
./gradlew build
```

The plugin jar is written to `build/libs/valuable-drops-party.jar`.

Note that RuneLite only sideloads jars when it is started in developer mode, which is not available through
the normal RuneLite launcher. If you run the client from source with `--developer-mode`, place the jar in
`~/.runelite/sideloaded-plugins/` and it will be picked up on the next start. For everyday use the plugin
must be installed from the Plugin Hub.

## Publishing to the Plugin Hub

The repository follows the [Plugin Hub](https://github.com/runelite/plugin-hub) layout
(`runelite-plugin.properties`, BSD 2-Clause `LICENSE`, `build=standard`). To publish, fork the plugin-hub
repository and add a `plugins/valuable-drops-party` file containing this repository's URL and the commit hash
to publish, then open a pull request there.

## Configuration

| Setting | Default | Description |
| --- | --- | --- |
| Minimum Value | 1,000,000 | Minimum GP value of a drop to broadcast |
| Broadcast 0-Value Drops | On | Broadcast valuable untradeables that have no GP value |
| Custom Item IDs | empty | Comma-separated item IDs to always broadcast |

## License

BSD 2-Clause. See [LICENSE](LICENSE).
