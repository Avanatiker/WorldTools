package org.waste.of.time.event.serializable

import net.minecraft.text.MutableText
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.MessageManager.translateHighlight
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.WorldTools.sanitize
import org.waste.of.time.event.Storeable
import org.waste.of.time.serializer.StatisticSerializer
import org.waste.of.time.storage.CustomRegionBasedStorage

class StatisticStorable : Storeable {
    override fun shouldStore() = config.capture.statistics

    override val verboseInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.statistics",
            mc.player?.name ?: "Unknown"
        )

    override val anonymizedInfo: MutableText
        get() = verboseInfo

    override fun store(session: LevelStorage.Session, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        StatisticSerializer.writeStats(session)
    }
}