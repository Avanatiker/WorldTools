package org.waste.of.time.event.serializable

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.StatisticManager
import org.waste.of.time.WorldTools.highlight
import org.waste.of.time.WorldTools.mm
import org.waste.of.time.event.Cacheable
import org.waste.of.time.event.HotCache
import org.waste.of.time.event.Storeable
import org.waste.of.time.storage.CustomRegionBasedStorage

data class PlayerStoreable(
    val player: PlayerEntity
) : Cacheable, Storeable {
    override fun toString() = "Player ${player.name.string}"

    override val message: Text
        get() = "Saving player ${highlight(player.name.string)} at ${highlight(player.pos.toString())} in dimension ${highlight(player.world.registryKey.value.path)}...".mm()

    override fun cache() {
        HotCache.players.add(this)
    }

    override fun flush() {
        HotCache.players.remove(this)
    }

    override fun store(session: Session, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        session.createSaveHandler().savePlayerData(player)
        StatisticManager.players++
    }
}