package org.waste.of.time


import net.minecraft.client.gui.hud.ClientBossBar
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.BossBar
import net.minecraft.text.Text
import net.minecraft.util.math.ChunkPos
import org.waste.of.time.WorldTools.MAX_CACHE_SIZE
import org.waste.of.time.WorldTools.cachedBlockEntities
import org.waste.of.time.WorldTools.cachedChunks
import org.waste.of.time.WorldTools.cachedEntities
import org.waste.of.time.WorldTools.caching
import org.waste.of.time.WorldTools.mm
import org.waste.of.time.WorldTools.savingMutex
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

    fun getProgressBar() = if (!savingMutex.isLocked) Optional.empty() else Optional.of(progressBar)

    fun getCaptureBar() = if (!caching) Optional.empty() else Optional.of(captureInfoBar)

    fun resetProgressBar() {
        progressBar.percent = 0f
    }

    fun updateCapture() {
        val cacheFilled = (cachedChunks.size + cachedEntities.size) / MAX_CACHE_SIZE.toFloat()
        captureInfoBar.percent = cacheFilled.coerceIn(.0f, 1.0f)
        captureInfoBar.name = mm(
            "Captured <color:#FFA2C4>${
                cachedChunks.size
            }</color> chunks and <color:#FFA2C4>${
                cachedEntities.size
            }</color> entities and <color:#FFA2C4>${
                cachedBlockEntities.size
            }</color> chests."
        )
    }

    fun updateSaveChunk(percentage: Float, savedChunks: Int, totalChunks: Int, pos: ChunkPos, dimension: String) {
        progressBar.percent = percentage.coerceIn(.0f, 1.0f)
        progressBar.name = mm(
            "${"%.2f".format(percentage * 100)}% - Saving chunk <color:#FFA2C4>$savedChunks</color>/<color:#FFA2C4>$totalChunks</color> at <color:#FFA2C4>$pos</color> in <color:#FFA2C4>$dimension</color>..."
        )
    }

    fun updateSaveEntity(percentage: Float, savedEntities: Int, totalEntitiesSaved: Int, entity: Entity) {
        progressBar.percent = percentage.coerceIn(.0f, 1.0f)
        progressBar.name = mm(
            "${"%.2f".format(percentage * 100)}% - Saving <color:#FFA2C4>${sanitizeName(entity.name.string)}</color> (<color:#FFA2C4>$savedEntities</color>/<color:#FFA2C4>$totalEntitiesSaved</color>) at <color:#FFA2C4>${entity.blockPos.toShortString()}</color>..."
        )
    }

    /**
     * Certain resource packs inject legacy formatting codes into entity names.
     * These codes will cause the bar to throw exceptions if present in the content.
     */
    private fun sanitizeName(name: String) = name.replace(Regex("§"), "")
}
