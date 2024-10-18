package org.waste.of.time.storage.cache

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtCompound
import org.waste.of.time.Utils.toByte
import org.waste.of.time.WorldTools.TIMESTAMP_KEY
import org.waste.of.time.WorldTools.config
import org.waste.of.time.storage.Cacheable

data class EntityCacheable(
    val entity: Entity
) : Cacheable {
    fun compound() = NbtCompound().apply {
        // saveSelfNbt has a check for RemovalReason.DISCARDED
        EntityType.getId(entity.type)?.let { putString(Entity.ID_KEY, it.toString()) }
        entity.writeNbt(this)

        if (config.entity.behavior.modifyEntityBehavior) {
            putByte("NoAI", config.entity.behavior.noAI.toByte())
            putByte("NoGravity", config.entity.behavior.noGravity.toByte())
            putByte("Invulnerable", config.entity.behavior.invulnerable.toByte())
            putByte("Silent", config.entity.behavior.silent.toByte())
        }

        if (config.entity.metadata.captureTimestamp) {
            putLong(TIMESTAMP_KEY, System.currentTimeMillis())
        }
    }

    override fun cache() {
        HotCache.entities.computeIfAbsent(entity.chunkPos) { mutableSetOf() }.apply {
            // Remove the entity if it already exists to update it
            removeIf { it.entity.uuid == entity.uuid }
            add(this@EntityCacheable)
        }
    }

    override fun flush() {
        val chunkPos = entity.chunkPos
        HotCache.entities[chunkPos]?.let { list ->
            list.remove(this)
            if (list.isEmpty()) {
                HotCache.entities.remove(chunkPos)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EntityCacheable) return super.equals(other)
        return entity.uuid == other.entity.uuid
    }

    override fun hashCode() = entity.uuid.hashCode()
}
