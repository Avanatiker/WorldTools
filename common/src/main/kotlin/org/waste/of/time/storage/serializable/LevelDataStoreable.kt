package org.waste.of.time.storage.serializable

import net.minecraft.SharedConstants
import net.minecraft.nbt.*
import net.minecraft.text.MutableText
import net.minecraft.util.Util
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.Utils.toByte
import org.waste.of.time.WorldTools.DAT_EXTENSION
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.config.WorldToolsConfig.World.WorldGenerator.GeneratorType
import org.waste.of.time.manager.CaptureManager
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.Storeable
import java.io.File
import java.io.IOException

class LevelDataStoreable : Storeable() {
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
            put("Data", dataNbt)
        }

        try {
            val newFile = File.createTempFile("level", DAT_EXTENSION, resultingFile).toPath()
            NbtIo.writeCompressed(levelNbt, newFile)
            val backup = session.getDirectory(WorldSavePath.LEVEL_DAT_OLD)
            val current = session.getDirectory(WorldSavePath.LEVEL_DAT)
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
        val player = CaptureManager.lastPlayer ?: mc.player ?: return@apply

        mc.networkHandler?.brand?.let {
            put("ServerBrands", NbtList().apply {
                add(NbtString.of(it))
            })
        }

        putBoolean("WasModded", false)

        // skip removed features

        // Omit detailed Version block; DataVersion is written below

        NbtHelper.putDataVersion(this)

        put("WorldGenSettings", generatorMockNbt())
        // Omit GameType for compatibility; client will infer

        putInt("SpawnX", player.blockPos.x)
        putInt("SpawnY", player.blockPos.y)
        putInt("SpawnZ", player.blockPos.z)
        putFloat("SpawnAngle", player.yaw)
        putLong("Time", player.getEntityWorld().time)
        putLong("DayTime", player.getEntityWorld().timeOfDay)
        putLong("LastPlayed", System.currentTimeMillis())
        putString("LevelName", currentLevelName)
        putInt("version", 19133)
        putInt("clearWeatherTime", 0) // not sure
        putInt("rainTime", 0) // not sure
        putBoolean("raining", player.getEntityWorld().isRaining)
        putBoolean("thundering", player.getEntityWorld().isThundering)
        putBoolean("hardcore", mc.server?.isHardcore ?: false)
        putInt("thunderTime", 0) // not sure
        putBoolean("allowCommands", true) // not sure
        putBoolean("initialized", true) // not sure

        // WorldBorder serialization changed in 1.21.11. The world remains loadable without
        // copying this optional block, so keep level.dat generation compatible here.

        putByte("Difficulty", player.getEntityWorld().levelProperties.difficulty.id.toByte())
        putBoolean("DifficultyLocked", false) // not sure

        // GameRules are written as the same string map level.dat expects. 1.21.11 moved
        // the API and removed the old toNbt() helper.
        put("GameRules", generateGameRulesNbt())

        // Minimal Player tag to spawn near captured area
        put("Player", NbtCompound().apply {
            put("Pos", NbtList().apply {
                add(NbtDouble.of(player.x))
                add(NbtDouble.of(player.y))
                add(NbtDouble.of(player.z))
            })
            put("Rotation", NbtList().apply {
                add(NbtFloat.of(player.yaw))
                add(NbtFloat.of(player.pitch))
            })
            remove("LastDeathLocation")
            putString("Dimension", "minecraft:${player.getEntityWorld().registryKey.value.path}")
        })

        put("DragonFight", NbtCompound()) // not sure
        put("CustomBossEvents", NbtCompound()) // not sure
        put("ScheduledEvents", NbtList()) // not sure
        putInt("WanderingTraderSpawnDelay", 0) // not sure
        putInt("WanderingTraderSpawnChance", 0) // not sure

        // skip wandering trader id
    }

    private fun generateGameRulesNbt() = NbtCompound().apply {
        val setting = config.world.gameRules

        putString("doWardenSpawning", setting.doWardenSpawning.toString())
        putString("doFireTick", setting.doFireTick.toString())
        putString("doVinesSpread", setting.doVinesSpread.toString())
        putString("doMobSpawning", setting.doMobSpawning.toString())
        putString("doDaylightCycle", setting.doDaylightCycle.toString())
        putString("keepInventory", setting.keepInventory.toString())
        putString("mobGriefing", setting.doMobGriefing.toString())
        putString("doTraderSpawning", setting.doTraderSpawning.toString())
        putString("doPatrolSpawning", setting.doPatrolSpawning.toString())
        putString("doWeatherCycle", setting.doWeatherCycle.toString())

        if (!setting.modifyGameRules) {
            // Keep vanilla defaults when the user does not want forced rule changes.
            putString("doWardenSpawning", "true")
            putString("doFireTick", "true")
            putString("doVinesSpread", "true")
            putString("doMobSpawning", "true")
            putString("doDaylightCycle", "true")
            putString("keepInventory", "false")
            putString("mobGriefing", "true")
            putString("doTraderSpawning", "true")
            putString("doPatrolSpawning", "true")
            putString("doWeatherCycle", "true")
        }
    }

    private fun generatorMockNbt() = NbtCompound().apply {
        putByte("bonus_chest", config.world.worldGenerator.bonusChest.toByte())
        putLong("seed", config.world.worldGenerator.seed)
        putByte("generate_features", config.world.worldGenerator.generateFeatures.toByte())

        put("dimensions", NbtCompound().apply {
            CaptureManager.lastWorldKeys.forEach { key ->
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
            GeneratorType.FLAT -> flatGenerator()
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
                    putString("type", "minecraft:the_end")
                })
                putString("settings", "minecraft:end")
                putString("type", "minecraft:noise")
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

    private fun NbtCompound.flatGenerator() {
        put("settings", NbtCompound().apply {
            putString("biome", "minecraft:plains")
            putByte("features", 0)
            putByte("lakes", 0)
            put("layers", NbtList().apply {
                add(NbtCompound().apply {
                    putString("block", "minecraft:bedrock")
                    putInt("height", 1)
                })
                add(NbtCompound().apply {
                    putString("block", "minecraft:dirt")
                    putInt("height", 2)
                })
                add(NbtCompound().apply {
                    putString("block", "minecraft:grass_block")
                    putInt("height", 1)
                })
            })
            put("structure_overrides", NbtList().apply {
                add(NbtString.of("minecraft:strongholds"))
                add(NbtString.of("minecraft:villages"))
            })
        })
        putString("type", "minecraft:flat")
    }
}
