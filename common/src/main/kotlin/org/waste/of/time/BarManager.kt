package org.waste.of.time


import net.minecraft.client.gui.hud.ClientBossBar
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.BossBar
import net.minecraft.text.Text
import net.minecraft.util.math.ChunkPos
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.caching
import org.waste.of.time.WorldTools.mc
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

    fun getProgressBar() = if (StorageFlow.currentStoreable != null) {
        Optional.of(progressBar)
    } else {
        Optional.empty()
    }

    fun getCaptureBar() = if (caching) {
        Optional.of(captureInfoBar)
    } else {
        Optional.empty()
    }

    fun updateCapture() {
        captureInfoBar.name = StatisticManager.message.mm()

        StorageFlow.currentStoreable?.let {
            progressBar.percent = 1f
            progressBar.name = it.message
        } ?: run {
            progressBar.percent = 0f
        }

        if (StorageFlow.lastStoreable != 0L && System.currentTimeMillis() - StorageFlow.lastStoreable > 1000) {
            StorageFlow.currentStoreable = null
            progressBar.percent = 0f
        }
    }
}
