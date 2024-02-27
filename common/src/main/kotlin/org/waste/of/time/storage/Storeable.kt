package org.waste.of.time.storage

import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.WorldTools.config

interface Storeable {
    fun store(
        session: Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    )

    fun load(
        session: Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {}

    fun emit() = StorageFlow.emit(this)

    fun shouldStore(): Boolean

    val verboseInfo: MutableText

    val anonymizedInfo: MutableText

    val formattedInfo: Text
        get() = if (config.advanced.anonymousMode) {
            anonymizedInfo
        } else {
            verboseInfo
        }.append(translateHighlight("worldtools.capture.took", StorageFlow.lastStoredTimeNeeded))
}