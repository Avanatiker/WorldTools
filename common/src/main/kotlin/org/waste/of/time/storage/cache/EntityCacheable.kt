package org.waste.of.time.storage.cache

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.mob.MobEntity
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

        // PersistenceRequired is server-authoritative and arrives as false on the
        // client, so name-tagged mobs would despawn after world load. Restore the
        // flag here: always for named mobs (vanilla's NameTagItem path), and for
        // every mob when the aggressive opt-in is set.
        if (entity is MobEntity && (entity.hasCustomName() || config.entity.behavior.forceMobPersistence)) {
            putByte("PersistenceRequired", 1)
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
