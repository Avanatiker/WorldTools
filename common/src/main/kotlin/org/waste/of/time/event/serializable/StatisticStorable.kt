package org.waste.of.time.event.serializable

import net.minecraft.text.Text
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.event.Storeable
import org.waste.of.time.serializer.StatisticSerializer
import org.waste.of.time.storage.CustomRegionBasedStorage

class StatisticStorable : Storeable {
    override fun toString() = "Statistics"

    override val message: Text
        get() = Text.of("Saving statistics...")

    override fun store(session: LevelStorage.Session, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        StatisticSerializer.writeStats(session)
    }
}