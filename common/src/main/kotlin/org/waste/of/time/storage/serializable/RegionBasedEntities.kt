package org.waste.of.time.storage.serializable

import net.minecraft.SharedConstants
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIntArray
import net.minecraft.nbt.NbtList
import net.minecraft.text.MutableText
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.manager.StatisticManager.joinWithAnd
import org.waste.of.time.WorldTools.config
import org.waste.of.time.storage.RegionBased
import org.waste.of.time.storage.cache.EntityCacheable

data class RegionBasedEntities(
    override val chunkPos: ChunkPos,
    val entities: MutableList<EntityCacheable>
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

    override val world: World = entities.first().entity.world

    override val suffix: String
        get() = "entities"

    override fun compound() = NbtCompound().apply {
        put("Entities", NbtList().apply {
            entities.toList().forEach { entity ->
                add(entity.compound())
            }
        })

        putInt("DataVersion", SharedConstants.getGameVersion().saveVersion.id)
        put("Position", NbtIntArray(intArrayOf(chunkPos.x, chunkPos.z)))
    }

    override fun incrementStats() {
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