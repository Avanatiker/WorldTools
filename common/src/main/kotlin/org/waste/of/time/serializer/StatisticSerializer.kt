package org.waste.of.time.serializer

import com.google.gson.JsonObject
import net.minecraft.SharedConstants
import net.minecraft.registry.Registries
import net.minecraft.util.PathUtil
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.mixin.accessor.StatHandlerAccessor

object StatisticSerializer {
    fun writeStats(session: Session) {
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

        LOG.info("Saved ${completeStatMap.entries.size} stats.")
    }
}