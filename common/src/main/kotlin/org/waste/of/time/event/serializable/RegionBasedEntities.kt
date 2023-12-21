package org.waste.of.time.event.serializable

import net.minecraft.SharedConstants
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIntArray
import net.minecraft.nbt.NbtList
import net.minecraft.text.Text
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import org.waste.of.time.StatisticManager
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.highlight
import org.waste.of.time.WorldTools.mm
import org.waste.of.time.WorldTools.sanitize
import org.waste.of.time.event.RegionBased

data class RegionBasedEntities(
    override val chunkPos: ChunkPos,
    val entities: MutableList<EntityCacheable>
) : RegionBased {
    override fun toString() = "${entities.size} entities in chunk $chunkPos"

    override fun shouldStore() = config.capture.entities

    override val message: String
        get() = "<lang:worldtools.capture.saved.entities:${
            entities.joinToString { it.entity.name.string.sanitize() }
        }:$chunkPos:$dimension>"

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
}