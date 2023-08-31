# WorldTools: Minecraft World Downloader

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen)](https://www.minecraft.net/)
[![Fabric API](https://img.shields.io/badge/Fabric%20API-Required-orange)](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
[![fabric-language-kotlin](https://img.shields.io/badge/fabric--language--kotlin-Required-orange)](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin)

<p><img style="display: block; margin-left: auto; margin-right: auto;" src="src/main/resources/assets/world_tools/WorldTools.png" alt="" width="256" height="256" ></p>

WorldTools is a powerful Minecraft 1.20.1 Fabric Mod
that allows you to capture and save high-detail snapshots of server worlds locally.
It empowers you to download comprehensive information, including chunks, entities,
chests, players, statistics, advancements, and detailed metadata from a server's world.
WorldTools ensures that you can retain an accurate and unaltered representation of the server's world for analysis,
sharing, or backup purposes on your local machine.
<p><a title="Fabric Language Kotlin" href="https://minecraft.curseforge.com/projects/fabric-language-kotlin" target="_blank" rel="noopener noreferrer"><img style="display: block; margin-left: auto; margin-right: auto;" src="https://i.imgur.com/c1DH9VL.png" alt="" width="171" height="50" /></a></p>

## Features

- **Comprehensive Data Capture**: WorldTools enables you to capture various critical aspects of a server's world, preventing any detail from being overlooked.
    - Chunks: Terrain and structures present in the server's world.
    - Entities: Positions and attributes of all entities.
    - Containers: Contents of chests, hopper etc.
    - Players: Player positions and inventories.
    - Statistics: Full gameplay statistics details.
    - Advancements: Player advancements and progress.
    - Detailed Metadata: Preserve essential capture metadata.

- **Capture Mode**: Enable capture mode to continuously cache loaded chunks and entities from the server. The data is periodically saved to your local disk, ensuring you don't lose any valuable information.

- **Configurable Options**:
    - Access the menu by pressing F12 (changeable in the controls screen), providing numerous options (work in progress) to customize WorldTools according to your requirements.

- **Freeze Entities**: This option freezes all entities, preventing them from moving upon login.

- **Freeze World**: Stop new chunks from generating using a custom world generator, freeze time and weather, and set other gamerules to ensure a non-altered snapshot of the world.

- **Easy Access to Saved Worlds**: Your locally captured world save can be found in the single-player worlds list, allowing you to load and explore it conveniently.

## Getting Started

1. **Installation**:
    - Install Fabric by following the [Fabric Installation Guide](https://fabricmc.net/wiki/install).
    - Download the latest version of WorldTools from the [releases page](https://github.com/Avanatiker/WorldTools/releases)
    - Place the WorldTools mod JAR file in the "mods" folder of your Fabric installation.

2. **Prerequisites**: Make sure you have the following mods installed:
    - [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
    - [fabric-language-kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin)

3. **Usage**:
    - Enable capture mode: Use `/worldtools capture start` to start capturing data.
    - Play the game normally while WorldTools caches the necessary data.
    - Save captured data: Trigger `/worldtools save` to save the cached data to disk.
    - Disable capture mode: Use `/worldtools capture stop` to stop capturing data.

4. **Access Configuration Menu**: Press F12 to access the configuration menu and explore available options (work in progress).

## File Structure

After capturing data, WorldTools creates the following files in the world directory's folder:

- `Capture Metadata.md`: Contains detailed information about the capture process itself.

- `Dimension Tree.txt`: Provides a tree of all dimension folder paths of the server, not just the downloaded ones.

- `Player Entry List.csv`: Lists all players that were online during the capture including all known metadata.

## License

WorldTools is licensed under the [GNU General Public License v3.0](LICENSE.md).

---

**Note**: WorldTools is a powerful tool for locally capturing and preserving an unaltered snapshot of server worlds. Please use it responsibly and respect the terms of the GNU GPL v3 license.

If you have any questions, concerns, or suggestions, you can reach us at support@worldtools.com or visit our [official Discord server](https://discord.gg/worldtools).

**Disclaimer:** WorldTools is not affiliated with Mojang Studios. Minecraft is a registered trademark of Mojang Studios. Use of the WorldTools software is subject to the terms outlined in the license agreement.
