package org.waste.of.time.event.serializable

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtCompound
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.addAuthor
import org.waste.of.time.event.Cacheable
import org.waste.of.time.event.HotCache

data class EntityCacheable(
    val entity: Entity
) : Cacheable {
    fun compound() = NbtCompound().apply {
        addAuthor()

        // saveSelfNbt has a check for DISCARDED
        EntityType.getId(entity.type)?.let { putString(Entity.ID_KEY, it.toString()) }
        entity.writeNbt(this)

        if (WorldTools.config.freezeEntities) {
            putByte("NoAI", 1)
            putByte("NoGravity", 1)
            putByte("Invulnerable", 1)
            putByte("Silent", 1)
        }
    }

    override fun cache() {
        val chunkPos = entity.chunkPos
        val list = HotCache.entities[chunkPos] ?: mutableListOf()

        list.add(this)
        HotCache.entities[chunkPos] = list
    }

    override fun flush() {
        val chunkPos = entity.chunkPos
        HotCache.entities[chunkPos]?.let { list ->
            list.remove(this)
            if (list.isEmpty()) {
                HotCache.entities.remove(chunkPos)
            } else {
                HotCache.entities[chunkPos] = list
            }
        }
    }
}
