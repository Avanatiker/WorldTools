package org.waste.of.time


import net.minecraft.client.gui.hud.ClientBossBar
import net.minecraft.entity.boss.BossBar
import net.minecraft.text.Text
import org.waste.of.time.CaptureManager.capturing
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mm
import org.waste.of.time.event.StorageFlow
import java.util.*

object BarManager {

    private val progressBar =
        ClientBossBar(
            UUID.randomUUID(),
            Text.of(""),
            0f,
            BossBar.Color.GREEN,
            BossBar.Style.PROGRESS,
            false,
            false,
            false
        )

    private val captureInfoBar =
        ClientBossBar(
            UUID.randomUUID(),
            Text.of(""),
            1.0f,
            BossBar.Color.PINK,
            BossBar.Style.NOTCHED_10,
            false,
            false,
            false
        )

    fun getProgressBar() = if (StorageFlow.lastStored != null) {
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
        captureInfoBar.name = StatisticManager.message.mm()

        StorageFlow.lastStored?.let {
            progressBar.percent = 1f
            progressBar.name = "${it.message} (<lang:worldtools.capture.took:${StorageFlow.lastStoredTimeNeeded}>)".mm()
        } ?: run {
            progressBar.percent = 0f
        }

        val elapsed = System.currentTimeMillis() - StorageFlow.lastStoredTimestamp
        val timeout = elapsed > config.advanced.bossBarTimeout

        if (StorageFlow.lastStoredTimestamp != 0L && timeout) {
            StorageFlow.lastStored = null
            progressBar.percent = 0f
        }
    }
}
