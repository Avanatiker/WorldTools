package org.waste.of.time.storage.cache

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtCompound
import org.waste.of.time.Utils.addAuthor
import org.waste.of.time.Utils.toByte
import org.waste.of.time.WorldTools.config
import org.waste.of.time.storage.Cacheable

data class EntityCacheable(
    val entity: Entity
) : Cacheable {
    fun compound() = NbtCompound().apply {

        // saveSelfNbt has a check for DISCARDED
        EntityType.getId(entity.type)?.let { putString(Entity.ID_KEY, it.toString()) }
        entity.writeNbt(this)

        if (config.entity.modifyEntityNbt) {
            addAuthor()
            putByte("NoAI", config.entity.noAI.toByte())
            putByte("NoGravity", config.entity.noGravity.toByte())
            putByte("Invulnerable", config.entity.invulnerable.toByte())
            putByte("Silent", config.entity.silent.toByte())
        }
    }

    override fun cache() {
        val chunkPos = entity.chunkPos
        val set = HotCache.entities[chunkPos] ?: mutableSetOf()
        // can occur when we reload the same entity
        // equality is based off entity uuid, prefer caching the newest (this) entity
        if (set.contains(this)) set.remove(this)
        set.add(this)
        HotCache.entities[chunkPos] = set
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

    override fun equals(other: Any?): Boolean {
        if (other !is EntityCacheable) return super.equals(other)
        return entity.uuid == other.entity.uuid
    }

    override fun hashCode(): Int {
        // consistent based off entity uuid
        return entity.uuid.hashCode()
    }
}
