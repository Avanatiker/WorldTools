package org.waste.of.time.storage.serializable

import net.minecraft.SharedConstants
import org.waste.of.time.config.WorldToolsConfig.World.WorldGenerator.GeneratorType
import net.minecraft.nbt.*
import net.minecraft.text.MutableText
import net.minecraft.util.Util
import net.minecraft.util.WorldSavePath
import net.minecraft.world.GameRules
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.Utils.addAuthor
import org.waste.of.time.Utils.toByte
import org.waste.of.time.WorldTools.DAT_EXTENSION
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.Storeable
import java.io.File
import java.io.IOException

class LevelDataStoreable : Storeable {
    override fun shouldStore() = config.general.capture.levelData

    override val verboseInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.levelData",
            currentLevelName,
            "level${DAT_EXTENSION}"
        )

    override val anonymizedInfo: MutableText
        get() = verboseInfo

    /**
     * See [net.minecraft.world.level.storage.LevelStorage.Session.backupLevelDataFile]
     */
    override fun store(
        session: Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        val resultingFile = session.getDirectory(WorldSavePath.ROOT).toFile()
        val dataNbt = serializeLevelData()
        // if we save an empty level.dat, clients will crash when opening the SP worlds screen
        if (dataNbt.isEmpty) throw RuntimeException("Failed to serialize level data")
        val levelNbt = NbtCompound().apply {
            addAuthor()
            put("Data", dataNbt)
        }

        try {
            val newFile = File.createTempFile("level", DAT_EXTENSION, resultingFile)
            NbtIo.writeCompressed(levelNbt, newFile)
            val backup = session.getDirectory(WorldSavePath.LEVEL_DAT_OLD).toFile()
            val current = session.getDirectory(WorldSavePath.LEVEL_DAT).toFile()
            Util.backupAndReplace(current, newFile, backup)
            LOG.info("Saved level data.")
        } catch (exception: IOException) {
            MessageManager.sendError(
                "worldtools.log.error.failed_to_save_level",
                resultingFile.path,
                exception.localizedMessage
            )
        }
    }

    /**
     * See [net.minecraft.world.level.LevelProperties.updateProperties]
     */
    private fun serializeLevelData() = NbtCompound().apply {
        val player = mc.player
        if (player == null) {
            // TODO: store a reference to the player when we start the capture so this isn't an issue
            //  can occur when the user disconnects while a capture is ongoing
            LOG.error("Player is null when trying to serialize level data")
            LOG.error("Level data will be incomplete")
        }

        player?.serverBrand?.let {
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
        player?.let { player ->
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
        }
        putLong("LastPlayed", System.currentTimeMillis())
        putString("LevelName", currentLevelName)
        putInt("version", 19133)
        putInt("clearWeatherTime", 0) // not sure
        putInt("rainTime", 0) // not sure
        player?.let { player ->
            putBoolean("raining", player.world.isRaining)
            putBoolean("thundering", player.world.isThundering)
            putBoolean("hardcore", player.server?.isHardcore ?: false)
        }
        putInt("thunderTime", 0) // not sure
        putBoolean("allowCommands", true) // not sure
        putBoolean("initialized", true) // not sure

        player?.let { player ->
            player.world.worldBorder.write().writeNbt(this)

            putByte("Difficulty", player.world.levelProperties.difficulty.id.toByte())
            putBoolean("DifficultyLocked", false) // not sure

            put("GameRules", genGameRules(player.world.gameRules))
            put("Player", NbtCompound().apply {
                player.writeNbt(this)
                remove("LastDeathLocation") // can contain sensitive information
                putString("Dimension", "minecraft:${player.world.registryKey.value.path}")
            })
        }

        put("DragonFight", NbtCompound()) // not sure
        put("CustomBossEvents", NbtCompound()) // not sure
        put("ScheduledEvents", NbtList()) // not sure
        putInt("WanderingTraderSpawnDelay", 0) // not sure
        putInt("WanderingTraderSpawnChance", 0) // not sure

        // skip wandering trader id
    }

    private fun genGameRules(gameRules: GameRules) = gameRules.toNbt().apply {
        val setting = config.world.gameRules
        if (!setting.modifyGameRules) return@apply

        putString(GameRules.DO_WARDEN_SPAWNING.name, setting.doWardenSpawning.toString())
        putString(GameRules.DO_FIRE_TICK.name, setting.doFireTick.toString())
        putString(GameRules.DO_VINES_SPREAD.name, setting.doVinesSpread.toString())
        putString(GameRules.DO_MOB_SPAWNING.name, setting.doMobSpawning.toString())
        putString(GameRules.DO_DAYLIGHT_CYCLE.name, setting.doDaylightCycle.toString())
        putString(GameRules.KEEP_INVENTORY.name, setting.keepInventory.toString())
        putString(GameRules.DO_MOB_GRIEFING.name, setting.doMobGriefing.toString())
        putString(GameRules.DO_TRADER_SPAWNING.name, setting.doTraderSpawning.toString())
        putString(GameRules.DO_PATROL_SPAWNING.name, setting.doPatrolSpawning.toString())
        putString(GameRules.DO_WEATHER_CYCLE.name, setting.doWeatherCycle.toString())
    }

    private fun generatorMockNbt() = NbtCompound().apply {
        putByte("bonus_chest", config.world.worldGenerator.bonusChest.toByte())
        putLong("seed", config.world.worldGenerator.seed)
        putByte("generate_features", config.world.worldGenerator.generateFeatures.toByte())

        put("dimensions", NbtCompound().apply {
            mc.networkHandler?.worldKeys?.forEach { key ->
                put("minecraft:${key.value.path}", NbtCompound().apply {
                    put("generator", generateGenerator(key.value.path))

                    when (key.value.path) {
                        "the_nether" -> {
                            putString("type", "minecraft:the_nether")
                        }
                        "the_end" -> {
                            putString("type", "minecraft:the_end")
                        }
                        else -> {
                            putString("type", "minecraft:overworld")
                        }
                    }
                })
            }
        })
    }

    private fun generateGenerator(path: String) = NbtCompound().apply {
        when (config.world.worldGenerator.type) {
            GeneratorType.VOID -> voidGenerator()
            GeneratorType.DEFAULT -> defaultGenerator(path)
            GeneratorType.FLAT -> flatGenerator(path)
        }
    }

    private fun NbtCompound.voidGenerator() {
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

    private fun NbtCompound.defaultGenerator(path: String) {
        when (path) {
            "the_nether" -> {
                put("biome_source", NbtCompound().apply {
                    putString("preset", "minecraft:nether")
                    putString("type", "minecraft:multi_noise")
                })
                putString("settings", "minecraft:nether")
                putString("type", "minecraft:noise")
            }
            "the_end" -> {
                put("biome_source", NbtCompound().apply {
                    putString("type", "minecraft:end")
                })
                putString("settings", "minecraft:end")
                putString("type", "minecraft:the_end")
            }
            else -> {
                put("biome_source", NbtCompound().apply {
                    putString("preset", "minecraft:overworld")
                    putString("type", "minecraft:multi_noise")
                })
                putString("settings", "minecraft:overworld")
                putString("type", "minecraft:noise")
            }
        }
    }

    private fun NbtCompound.flatGenerator(path: String) {
        // ToDo: Implement flat generator
    }
}
