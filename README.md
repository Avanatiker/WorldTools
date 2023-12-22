<p align="center">
  <img src="https://github.com/Avanatiker/WorldTools/blob/master/common/src/main/resources/assets/worldtools/WorldTools.png?raw=true" alt="" width="256" height="256" style="display: block; margin-left: auto; margin-right: auto;">
</p>

# WorldTools: World Downloader (Fabric / Forge)

[![CurseForge Downloads](https://cf.way2muchnoise.eu/worldtools.svg?badge_style=for_the_badge)](https://www.curseforge.com/minecraft/mc-mods/worldtools)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/FlFKBOIX?style=for-the-badge&logo=modrinth&label=Modrinth&color=00AF5C)](https://modrinth.com/mod/worldtools)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-lime?style=for-the-badge&link=https://www.minecraft.net/)](https://www.minecraft.net/)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue?style=for-the-badge&link=https://www.gnu.org/licenses/gpl-3.0.en.html)](https://www.gnu.org/licenses/gpl-3.0.en.html)

WorldTools is a powerful Minecraft mod that allows you to capture and save high-detail snapshots of server worlds
locally.
It empowers you to download comprehensive information, including chunks, entities,
chests, players, statistics, advancements, and detailed metadata.
WorldTools ensures that you can retain an accurate and unaltered representation of the server's world for analysis,
sharing, or backup purposes on your local machine.

<p align="center">
  <a href="https://fabricmc.net/wiki/install"><img src="https://cdn.jonasjones.dev/mod-badges/support-fabric.png" width="150px" alt="Fabric Supported"></a>
  <a href="https://files.minecraftforge.net/net/minecraftforge/forge/"><img src="https://cdn.jonasjones.dev/mod-badges/support-forge.png" width="150px" alt="Forge Supported"></a>
</p>

<div align="center">
  <a href="https://discord.gg/3y3ah5BtjB"><img src="https://invidget.switchblade.xyz/3y3ah5BtjB" alt="Link to the lambda discord server https://discord.gg/3y3ah5BtjB"></a>
</div>

## Features

- **World Download (_default keybind:_ `F12`)**:
  Initiate a quick download by hitting the `F12` key, which can be altered in the keybind settings.
  Alternatively, you can access the GUI (_default keybind:_ `F10`) via the escape menu.
  The GUI allows you to tailor the capture process according to your requirements.
  WorldTools facilitates the capture of a wide range of crucial elements, ensuring no detail is missed.
    - Chunks: Terrain, biomes and structures
    - Entities: Positions and attributes of all entities
    - Containers: Contents of all tile entities like chests, shulkers, hoppers, furnaces, brewing stands, droppers, dispensers etc...
    - Players: Player positions and inventories
    - Statistics: Full personal player statistics
    - Advancements: Player advancements and progress
    - Detailed Metadata: Exhaustive capture details like modt, server version, and more

- **Easy Access to Saved Worlds**: Your locally captured world save can be found in the single-player worlds list,
  allowing you to load and explore it conveniently.

- **Advanced Configuration**: WorldTools provides a wide range of settings to customize the capture process to your needs.
  Select elements to capture, modify game rules, alter entity NBT data, and configure the capture process in detail.

## Getting Started

### Fabric

<p>
  <a title="Fabric API" href="https://www.curseforge.com/minecraft/mc-mods/fabric-api" target="_blank" rel="noopener noreferrer">
    <img src="https://i.imgur.com/Ol1Tcf8.png" alt="" height="50" />
  </a>
  <a title="Fabric Language Kotlin" href="https://minecraft.curseforge.com/projects/fabric-language-kotlin" target="_blank" rel="noopener noreferrer">
    <img src="https://i.imgur.com/c1DH9VL.png" alt="" height="50"/>
  </a>
</p>

1. **Installation**:
    - Install Fabric by following the [Fabric Installation Guide](https://fabricmc.net/wiki/install).
    - Download the latest Fabric version of WorldTools from
      the [releases page](https://github.com/Avanatiker/WorldTools/releases)
    - Place the WorldTools Fabric mod JAR file in the "mods" folder of your Fabric installation.

2. **Prerequisites**: Make sure you have the following mods installed:
    - [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
    - [fabric-language-kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin)

### Forge

1. **Installation**:
    - Install Forge by following the [Forge Download Link](https://files.minecraftforge.net/net/minecraftforge/forge/).
    - Download the latest Forge version of WorldTools from
      the [releases page](https://github.com/Avanatiker/WorldTools/releases)
    - Place the WorldTools Forge mod JAR file in the "mods" folder of your Forge installation.

2. **Prerequisites**: Make sure you have the following mods installed:
    - [Kotlin For Forge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge)

### Usage

1. **Download**:
    - Enable capture mode: Hit `F12` the GUI or `/worldtools capture` to start capturing data. 
    - Play the game normally while WorldTools downloads the all data. You need to open containers like chests to capture their contents.
    - Save captured data: Hit `F12` the GUI or `/worldtools capture` again to stop capturing data and save the world.
2. **Access Downloaded World**: Your downloaded world can be found in the single-player worlds list.

### File Structure

After capturing data, WorldTools creates the following files in the world directory's folder:

- `Capture Metadata.md`: Contains detailed information about the capture process itself.

- `Dimension Tree.txt`: Provides a tree of all dimension folder paths of the server, not just the downloaded ones.

- `Player Entry List.csv`: Lists all players that were online during the capture including all known metadata.

## Contributing

Contributions are welcome!
Please read our [Code of Conduct](https://github.com/Avanatiker/WorldTools/blob/master/CODE_OF_CONDUCT.md)
and [Contributing Guidelines](https://github.com/Avanatiker/WorldTools/blob/master/CONTRIBUTING.md) before submitting a
Pull Request.

1. Fork the repository and clone it to your local machine.  
   `git clone https://github.com/Avanatiker/WorldTools`
2. Create a new branch for your feature.  
   `git checkout -b my-new-feature`
3. Make your changes and commit them to your branch.  
   `git commit -am 'Add some feature'`
4. Push your changes to your fork.  
   `git push origin my-new-feature`
5. Open a Pull Request in this repository.
6. Your Pull Request will be reviewed and merged as soon as possible.
7. Wait for the next release to see your changes in action!

## Building

1. Once forked and cloned, run `./gradlew build` to build the mod for both mod loaders.
2. IntelliJ IDEA will generate run configurations for both mod loaders that can be used to run the mod in a test
   environment.
3. The Fabric mod JAR file can be found in `fabric/build/libs` and the Forge mod JAR file in `forge/build/libs`.

## ToDo

- [ ] Capture Mode: Choose between two capture modes: Full and Incremental.
  The Full mode captures all data from the server, while the Incremental mode only captures data that has changed
  since the last capture.
- [ ] Save ender chest contents
- [ ] Save lectern contents
- [ ] Anonymous censored mode
- [ ] Fix chests being removed from the chunk and then the data is lost until the chunk is serialized
- [ ] On capture switch config button functionality
- [ ] Save more entity data (NBT) like trades, inventory from boats and minecarts, etc.
- [ ] Live statistics: Data usage, time elapsed, etc.
- [ ] Support for different mc versions and ViaVersion
- [ ] Different world generators and seed setting
- [ ] Automatic World Downloading (Disabled by default) - decides whether or not the mod automatically starts downloading worlds upon join
- [ ] Download Distance (locked to client's chunkloading distance by default) - decides the chunk radius around the player in which world data is downloaded, has 3 options: Custom, Client View Distance, and Server View Distance.
- [ ] Dimension, XP, selected item slot, player game type, is not saved to player nbt in level.dat

## License

WorldTools is distributed under
the [GNU General Public License v3.0](https://github.com/Avanatiker/WorldTools/blob/master/LICENSE.md).

---

If you have any questions, concerns, or suggestions,
you can visit our [official Discord server](https://discord.gg/3y3ah5BtjB).

**Disclaimer:** WorldTools is not affiliated with Mojang Studios. Minecraft is a registered trademark of Mojang Studios.
Use of the WorldTools software is subject to the terms outlined in the license agreement.
