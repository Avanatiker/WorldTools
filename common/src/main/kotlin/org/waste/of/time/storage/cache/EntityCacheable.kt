package org.waste.of.time.storage.cache

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.storage.NbtWriteView
import net.minecraft.util.ErrorReporter
import org.waste.of.time.Utils.toByte
import org.waste.of.time.WorldTools.TIMESTAMP_KEY
import org.waste.of.time.WorldTools.config
import org.waste.of.time.storage.Cacheable

data class EntityCacheable(
    val entity: Entity
) : Cacheable {
    fun compound(): NbtCompound {
        val view = NbtWriteView.create(ErrorReporter.EMPTY, entity.registryManager)
        entity.saveData(view)
        val nbt = view.nbt

        // Keep a valid id in the entity region even for edge-case client-side entities.
        EntityType.getId(entity.type)?.let { nbt.putString(Entity.ID_KEY, it.toString()) }

        if (config.entity.behavior.modifyEntityBehavior) {
            nbt.putByte("NoAI", config.entity.behavior.noAI.toByte())
            nbt.putByte("NoGravity", config.entity.behavior.noGravity.toByte())
            nbt.putByte("Invulnerable", config.entity.behavior.invulnerable.toByte())
            nbt.putByte("Silent", config.entity.behavior.silent.toByte())
        }

        if (config.entity.metadata.captureTimestamp) {
            nbt.putLong(TIMESTAMP_KEY, System.currentTimeMillis())
        }

        return nbt
    }

    override fun cache() {
        HotCache.entities.computeIfAbsent(entity.getChunkPos()) { mutableSetOf() }.apply {
            // Remove the entity if it already exists to update it
            removeIf { it.entity.uuid == entity.uuid }
            add(this@EntityCacheable)
        }
    }

    override fun flush() {
        val chunkPos = entity.getChunkPos()
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
