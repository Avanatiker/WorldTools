package org.waste.of.time

import net.minecraft.client.gui.hud.ClientBossBar
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.BossBar
import net.minecraft.text.Text
import net.minecraft.util.math.ChunkPos
import org.waste.of.time.WorldTools.MAX_CACHE_SIZE
import org.waste.of.time.WorldTools.cachedChunks
import org.waste.of.time.WorldTools.cachedEntities
import org.waste.of.time.WorldTools.mm
import org.waste.of.time.mixin.BossBarHudAccessor
import java.util.*
import kotlin.math.max

object BarManager {
    private val progressBar = ClientBossBar(
        UUID.randomUUID(),
        Text.of(""),
        0.0f,
        BossBar.Color.GREEN,
        BossBar.Style.PROGRESS,
        false,
        false,
        false
    )

    private val captureInfoBar = ClientBossBar(
        UUID.randomUUID(),
        Text.of(""),
        1.0f,
        BossBar.Color.PINK,
        BossBar.Style.NOTCHED_10,
        false,
        false,
        false
    )

    private val bossBars: MutableMap<UUID, ClientBossBar> by lazy {
        (WorldTools.mc.inGameHud.bossBarHud as BossBarHudAccessor).bossBars
    }

    fun startSaving() {
        WorldTools.saving = true
        bossBars.putIfAbsent(progressBar.uuid, progressBar)
    }

    fun stopSaving() {
        WorldTools.saving = false
        progressBar.percent = 0.0f
        bossBars.remove(progressBar.uuid)
    }

    fun startCapture() {
        bossBars.putIfAbsent(captureInfoBar.uuid, captureInfoBar)
    }

    fun stopCapture() {
        captureInfoBar.percent = 0.0f
        bossBars.remove(captureInfoBar.uuid)
    }

    fun updateCapture() {
        startCapture()
        captureInfoBar.percent =
            max(cachedChunks.size / MAX_CACHE_SIZE.toFloat(), cachedEntities.size / MAX_CACHE_SIZE.toFloat())
        captureInfoBar.name = mm(
            "Captured <color:#FFA2C4>${
                cachedChunks.size
            }</color> chunks and <color:#FFA2C4>${
                cachedEntities.size
            }</color> entities."
        )
    }

    fun updateSaveChunk(percentage: Float, savedChunks: Int, totalChunks: Int, pos: ChunkPos, dimension: String) {
        progressBar.percent = percentage
        progressBar.name = mm(
            "${"%.2f".format(percentage * 100)}% - Saving chunk <color:#FFA2C4>$savedChunks</color>/<color:#FFA2C4>$totalChunks</color> at <color:#FFA2C4>$pos</color> in <color:#FFA2C4>$dimension</color>..."
        )
    }

    fun updateSaveEntity(percentage: Float, savedEntities: Int, totalEntitiesSaved: Int, entity: Entity) {
        progressBar.percent = percentage
        progressBar.name = mm(
            "${"%.2f".format(percentage * 100)}% - Saving <color:#FFA2C4>${entity.name.string}</color> (<color:#FFA2C4>$savedEntities</color>/<color:#FFA2C4>$totalEntitiesSaved</color>) at <color:#FFA2C4>${entity.blockPos.toShortString()}</color>..."
        )
    }
}