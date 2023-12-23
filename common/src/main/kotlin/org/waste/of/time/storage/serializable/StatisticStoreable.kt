package org.waste.of.time.storage.serializable

import com.google.gson.JsonObject
import net.minecraft.SharedConstants
import net.minecraft.registry.Registries
import net.minecraft.text.MutableText
import net.minecraft.util.PathUtil
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.storage.Storeable
import org.waste.of.time.mixin.accessor.StatHandlerAccessor
import org.waste.of.time.storage.CustomRegionBasedStorage

class StatisticStoreable : Storeable {
    override fun shouldStore() = config.capture.statistics

    override val verboseInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.statistics",
            mc.player?.name ?: "Unknown"
        )

    override val anonymizedInfo: MutableText
        get() = verboseInfo

    override fun store(session: LevelStorage.Session, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        // we need to get the stat map from the player's stat handler instead of the packet because the packet only
        // contains the stats that have changed since the last time the packet was sent
        val completeStatMap = (mc.player?.statHandler as? StatHandlerAccessor)?.statMap?.toMap() ?: return

        val uuid = mc.player?.uuid ?: return
        val statDirectory = session.getDirectory(WorldSavePath.STATS)
        val path = statDirectory.resolve("$uuid.json").toFile()

        PathUtil.createDirectories(statDirectory)

        path.writeText(JsonObject().apply {
            addProperty("Author", WorldTools.CREDIT_MESSAGE)
            add("stats", JsonObject().apply {
                completeStatMap.entries.groupBy { it.key.type }.forEach { (type, entries) ->
                    val typeObject = JsonObject()
                    entries.forEach { (stat, value) ->
                        stat.name.split(":").getOrNull(1)?.replace('.', ':')?.let {
                            typeObject.addProperty(it, value)
                        }
                    }
                    add(Registries.STAT_TYPE.getId(type).toString(), typeObject)
                }
            })
            addProperty("DataVersion", SharedConstants.getGameVersion().saveVersion.id)
        }.toString())

        WorldTools.LOG.info("Saved ${completeStatMap.entries.size} stats.")
    }
}