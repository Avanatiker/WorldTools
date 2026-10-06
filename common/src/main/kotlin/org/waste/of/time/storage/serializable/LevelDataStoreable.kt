package org.waste.of.time.storage.serializable

import net.minecraft.SharedConstants
import net.minecraft.entity.boss.dragon.EnderDragonFight
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.*
import net.minecraft.predicate.NbtPredicate
import net.minecraft.resource.DataConfiguration
import net.minecraft.resource.featuretoggle.FeatureFlags
import net.minecraft.text.MutableText
import net.minecraft.util.Util
import net.minecraft.util.WorldSavePath
import net.minecraft.world.WorldProperties
import net.minecraft.world.border.WorldBorder
import net.minecraft.world.level.storage.LevelStorage.Session
import net.minecraft.world.rule.GameRules
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

        put("Version", NbtCompound().apply {
            putString("Name", SharedConstants.getGameVersion().name())
            putInt("Id", SharedConstants.getGameVersion().dataVersion().id)
            putBoolean("Snapshot", !SharedConstants.getGameVersion().stable())
            putString("Series", SharedConstants.getGameVersion().dataVersion().series)
        })

        NbtHelper.putDataVersion(this)

        put("WorldGenSettings", generatorMockNbt())
        mc.networkHandler?.listedPlayerListEntries?.find {
            it.profile.id == player.uuid
        }?.let {
            putInt("GameType", it.gameMode.index)
        } ?: putInt("GameType", player.entityWorld.server?.defaultGameMode?.index ?: 0)

        put("spawn", WorldProperties.SpawnPoint.CODEC, player.entityWorld.levelProperties.spawnPoint)
        putLong("Time", player.entityWorld.time)
        putLong("DayTime", player.entityWorld.timeOfDay)
        putLong("LastPlayed", System.currentTimeMillis())
        putString("LevelName", currentLevelName)
        putInt("version", 19133)
        putInt("clearWeatherTime", 0) // not sure
        putInt("rainTime", 0) // not sure
        putBoolean("raining", player.entityWorld.isRaining)
        putBoolean("thundering", player.entityWorld.isThundering)
        putBoolean("hardcore", player.entityWorld.server?.isHardcore ?: false)
        putInt("thunderTime", 0) // not sure
        putBoolean("allowCommands", true) // not sure
        putBoolean("initialized", true) // not sure

        put("world_border", WorldBorder.Properties.CODEC, WorldBorder.Properties(player.entityWorld.worldBorder))

        putByte("Difficulty", player.entityWorld.levelProperties.difficulty.id.toByte())
        putBoolean("DifficultyLocked", false) // not sure

        // ToDo: Seems that the client side game rules were removed. Now only works for single player :/
        val rules = genGameRules(player)
        put("game_rules", GameRules.createCodec(FeatureFlags.VANILLA_FEATURES), rules)
        put("Player", NbtPredicate.entityToNbt(player).apply {
            remove("LastDeathLocation") // can contain sensitive information
            putString("Dimension", "minecraft:${player.entityWorld.registryKey.value.path}")
        })

        put("DragonFight", EnderDragonFight.Data.CODEC, EnderDragonFight.Data.DEFAULT)
        copyFromCodec(DataConfiguration.MAP_CODEC, DataConfiguration.SAFE_MODE)
        put("CustomBossEvents", NbtCompound()) // not sure
        put("ScheduledEvents", NbtList()) // not sure
        putInt("WanderingTraderSpawnDelay", 0) // not sure
        putInt("WanderingTraderSpawnChance", 0) // not sure

        // skip wandering trader id
    }

    private fun genGameRules(player: PlayerEntity): GameRules {
        val serverRules = player.entityWorld.server?.saveProperties?.gameRules
        val rules = if (serverRules != null) GameRules(FeatureFlags.VANILLA_FEATURES).apply { copyFrom(serverRules, null) } else GameRules(FeatureFlags.VANILLA_FEATURES)
        val setting = config.world.gameRules
        if (!setting.modifyGameRules) return rules

        rules.setValue(GameRules.SPAWN_WARDENS, setting.doWardenSpawning, null)
        rules.setValue(GameRules.SPREAD_VINES, setting.doVinesSpread, null)
        rules.setValue(GameRules.DO_MOB_SPAWNING, setting.doMobSpawning, null)
        rules.setValue(GameRules.ADVANCE_TIME, setting.doDaylightCycle, null)
        rules.setValue(GameRules.KEEP_INVENTORY, setting.keepInventory, null)
        rules.setValue(GameRules.DO_MOB_GRIEFING, setting.doMobGriefing, null)
        rules.setValue(GameRules.SPAWN_WANDERING_TRADERS, setting.doTraderSpawning, null)
        rules.setValue(GameRules.SPAWN_PATROLS, setting.doPatrolSpawning, null)
        rules.setValue(GameRules.ADVANCE_WEATHER, setting.doWeatherCycle, null)
        return rules
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
