package org.waste.of.time.event

import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.storage.CustomRegionBasedStorage

interface Storeable {
    fun store(
        session: Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    )

    fun emit() = StorageFlow.emit(this)
}