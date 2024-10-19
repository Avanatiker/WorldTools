package org.waste.of.time.storage.serializable

import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.MessageManager.infoToast
import org.waste.of.time.manager.MessageManager.sendInfo
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.Storeable

class EndFlow : Storeable() {
    override fun shouldStore() = true

    override val verboseInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.end_flow",
            currentLevelName
        )

    override val anonymizedInfo: MutableText
        get() = verboseInfo

    override fun store(
        session: LevelStorage.Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        StatisticManager.infoMessage.apply {
            infoToast()

            val directory = Text.translatable("worldtools.capture.to_directory")
            val clickToOpen = translateHighlight(
                "worldtools.capture.click_to_open",
                currentLevelName
            ).copy().styled {
                it.withClickEvent(
                    ClickEvent(
                        ClickEvent.Action.OPEN_FILE,
                        session.getDirectory(WorldSavePath.ROOT).toFile().path
                    )
                )
            }

            copy().append(directory).append(clickToOpen).sendInfo()
        }
    }
}