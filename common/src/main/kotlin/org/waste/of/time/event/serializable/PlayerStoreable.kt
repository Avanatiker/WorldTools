package org.waste.of.time.event.serializable

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.MessageManager.asString
import org.waste.of.time.MessageManager.translateHighlight
import org.waste.of.time.StatisticManager
import org.waste.of.time.WorldTools.config
import org.waste.of.time.event.Cacheable
import org.waste.of.time.event.HotCache
import org.waste.of.time.event.Storeable
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