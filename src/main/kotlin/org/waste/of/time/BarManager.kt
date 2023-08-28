package org.waste.of.time

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.platform.fabric.FabricClientAudiences
import net.kyori.adventure.text.Component.text
import net.minecraft.entity.Entity
import net.minecraft.util.math.ChunkPos
import org.waste.of.time.WorldTools.MAX_CACHE_SIZE
import org.waste.of.time.WorldTools.cachedBlockEntities
import org.waste.of.time.WorldTools.cachedChunks
import org.waste.of.time.WorldTools.cachedEntities
import org.waste.of.time.WorldTools.mm

object BarManager {
    private val selfAudience = FabricClientAudiences.of()

    private val progressBar: BossBar =
        BossBar.bossBar(text(""), 0.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)

    private val captureInfoBar: BossBar =
        BossBar.bossBar(text(""), 1.0f, BossBar.Color.PINK, BossBar.Overlay.NOTCHED_10)

    fun startSaving() {
        WorldTools.saving = true
        selfAudience.audience().showBossBar(progressBar)
    }

    fun stopSaving() {
        WorldTools.saving = false
        selfAudience.audience().hideBossBar(progressBar)
        progressBar.progress(0.0f)
    }

    fun startCapture() {
        selfAudience.audience().showBossBar(captureInfoBar)
    }

    fun stopCapture() {
        selfAudience.audience().hideBossBar(captureInfoBar)
        captureInfoBar.progress(0.0f)
    }

    fun updateCapture() {
        val cacheFilled = (cachedChunks.size + cachedEntities.size) / MAX_CACHE_SIZE.toFloat()
        startCapture()
        captureInfoBar.progress(cacheFilled.coerceIn(.0f, 1.0f))
        captureInfoBar.name(mm(
            "Captured <color:#FFA2C4>${
                cachedChunks.size
            }</color> chunks and <color:#FFA2C4>${
                cachedEntities.size
            }</color> entities and <color:#FFA2C4>${
                cachedBlockEntities.size
            }</color> chests."
        ))
    }

    fun updateSaveChunk(percentage: Float, savedChunks: Int, totalChunks: Int, pos: ChunkPos, dimension: String) {
        progressBar.progress(percentage.coerceIn(.0f, 1.0f))
        progressBar.name(mm(
            "${"%.2f".format(percentage * 100)}% - Saving chunk <color:#FFA2C4>$savedChunks</color>/<color:#FFA2C4>$totalChunks</color> at <color:#FFA2C4>$pos</color> in <color:#FFA2C4>$dimension</color>..."
        ))
    }

    fun updateSaveEntity(percentage: Float, savedEntities: Int, totalEntitiesSaved: Int, entity: Entity) {
        progressBar.progress(percentage.coerceIn(.0f, 1.0f))
        progressBar.name(mm(
            "${"%.2f".format(percentage * 100)}% - Saving <color:#FFA2C4>${sanitizeName(entity.name.string)}</color> (<color:#FFA2C4>$savedEntities</color>/<color:#FFA2C4>$totalEntitiesSaved</color>) at <color:#FFA2C4>${entity.blockPos.toShortString()}</color>..."
        ))
    }

    /**
     * Certain resource packs inject legacy formatting codes into entity names.
     * These codes will cause the bar to throw exceptions if present in the content.
     */
    private fun sanitizeName(name: String) = name.replace(Regex("§"), "")
}
