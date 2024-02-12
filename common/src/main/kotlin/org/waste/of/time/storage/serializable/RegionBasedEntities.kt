package org.waste.of.time.storage.serializable

import net.minecraft.SharedConstants
import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIntArray
import net.minecraft.nbt.NbtList
import net.minecraft.text.MutableText
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.config
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.manager.StatisticManager.joinWithAnd
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.RegionBased
import org.waste.of.time.storage.cache.EntityCacheable

data class RegionBasedEntities(
    override val chunkPos: ChunkPos,
    val entities: Set<EntityCacheable> // can be empty, signifies we should clear any previously saved entities
) : RegionBased {
    override fun shouldStore() = config.capture.entities

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

    override val world: World = entities.firstOrNull()?.entity?.world ?: MinecraftClient.getInstance().world!!

    override val suffix: String
        get() = "entities"

    override fun compound(storage: CustomRegionBasedStorage) = NbtCompound().apply {
        put("Entities", NbtList().apply {
            entities.forEach { entity ->
                add(entity.compound())
            }
        })

        putInt("DataVersion", SharedConstants.getGameVersion().saveVersion.id)
        put("Position", NbtIntArray(intArrayOf(chunkPos.x, chunkPos.z)))
        if (config.debug.logSavedEntities)
            entities.forEach { entity -> WorldTools.LOG.info("Entity saved: $entity (Chunk: $chunkPos)") }
    }

    override fun writeToStorage(session: LevelStorage.Session, storage: CustomRegionBasedStorage, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        if (this.entities.isEmpty()) {
            // remove any previously stored entities in this chunk
            if (WorldTools.config.debug.logSavedEntities)
                WorldTools.LOG.info("Removing any previously saved entities from chunk: {}", chunkPos)
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
