package org.waste.of.time.storage.serializable

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.text.MutableText
import net.minecraft.util.Util
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.Utils.asString
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.config
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.storage.Cacheable
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.Storeable
import org.waste.of.time.storage.cache.HotCache
import java.io.File

data class PlayerStoreable(
    val player: PlayerEntity
) : Cacheable, Storeable {
    override fun shouldStore() = config.capture.players

    override val verboseInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.player",
            player.name,
            player.pos.asString(),
            player.world.registryKey.value.path
        )

    override val anonymizedInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.player.anonymized",
            player.name,
            player.world.registryKey.value.path
        )

    override fun cache() {
        HotCache.players.add(this)
    }

    override fun flush() {
        HotCache.players.remove(this)
    }

    override fun store(session: Session, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        savePlayerData(player, session)
        StatisticManager.players++
        StatisticManager.dimensions.add(player.world.registryKey.value.path)
    }

    private fun savePlayerData(player: PlayerEntity, session: Session) {
        try {
            val playerDataDir = session.getDirectory(WorldSavePath.PLAYERDATA).toFile()
            playerDataDir.mkdirs()
            val nbtCompound = player.writeNbt(NbtCompound())
            nbtCompound.remove("LastDeathLocation") // can contain sensitive information
            val file = File.createTempFile(player.uuidAsString + "-", ".dat", playerDataDir)
            NbtIo.writeCompressed(nbtCompound, file)
            val file2 = File(playerDataDir, player.uuidAsString + ".dat")
            val file3 = File(playerDataDir, player.uuidAsString + ".dat_old")
            Util.backupAndReplace(file2, file, file3)
        } catch (var6: Exception) {
            WorldTools.LOG.warn("Failed to save player data for {}", player.name.string)
        }
    }
}
