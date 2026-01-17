package org.waste.of.time.storage

import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.WorldTools.config

abstract class Storeable {
    abstract fun store(
        session: Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    )

    abstract fun shouldStore(): Boolean

    abstract val verboseInfo: MutableText

    abstract val anonymizedInfo: MutableText

    fun emit() = StorageFlow.emit(this)

    val formattedInfo: Text by lazy {
        if (config.advanced.anonymousMode) {
            anonymizedInfo
        } else {
            verboseInfo
        }.append(translateHighlight("worldtools.capture.took", StorageFlow.lastStoredTimeNeeded))
    }
}