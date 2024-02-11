package org.waste.of.time.storage.cache

import it.unimi.dsi.fastutil.longs.LongArraySet
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.util.math.ChunkPos
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.storage.serializable.PlayerStoreable
import org.waste.of.time.storage.serializable.RegionBasedChunk
import org.waste.of.time.storage.serializable.RegionBasedEntities
import java.util.concurrent.ConcurrentHashMap

/**
 * [HotCache] that caches all currently loaded objects in the world that are needed for the world download.
 * It will be maintained until the user stops the capture process.
 * Then the data will be released into the storage data flow to be serialized in the storage thread.
 * This is needed because objects won't be saved to disk until they are unloaded from the world.
 */
object HotCache {
    val chunks = ConcurrentHashMap<ChunkPos, RegionBasedChunk>()
    internal val savedChunks = LongArraySet()
    val entities = ConcurrentHashMap<ChunkPos, MutableSet<EntityCacheable>>()
    val players: ConcurrentHashMap.KeySetView<PlayerStoreable, Boolean> = ConcurrentHashMap.newKeySet()
    val blockEntities: ConcurrentHashMap.KeySetView<LockableContainerBlockEntity, Boolean> =
        ConcurrentHashMap.newKeySet()
    var lastOpenedContainer: LockableContainerBlockEntity? = null
    // TODO: load existing cached containers from already written chunks
    //  e.g. the player reloads a chunk that contains block entities that we already saved in the storage session
    //  The loading should be done async as it requires IO, so we should add some delay onto the debug rendering here
    //  so it doesn't confuse players as the box would pop in and then later disappear
    val cachedMissingContainers by LazyUpdatingDelegate(100) {
        chunks.values
            .flatMap { it.chunk.blockEntities.values }
            .filterIsInstance<LockableContainerBlockEntity>()
            .filter { it !in blockEntities }
    }

    fun getEntitySerializableForChunk(chunkPos: ChunkPos) =
        entities[chunkPos]?.let { entities ->
            RegionBasedEntities(chunkPos, entities)
        }

    fun convertEntities() =
        entities.map { entry ->
            RegionBasedEntities(entry.key, entry.value)
        }

    // used as public API for external mods like XaeroPlus, change carefully
    fun isChunkSaved(chunkX: Int, chunkZ: Int) = savedChunks.contains(ChunkPos.toLong(chunkX, chunkZ))

    fun clear() {
        chunks.clear()
        savedChunks.clear()
        entities.clear()
        players.clear()
        blockEntities.clear()
        lastOpenedContainer = null
        LOG.info("Cleared hot cache")
    }
}
