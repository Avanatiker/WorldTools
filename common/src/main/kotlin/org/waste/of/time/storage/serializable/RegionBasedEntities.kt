package org.waste.of.time.storage.serializable

import net.minecraft.SharedConstants
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIntArray
import net.minecraft.nbt.NbtList
import net.minecraft.text.MutableText
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.manager.StatisticManager.joinWithAnd
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.RegionBased
import org.waste.of.time.storage.cache.EntityCacheable

class RegionBasedEntities(
    chunkPos: ChunkPos,
    val entities: Set<EntityCacheable>, // can be empty, signifies we should clear any previously saved entities
    world: World
) : RegionBased(chunkPos, world, "entities") {
    override fun shouldStore() = config.general.capture.entities

    override val verboseInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.entities",
            stackEntities(),
            chunkPos,
            dimension
        )

    override val anonymizedInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.entities.anonymized",
            stackEntities(),
            dimension
        )

    override fun compound() = NbtCompound().apply {
        put("Entities", NbtList().apply {
            entities.forEach { entity ->
                add(entity.compound())
            }
        })

        putInt("DataVersion", SharedConstants.getGameVersion().saveVersion.id)
        put("Position", NbtIntArray(intArrayOf(chunkPos.x, chunkPos.z)))
        if (config.debug.logSavedEntities) {
            entities.forEach { entity -> LOG.info("Entity saved: $entity (Chunk: $chunkPos)") }
        }
    }

    override fun writeToStorage(
        session: LevelStorage.Session,
        storage: CustomRegionBasedStorage,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        if (entities.isEmpty()) {
            // remove any previously stored entities in this chunk
            if (config.debug.logSavedEntities) {
                LOG.info("Removing any previously saved entities from chunk: {}", chunkPos)
            }
            storage.write(chunkPos, null)
            return
        }
        super.writeToStorage(session, storage, cachedStorages)
    }

    override fun incrementStats() {
        // todo: entities count becomes completely wrong when entities are removed or are loaded twice during the capture
        //  i.e. the player moves away, unloads them, and then comes back to load them again
        StatisticManager.entities += entities.size
        StatisticManager.dimensions.add(dimension)
    }

    private fun stackEntities() = entities.groupBy {
        it.entity.name
    }.map {
        val count = if (it.value.size > 1) {
            " (${it.value.size})"
        } else ""
        it.key.copy().append(count)
    }.joinWithAnd()
}
