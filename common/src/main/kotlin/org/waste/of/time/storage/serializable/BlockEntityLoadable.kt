package org.waste.of.time.storage.serializable

import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.LecternBlockEntity
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.WorldTools.config
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

    fun load(
        session: LevelStorage.Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        generateStorage(session, cachedStorages)
            .getBlockEntities(chunkPos)
            .forEach { existing ->
                HotCache.chunks[chunkPos]
                    ?.cachedBlockEntities
                    ?.get(existing.pos)
                    ?.let { blockEntity ->
                        when (blockEntity) {
                            is LockableContainerBlockEntity -> blockEntity.migrateData(existing)
                            is LecternBlockEntity -> blockEntity.migrateData(existing)
                        }
                    }
            }
    }

    private fun LockableContainerBlockEntity.migrateData(existing: BlockEntity) {
        if (existing !is LockableContainerBlockEntity) return
        if (!isEmpty) return
        heldStacks = existing.heldStacks
        HotCache.scannedBlockEntities[existing.pos] = this
    }

    private fun LecternBlockEntity.migrateData(existing: BlockEntity) {
        if (existing !is LecternBlockEntity) return
        if (!book.isEmpty) return
        book = existing.book
        HotCache.scannedBlockEntities[existing.pos] = this
    }
}
