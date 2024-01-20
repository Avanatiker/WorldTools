package org.waste.of.time.manager


import net.minecraft.client.gui.hud.ClientBossBar
import net.minecraft.text.Text
import org.waste.of.time.WorldTools.config
import org.waste.of.time.manager.CaptureManager.capturing
import org.waste.of.time.storage.StorageFlow
import java.util.*

object BarManager {

    val progressBar =
        ClientBossBar(
            UUID.randomUUID(),
            Text.of(""),
            0f,
            config.advanced.progressBarColor,
            config.advanced.progressBarStyle,
            false,
            false,
            false
        )

    private val captureInfoBar =
        ClientBossBar(
            UUID.randomUUID(),
            Text.of(""),
            1.0f,
            config.advanced.captureBarColor,
            config.advanced.captureBarStyle,
            false,
            false,
            false
        )

    fun progressBar() = if (StorageFlow.lastStored != null) {
        Optional.of(progressBar)
    } else {
        Optional.empty()
    }

    fun getCaptureBar() = if (capturing) {
        Optional.of(captureInfoBar)
    } else {
        Optional.empty()
    }

    fun updateCapture() {
        captureInfoBar.name = StatisticManager.infoMessage
        captureInfoBar.color = config.advanced.captureBarColor
        captureInfoBar.style = config.advanced.captureBarStyle
        progressBar.color = config.advanced.progressBarColor
        progressBar.percent = 0f

        StorageFlow.lastStored?.let {
            progressBar.percent = 1f
            progressBar.name = it.formattedInfo
        }

        val elapsed = System.currentTimeMillis() - StorageFlow.lastStoredTimestamp
        val timeout = elapsed > config.advanced.progressBarTimeout

        if (StorageFlow.lastStoredTimestamp != 0L && timeout) {
            StorageFlow.lastStored = null
            progressBar.percent = 0f
        }
    }
}
