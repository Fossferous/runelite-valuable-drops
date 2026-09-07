# Valuable Drops Party

A [RuneLite](https://runelite.net) plugin that broadcasts valuable drops to everyone in your RuneLite party.
When a party member receives a drop worth at least a configurable threshold, the other party members
running this plugin see a chat message, and the drop is added to everyone's session history panel in the
sidebar, including your own.

## Features

- **Party broadcasts:** notifies your party in game chat when someone receives a valuable drop.
- **Session history panel:** a sidebar panel listing the valuable drops the party has received this session,
  with item icons, values and who got them.
- **Configurable threshold:** minimum GP value to broadcast (default 1,000,000 GP). The higher of the Grand
  Exchange value and the High Alchemy value is used.
- **Untradeable overrides:** optionally broadcast notable untradeable drops that never reach the threshold,
  such as champion scrolls, mutagens and raid pets. Tradeable items such as boss jars, ornament kits and
  raid weapons are broadcast when their value reaches the threshold.
- **Custom item IDs:** a comma-separated list of item IDs to always broadcast, regardless of value.

Drops are taken from RuneLite's built-in **Loot Tracker** plugin, so anything the Loot Tracker records is
covered: NPC kills, PvP kills, raids, Barrows, clue caskets, chests and so on. The Loot Tracker plugin must
be enabled; if it is off, this plugin tells you so in chat when you log in. Pets that start following you
after a kill are not reported as loot by RuneLite, so only pets handed out through a reward chest
(for example raid pets) can be broadcast.

Only party members who also have this plugin installed will receive the broadcasts.

## Requirements

- A Java Development Kit, version 11, 17 or 21. [Eclipse Temurin](https://adoptium.net/temurin/releases/)
  is recommended; pick one of those versions rather than the newest release, because the Gradle and Lombok
  versions pinned by this project do not run on JDK 22 or newer.
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

Once the client is up:

1. Enable **Valuable Drops Party** in the plugin configuration sidebar. Make sure **Loot Tracker** is enabled too.
2. Enable the built-in **Party** plugin (it is off by default), open its sidebar panel and click **Create party**,
   or **Join party** and enter the party's passphrase.
3. Get a drop worth at least your threshold and it will be broadcast to the party.

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
| Broadcast Untradeables | On | Also broadcast notable untradeable drops that never reach the minimum value |
| Custom Item IDs | empty | Comma-separated item IDs to always broadcast |

## License

BSD 2-Clause. See [LICENSE](LICENSE).
