package org.waste.of.time.storage.serializable

import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.extension.IBlockEntityContainerExtension
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.cache.HotCache

class BlockEntityLoadable(
    chunk: WorldChunk
) : RegionBasedChunk(chunk) {
    override fun shouldStore() =
        config.general.reloadBlockEntities && chunk.blockEntities.isNotEmpty()

    override val verboseInfo = translateHighlight(
        "worldtools.capture.loaded.block_entities",
        chunk.pos,
        chunk.world.registryKey.value.path
    )

    override val anonymizedInfo = translateHighlight(
        "worldtools.capture.loaded.block_entities.anonymized",
        chunk.world.registryKey.value.path
    )

    override fun load(
        session: LevelStorage.Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        LOG.info("[BlockEntity Load] Loading block entities for chunk {} with entities {}", chunk.pos, chunk.blockEntities.size)
        generateStorage(session, cachedStorages)
            .getBlockEntities(chunkPos)
            .filterIsInstance<LockableContainerBlockEntity>()
            .forEach { existing ->
                LOG.info("[BlockEntity Merge] Checking for existing block entity at {}", existing.pos)
                HotCache.chunks[chunkPos]?.cachedBlockEntities?.get(existing.pos)?.let { cached ->
                    if (cached !is LockableContainerBlockEntity) return@let
                    LOG.info("[BlockEntity Merge] Found cached block entity at {}", cached.pos)
                    syncContainer(existing, cached)
                }
            }
    }

    fun tryEmit() {
//        if (shouldStore()) emit()
    }

    // in-place merge onto the new block entity container contents
    private fun syncContainer(
        existing: LockableContainerBlockEntity,
        cached: LockableContainerBlockEntity
    ) {
        if ((cached as IBlockEntityContainerExtension).wtContentsRead) return
        if (existing.pos != cached.pos) {
            LOG.warn("[Container Merge] BlockEntity at ${cached.pos} is not at the expected pos ${existing.pos}")
            return
        }
        if (existing.javaClass != cached.javaClass) {
            LOG.warn("[Container Merge] BlockEntity type ${cached.javaClass} is not the expected type ${existing.javaClass}")
            return
        }
        if (!cached.isEmpty) return

        // todo: we need extra context to know if the newStacks have been captured during the WDL
        //  or if we just loaded the block entity without viewing its content
        //  e.g. the player reloads a block entity and removes all items from its inventory during WDL
        repeat(existing.size()) { i ->
            cached.setStack(i, existing.getStack(i))
        }
        HotCache.scannedContainers[cached.pos] = cached
    }
}