package org.waste.of.time.event.serializable

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtCompound
import org.waste.of.time.WorldTools.addAuthor
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.toByte
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

        if (config.entity.modifyNBT) {
            putByte("NoAI", config.entity.noAI.toByte())
            putByte("NoGravity", config.entity.noGravity.toByte())
            putByte("Invulnerable", config.entity.invulnerable.toByte())
            putByte("Silent", config.entity.silent.toByte())
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
