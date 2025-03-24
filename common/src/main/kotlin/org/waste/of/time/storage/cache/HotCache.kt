package org.waste.of.time.storage.cache

import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongCollection
import it.unimi.dsi.fastutil.longs.LongLists
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.LecternBlockEntity
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.vehicle.VehicleInventory
import net.minecraft.inventory.EnderChestInventory
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.StatisticManager
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
    internal val savedDimensionChunks = ConcurrentHashMap<RegistryKey<World>, LongOpenHashSet>()
    val entities = ConcurrentHashMap<ChunkPos, MutableSet<EntityCacheable>>()
    val players: ConcurrentHashMap.KeySetView<PlayerStoreable, Boolean> = ConcurrentHashMap.newKeySet()
    val scannedBlockEntities = ConcurrentHashMap<BlockPos, BlockEntity>()
    private val scannedEntities: ConcurrentHashMap.KeySetView<Entity, Boolean> = ConcurrentHashMap.newKeySet()
    val loadedBlockEntities = ConcurrentHashMap<BlockPos, BlockEntity>()
    var lastInteractedBlockEntity: BlockEntity? = null
    var lastInteractedEntity: Entity? = null
    val unscannedBlockEntities by LazyUpdatingDelegate(100) {
        chunks.values
            .flatMap { it.chunk.blockEntities.values }
            .filter { it.isSupported }
            .filterNot { scannedBlockEntities.containsKey(it.pos) }
    }
    val unscannedEntities by LazyUpdatingDelegate(100) {
        entities.values
            .flatten()
            .filter { it.entity.isSupported }
            .filterNot { it.entity in scannedEntities }
    }
    // map id's of maps that we've seen during the capture
    val mapIDs = mutableSetOf<Int>()
    val BlockEntity.isSupported get() =
        this is LockableContainerBlockEntity
                || this is LecternBlockEntity
    val Entity.isSupported get() = this is VehicleInventory

    fun getEntitySerializableForChunk(chunkPos: ChunkPos, world: World) =
        entities[chunkPos]?.let { entities ->
            RegionBasedEntities(chunkPos, entities, world)
        }

    /**
     * Used as a public API for external mods like [XaeroPlus](https://github.com/rfresh2/XaeroPlus), change carefully.
     *
     * @param chunkX The X coordinate of the chunk.
     * @param chunkZ The Z coordinate of the chunk.
     * @return True if the chunk is saved, false otherwise.
     */
    @Deprecated("This method will default to the current dimension. Please use the new method by passing in a dimension.")
    @Suppress("unused")
    fun isChunkSaved(chunkX: Int, chunkZ: Int) = isChunkSaved(chunkX, chunkZ, mc.world?.registryKey!!)

    /**
     * Used as a public API for external mods like [XaeroPlus](https://github.com/rfresh2/XaeroPlus), change carefully.
     *
     * @param chunkX The X coordinate of the chunk.
     * @param chunkZ The Z coordinate of the chunk.
     * @param dimension The dimension of the chunk.
     * @return True if the chunk is saved, false otherwise.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun isChunkSaved(chunkX: Int, chunkZ: Int, dimension: RegistryKey<World>): Boolean {
        val savedChunks = savedDimensionChunks[dimension] ?: return false
        return savedChunks.contains(ChunkPos.toLong(chunkX, chunkZ))
    }

    /**
     * Returns a [LongCollection] of all the chunk positions saved by WorldTools.
     *
     * @param dimension The dimension to get the saved chunks.
     * @return All the chunk positions saved.
     */
    @Suppress("unused")
    fun getSavedChunks(dimension: RegistryKey<World>): LongCollection {
        val savedChunks = savedDimensionChunks[dimension] ?: return LongLists.EMPTY_LIST
        synchronized(savedChunks) {
            return LongArrayList(savedChunks)
        }
    }

    fun clear() {
        chunks.clear()
        savedDimensionChunks.clear()
        entities.clear()
        players.clear()
        scannedBlockEntities.clear()
        loadedBlockEntities.clear()
        mapIDs.clear()

        // failing to reset this could cause users to accidentally save their echest contents on subsequent captures
        if (!mc.isInSingleplayer && !config.advanced.keepEnderChestContents) {
            mc.player?.enderChestInventory = EnderChestInventory()
        }
        lastInteractedBlockEntity = null
        LOG.info("Cleared hot cache")
    }

    fun BlockEntity.markScanned(fromCache: Boolean = false) {
        if (fromCache) {
            loadedBlockEntities[pos] = this
        } else {
            scannedBlockEntities[pos] = this
            loadedBlockEntities.remove(pos)
        }

        world?.registryKey?.value?.path?.let {
            StatisticManager.dimensions.add(it)
        }
        if (config.debug.logSavedContainers) {
            LOG.info("Saved block entity: ${Registries.BLOCK_ENTITY_TYPE.getId(type)?.path} at $pos")
        }
    }

    fun Entity.markScanned() {
        scannedEntities.add(this)
    }
}
