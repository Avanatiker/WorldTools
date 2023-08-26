package org.waste.of.time.serializer

import net.minecraft.SharedConstants
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.nbt.*
import net.minecraft.util.Util
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.LOGGER
import org.waste.of.time.WorldTools.credits
import org.waste.of.time.WorldTools.mc
import java.io.File

object LevelPropertySerializer {
    /**
     * See [net.minecraft.world.level.storage.LevelStorage.Session.backupLevelDataFile]
     */
    fun backupLevelDataFile(session: Session, levelName: String, player: ClientPlayerEntity) {
        val resultingFile = session.getDirectory(WorldSavePath.ROOT).toFile()
        val dataNbt = serialize(levelName, player)
        val levelNbt = NbtCompound()
        levelNbt.put("Data", dataNbt)
        levelNbt.copyFrom(credits)

        try {
            val newFile = File.createTempFile("level", WorldTools.DAT_EXTENSION, resultingFile)
            NbtIo.writeCompressed(levelNbt, newFile)
            val backup = session.getDirectory(WorldSavePath.LEVEL_DAT_OLD).toFile()
            val current = session.getDirectory(WorldSavePath.LEVEL_DAT).toFile()
            Util.backupAndReplace(current, newFile, backup)
            LOGGER.info("Saved level data.")
        } catch (exception: Exception) {
            LOGGER.error("Failed to save level {}", resultingFile, exception)
        }
    }

    /**
     * See [net.minecraft.world.level.LevelProperties.updateProperties]
     */
    private fun serialize(levelName: String, player: ClientPlayerEntity): NbtCompound {
        val nbt = NbtCompound()

        player.serverBrand?.let {
            val brandList = NbtList()
            brandList.add(NbtString.of(it))
            nbt.put("ServerBrands", brandList)
        }

        nbt.putBoolean("WasModded", true) // not sure

        // skip removed features

        val gameNbt = NbtCompound()
        gameNbt.putString("Name", SharedConstants.getGameVersion().name)
        gameNbt.putInt("Id", SharedConstants.getGameVersion().saveVersion.id)
        gameNbt.putBoolean("Snapshot", !SharedConstants.getGameVersion().isStable)
        gameNbt.putString("Series", SharedConstants.getGameVersion().saveVersion.series)

        nbt.put("Version", gameNbt)
        NbtHelper.putDataVersion(nbt)

        nbt.put("WorldGenSettings", generatorMockNbt())

        val playerEntry = mc.networkHandler?.listedPlayerListEntries?.find {
            it.profile.id == player.uuid
        }

        nbt.putInt(
            "GameType",
            playerEntry?.gameMode?.id
                ?: player.server?.defaultGameMode?.id
                ?: 0
        )
        nbt.putInt("SpawnX", player.world.levelProperties.spawnX)
        nbt.putInt("SpawnY", player.world.levelProperties.spawnY)
        nbt.putInt("SpawnZ", player.world.levelProperties.spawnZ)
        nbt.putFloat("SpawnAngle", player.world.levelProperties.spawnAngle)
        nbt.putLong("Time", player.world.time)
        nbt.putLong("DayTime", player.world.timeOfDay)
        nbt.putLong("LastPlayed", System.currentTimeMillis())
        nbt.putString("LevelName", levelName)
        nbt.putInt("version", 19133)
        nbt.putInt("clearWeatherTime", 0) // not sure
        nbt.putInt("rainTime", 0) // not sure
        nbt.putBoolean("raining", player.world.isRaining)
        nbt.putInt("thunderTime", 0) // not sure
        nbt.putBoolean("thundering", player.world.isThundering)
        nbt.putBoolean("hardcore", player.server?.isHardcore ?: false)
        nbt.putBoolean("allowCommands", true) // not sure
        nbt.putBoolean("initialized", true) // not sure
        player.world.worldBorder.write().writeNbt(nbt)
        nbt.putByte("Difficulty", player.world.levelProperties.difficulty.id.toByte())
        nbt.putBoolean("DifficultyLocked", false) // not sure
        nbt.put("GameRules", player.world.levelProperties.gameRules.toNbt())
        nbt.put("DragonFight", NbtCompound()) // not sure

        val playerNbt = NbtCompound()
        player.writeNbt(playerNbt)
        playerNbt.putString("Dimension", player.world.registryKey.value.path)
        nbt.put("Player", playerNbt)

        nbt.put("CustomBossEvents", NbtCompound()) // not sure

        nbt.put("ScheduledEvents", NbtList()) // not sure
        nbt.putInt("WanderingTraderSpawnDelay", 0) // not sure
        nbt.putInt("WanderingTraderSpawnChance", 0) // not sure

        // skip wandering trader id

        return nbt
    }

    private fun generatorMockNbt(): NbtCompound {
        val genNbt = NbtCompound()

        genNbt.putByte("bonus_chest", 0)
        genNbt.putLong("seed", 0)
        genNbt.putByte("generate_features", 0)

        val dimensionsNbt = NbtCompound()

        val overworld = NbtCompound()

        overworld.put("generator", voidGenerator())
        overworld.putString("type", "minecraft:overworld")

        dimensionsNbt.put("minecraft:overworld", overworld)

        val nether = NbtCompound()

        nether.put("generator", voidGenerator())
        nether.putString("type", "minecraft:the_nether")

        dimensionsNbt.put("minecraft:the_nether", nether)

        val end = NbtCompound()

        end.put("generator", voidGenerator())
        end.putString("type", "minecraft:the_end")

        dimensionsNbt.put("minecraft:the_end", end)

        genNbt.put("dimensions", dimensionsNbt)

        return genNbt
    }

    private fun voidGenerator(): NbtCompound {
        val voidGen = NbtCompound()
        val voidLayers = NbtList()
        val onlyLayer = NbtCompound()

        onlyLayer.putString("block", "minecraft:air")
        onlyLayer.putInt("height", 1)
        voidLayers.add(onlyLayer)

        val settingsNbt = NbtCompound()
        settingsNbt.putByte("features", 1)
        settingsNbt.putString("biome", "minecraft:the_void")
        settingsNbt.put("layers", voidLayers)
        settingsNbt.put("structure_overrides", NbtList())
        settingsNbt.putByte("lakes", 0)

        voidGen.put("settings", settingsNbt)
        voidGen.putString("type", "minecraft:flat")

        return voidGen
    }
}