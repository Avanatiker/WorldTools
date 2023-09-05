package org.waste.of.time.serializer

import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.SharedConstants
import net.minecraft.registry.Registries
import net.minecraft.util.PathUtil
import net.minecraft.util.WorldSavePath
import org.waste.of.time.WorldTools
import org.waste.of.time.mixin.accessor.StatHandlerAccessor

object StatisticSerializer {
    fun writeStats() {
        // we need to get the stat map from the player's stat handler instead of the packet because the packet only
        // contains the stats that have changed since the last time the packet was sent
        val completeStatMap = (WorldTools.mc.player?.statHandler as? StatHandlerAccessor)?.statMap?.toMap() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            WorldTools.withSessionBlocking {
                val uuid = WorldTools.mc.player?.uuid ?: return@withSessionBlocking
                val statDirectory = getDirectory(WorldSavePath.STATS)
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

                WorldTools.LOGGER.info("Saved ${completeStatMap.entries.size} stats.")
            }
        }
    }
}