package org.waste.of.time

import net.minecraft.SharedConstants
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.nbt.*
import net.minecraft.util.Util
import net.minecraft.util.WorldSavePath
import net.minecraft.world.World
import net.minecraft.world.gen.GeneratorOptions
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.WorldTools.LOGGER
import java.io.File

object LevelPropertySerializer {
    fun backupLevelDataFile(session: Session, levelName: String, player: ClientPlayerEntity, world: World) {
        val resultingFile = session.getDirectory(WorldSavePath.ROOT).toFile()
        val dataNbt = serialize(levelName, player, world)
        val levelNbt = NbtCompound()
        levelNbt.put("Data", dataNbt)

        try {
            val newFile = File.createTempFile("level", ".dat", resultingFile)
            NbtIo.writeCompressed(levelNbt, newFile)
            val backup = session.getDirectory(WorldSavePath.LEVEL_DAT_OLD).toFile()
            val current = session.getDirectory(WorldSavePath.LEVEL_DAT).toFile()
            Util.backupAndReplace(current, newFile, backup)
        } catch (exception: Exception) {
            LOGGER.error("Failed to save level {}", resultingFile, exception)
        }
    }

    private fun serialize(levelName: String, player: ClientPlayerEntity, world: World): NbtCompound {
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

        // ToDo: maybe find a way to use this instead of the above, making a valid registry is essential
//        WorldGenSettings.CODEC.encodeStart(
//            NbtOps.INSTANCE, WorldGenSettings(genOptions, DimensionOptionsRegistryHolder.DimensionsConfig())
//        ).resultOrPartial {
//            LOGGER.error(it)
//        }.ifPresent {
//            nbt.put("WorldGenSettings", it)
//        }

        nbt.putInt("GameType", player.server?.defaultGameMode?.id ?: 0)
        nbt.putInt("SpawnX", world.levelProperties.spawnX)
        nbt.putInt("SpawnY", world.levelProperties.spawnY)
        nbt.putInt("SpawnZ", world.levelProperties.spawnZ)
        nbt.putFloat("SpawnAngle", world.levelProperties.spawnAngle)
        nbt.putLong("Time", world.time)
        nbt.putLong("DayTime", world.timeOfDay)
        nbt.putLong("LastPlayed", System.currentTimeMillis())
        nbt.putString("LevelName", levelName)
        nbt.putInt("version", 19133)
        nbt.putInt("clearWeatherTime", 0) // not sure
        nbt.putInt("rainTime", 0) // not sure
        nbt.putBoolean("raining", world.isRaining)
        nbt.putInt("thunderTime", 0) // not sure
        nbt.putBoolean("thundering", world.isThundering)
        nbt.putBoolean("hardcore", player.server?.isHardcore ?: false)
        nbt.putBoolean("allowCommands", true) // not sure
        nbt.putBoolean("initialized", true) // not sure
        world.worldBorder.write().writeNbt(nbt)
        nbt.putByte("Difficulty", world.levelProperties.difficulty.id.toByte())
        nbt.putBoolean("DifficultyLocked", false) // not sure
        nbt.put("GameRules", world.levelProperties.gameRules.toNbt())
        nbt.put("DragonFight", NbtCompound()) // not sure

        val playerNbt = NbtCompound()
        nbt.put("Player", player.writeNbt(playerNbt)) // ???

        // skip custom boss events
        nbt.put("CustomBossEvents", NbtCompound())

        nbt.put("ScheduledEvents", NbtList()) // not sure
        nbt.putInt("WanderingTraderSpawnDelay", 0) // not sure
        nbt.putInt("WanderingTraderSpawnChance", 0) // not sure

        // skip wandering trader id

        return nbt
    }

    private fun generatorMockNbt(): NbtCompound {
        val genOptions = GeneratorOptions.createRandom()
        val genNbt = NbtCompound()

        genNbt.putByte("bonus_chest", 0)
        genNbt.putLong("seed", genOptions.seed)
        genNbt.putByte("generate_features", 1)

        val dimensionsNbt = NbtCompound()

        val overworld = NbtCompound()
        val overworldGen = NbtCompound()
        val overworldBiomeSource = NbtCompound()

        overworldBiomeSource.putString("preset", "minecraft:overworld")
        overworldBiomeSource.putString("type", "minecraft:multi_noise")

        overworldGen.putString("settings", "minecraft:overworld")
        overworldGen.put("biome_source", overworldBiomeSource)
        overworldGen.putString("type", "minecraft:noise")

        overworld.put("generator", overworldGen)
        overworld.putString("type", "minecraft:overworld")

        dimensionsNbt.put("minecraft:overworld", overworld)

        val nether = NbtCompound()
        val netherGen = NbtCompound()
        val netherBiomeSource = NbtCompound()

        netherBiomeSource.putString("preset", "minecraft:nether")
        netherBiomeSource.putString("type", "minecraft:multi_noise")

        netherGen.putString("settings", "minecraft:nether")
        netherGen.put("biome_source", netherBiomeSource)
        netherGen.putString("type", "minecraft:noise")

        nether.put("generator", netherGen)
        nether.putString("type", "minecraft:the_nether")

        dimensionsNbt.put("minecraft:the_nether", nether)

        val end = NbtCompound()
        val endGen = NbtCompound()
        val endBiomeSource = NbtCompound()

        endBiomeSource.putString("type", "minecraft:the_end")

        endGen.putString("settings", "minecraft:end")
        endGen.put("biome_source", endBiomeSource)
        endGen.putString("type", "minecraft:noise")

        end.put("generator", endGen)
        end.putString("type", "minecraft:the_end")

        dimensionsNbt.put("minecraft:the_end", end)

        genNbt.put("dimensions", dimensionsNbt)

        return genNbt
    }
}