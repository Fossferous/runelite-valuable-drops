# Valuable Drops Party - RuneLite Plugin

This plugin broadcasts valuable drops to players connected via RuneLite's native `PartyService`. It hooks into `LootReceived` events, calculates item values via the `ItemManager`, and transmits a custom `PartyMemberMessage` payload over the WebSocket.

## Features

- **Party Broadcasts:** Automatically notifies your party members in game chat when you receive a valuable drop.
- **Configurable Thresholds:** Set a minimum GP threshold for broadcast (default: 1,000,000 GP). Uses both GE value and High Alch value to determine total drop value.
- **Zero-Value Untradeable Overrides:** Contains a custom toggle to automatically broadcast untradeable but highly sought-after 0-GP items such as:
  - Pets
  - Champion Scrolls
  - Mutagens
  - Raid Uniques and Ornament Kits
- **Custom Item IDs:** Allows you to input a comma-separated list of item IDs to always broadcast, regardless of their GP value.

## How to Sideload and Test

To test this plugin locally on your developer RuneLite client, follow these steps:

1. **Build the Plugin:**
   In the root of this project directory (`runelite-valuable-drops-plugin`), run the Gradle build command.
   ```bash
   ./gradlew build
   ```
   *This has already been completed, and the compiled `.jar` is located in `build/libs/`.*

2. **Locate your local RuneLite Plugin Directory:**
   By default, RuneLite allows you to sideload external plugins from its configuration directory.
   - **Windows:** `%userprofile%\.runelite\plugins\`
   - **macOS:** `$HOME/.runelite/plugins/`
   - **Linux:** `$HOME/.runelite/plugins/`

3. **Copy the `.jar` File:**
   Copy the `.jar` file from `build/libs/valuable-drops-party-plugin-1.0-SNAPSHOT.jar` into the `.runelite/plugins/` directory. Create the `plugins` folder if it doesn't already exist.

4. **Launch RuneLite (Developer Mode):**
   Run your RuneLite client with developer mode enabled (using the `--developer-mode` flag or running it via your IDE). 

5. **Enable the Plugin:**
   Open the RuneLite configuration sidebar, search for **"Valuable Drops Party"**, and turn it on. Configure your minimum thresholds and custom item IDs.

6. **Test in a Party:**
   Join a RuneLite party with your friends. When you receive a valuable drop (or when they do), you will see a color-coded broadcast in your game chat!
