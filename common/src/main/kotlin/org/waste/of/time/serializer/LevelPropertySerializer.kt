package org.waste.of.time.serializer

import net.minecraft.SharedConstants
import net.minecraft.nbt.*
import net.minecraft.util.Util
import net.minecraft.util.WorldSavePath
import net.minecraft.world.GameRules
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.addAuthor
import org.waste.of.time.WorldTools.mc
import java.io.File

object LevelPropertySerializer {
    /**
     * See [net.minecraft.world.level.storage.LevelStorage.Session.writeLevelDataFile]
     */
    fun Session.writeLevelDataFile(freezeWorld: Boolean = true) {
        val resultingFile = getDirectory(WorldSavePath.ROOT).toFile()
        val dataNbt = serializeLevelData(freezeWorld)
        val levelNbt = NbtCompound().apply {
            addAuthor()
            put("Data", dataNbt)
        }

        try {
            val newFile = File.createTempFile("level", WorldTools.DAT_EXTENSION, resultingFile)
            NbtIo.writeCompressed(levelNbt, newFile)
            val backup = getDirectory(WorldSavePath.LEVEL_DAT_OLD).toFile()
            val current = getDirectory(WorldSavePath.LEVEL_DAT).toFile()
            Util.backupAndReplace(current, newFile, backup)
            LOG.info("Saved level data.")
        } catch (exception: Exception) {
            LOG.error("Failed to save level {}", resultingFile, exception)
        }
    }

    /**
     * See [net.minecraft.world.level.LevelProperties.updateProperties]
     */
    private fun serializeLevelData(freezeWorld: Boolean) = NbtCompound().apply {
        val player = mc.player ?: return@apply

        player.serverBrand?.let {
            put("ServerBrands", NbtList().apply {
                add(NbtString.of(it))
            })
        }

        putBoolean("WasModded", false)

        // skip removed features

        put("Version", NbtCompound().apply {
            putString("Name", SharedConstants.getGameVersion().name)
            putInt("Id", SharedConstants.getGameVersion().saveVersion.id)
            putBoolean("Snapshot", !SharedConstants.getGameVersion().isStable)
            putString("Series", SharedConstants.getGameVersion().saveVersion.series)
        })

        NbtHelper.putDataVersion(this)

        put("WorldGenSettings", generatorMockNbt())

        mc.networkHandler?.listedPlayerListEntries?.find {
            it.profile.id == player.uuid
        }?.let {
            putInt("GameType", it.gameMode.id)
        } ?: putInt("GameType", player.server?.defaultGameMode?.id ?: 0)

        putInt("SpawnX", player.world.levelProperties.spawnX)
        putInt("SpawnY", player.world.levelProperties.spawnY)
        putInt("SpawnZ", player.world.levelProperties.spawnZ)
        putFloat("SpawnAngle", player.world.levelProperties.spawnAngle)
        putLong("Time", player.world.time)
        putLong("DayTime", player.world.timeOfDay)
        putLong("LastPlayed", System.currentTimeMillis())
        putString("LevelName", WorldTools.capturingWorldName!!)
        putInt("version", 19133)
        putInt("clearWeatherTime", 0) // not sure
        putInt("rainTime", 0) // not sure
        putBoolean("raining", player.world.isRaining)
        putInt("thunderTime", 0) // not sure
        putBoolean("thundering", player.world.isThundering)
        putBoolean("hardcore", player.server?.isHardcore ?: false)
        putBoolean("allowCommands", true) // not sure
        putBoolean("initialized", true) // not sure

        player.world.worldBorder.write().writeNbt(this)

        putByte("Difficulty", player.world.levelProperties.difficulty.id.toByte())
        putBoolean("DifficultyLocked", false) // not sure

        put("GameRules", genGameRules(player.world.gameRules, freezeWorld))

        put("DragonFight", NbtCompound()) // not sure

        put("Player", NbtCompound().apply {
            player.writeNbt(this)
            putString("Dimension", "minecraft:${player.world.registryKey.value.path}")
        })

        put("CustomBossEvents", NbtCompound()) // not sure
        put("ScheduledEvents", NbtList()) // not sure
        putInt("WanderingTraderSpawnDelay", 0) // not sure
        putInt("WanderingTraderSpawnChance", 0) // not sure

        // skip wandering trader id
    }

    private fun genGameRules(gameRules: GameRules, freezeWorld: Boolean) = gameRules.toNbt().apply {
        if (!freezeWorld) return@apply

        putString(GameRules.DO_WARDEN_SPAWNING.name, "false")
        putString(GameRules.DO_FIRE_TICK.name, "false")
        putString(GameRules.DO_VINES_SPREAD.name, "false")
        putString(GameRules.DO_MOB_SPAWNING.name, "false")
        putString(GameRules.DO_DAYLIGHT_CYCLE.name, "false")
        putString(GameRules.DO_TILE_DROPS.name, "false")
        putString(GameRules.DO_MOB_LOOT.name, "false")
        putString(GameRules.KEEP_INVENTORY.name, "true")
        putString(GameRules.DO_ENTITY_DROPS.name, "false")
        putString(GameRules.DO_MOB_GRIEFING.name, "false")
        putString(GameRules.DO_TRADER_SPAWNING.name, "false")
        putString(GameRules.DO_PATROL_SPAWNING.name, "false")
    }

    private fun generatorMockNbt() = NbtCompound().apply {
        putByte("bonus_chest", 0)
        putLong("seed", 0)
        putByte("generate_features", 0)

        put("dimensions", NbtCompound().apply {
            put("minecraft:${mc.player?.world?.registryKey?.value?.path}", NbtCompound().apply {
                put("generator", voidGenerator())
                putString("type", "minecraft:overworld")
            })

            put("minecraft:overworld", NbtCompound().apply {
                put("generator", voidGenerator())
                putString("type", "minecraft:overworld")
            })

            put("minecraft:the_nether", NbtCompound().apply {
                put("generator", voidGenerator())
                putString("type", "minecraft:the_nether")
            })

            put("minecraft:the_end", NbtCompound().apply {
                put("generator", voidGenerator())
                putString("type", "minecraft:the_end")
            })
        })
    }

    private fun voidGenerator() = NbtCompound().apply {
        put("settings", NbtCompound().apply {
            putByte("features", 1)
            putString("biome", "minecraft:the_void")
            put("layers", NbtList().apply {
                add(NbtCompound().apply {
                    putString("block", "minecraft:air")
                    putInt("height", 1)
                })
            })
            put("structure_overrides", NbtList())
            putByte("lakes", 0)
        })
        putString("type", "minecraft:flat")
    }
}
