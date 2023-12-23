package org.waste.of.time.storage.serializable

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.manager.MessageManager.asString
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.WorldTools.config
import org.waste.of.time.storage.Cacheable
import org.waste.of.time.storage.cache.HotCache
import org.waste.of.time.storage.Storeable
import org.waste.of.time.storage.CustomRegionBasedStorage

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
        session.createSaveHandler().savePlayerData(player)
        StatisticManager.players++
        StatisticManager.dimensions.add(player.world.registryKey.value.path)
    }
}