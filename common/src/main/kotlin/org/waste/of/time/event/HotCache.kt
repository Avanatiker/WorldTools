package org.waste.of.time.event

import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.util.math.ChunkPos
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.event.serializable.RegionBasedChunk
import org.waste.of.time.event.serializable.EntityCacheable
import org.waste.of.time.event.serializable.PlayerStoreable
import org.waste.of.time.event.serializable.RegionBasedEntities
import java.util.concurrent.ConcurrentHashMap

object HotCache {
    val chunks = ConcurrentHashMap<ChunkPos, RegionBasedChunk>()
    val entities = ConcurrentHashMap<ChunkPos, MutableList<EntityCacheable>>()
    val players: ConcurrentHashMap.KeySetView<PlayerStoreable, Boolean> = ConcurrentHashMap.newKeySet()
    val blockEntities: ConcurrentHashMap.KeySetView<ChestBlockEntity, Boolean> = ConcurrentHashMap.newKeySet()
    var lastOpenedContainer: ChestBlockEntity? = null

    fun getEntitySerializableForChunk(chunkPos: ChunkPos) =
        entities[chunkPos]?.let { entities ->
            if (entities.isEmpty()) {
                LOG.info("No entities in chunk $chunkPos")
                return@let null
            }

            RegionBasedEntities(chunkPos, entities)
        }

    fun convertEntities() =
        entities.map { entry ->
            RegionBasedEntities(entry.key, entry.value)
        }

    fun clear() {
        chunks.clear()
        entities.clear()
        players.clear()
        blockEntities.clear()
        lastOpenedContainer = null
        LOG.info("Cleared hot cache")
    }
}