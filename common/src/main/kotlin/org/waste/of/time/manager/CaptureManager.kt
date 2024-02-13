package org.waste.of.time.manager

import kotlinx.coroutines.Job
import net.minecraft.client.gui.screen.ConfirmScreen
import net.minecraft.client.network.ServerInfo
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket
import net.minecraft.text.Text
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.config.WorldToolsConfig
import org.waste.of.time.storage.StorageFlow
import org.waste.of.time.storage.cache.HotCache
import org.waste.of.time.storage.serializable.*

object CaptureManager {
    private const val MAX_WORLD_NAME_LENGTH = 64
    var capturing = false
    private var storeJob: Job? = null
    var currentLevelName: String = "Not yet initialized"
    val serverInfo: ServerInfo
        get() = mc.networkHandler?.serverInfo ?: throw IllegalStateException("Server info should not be null")

    val levelName: String
        get() = if (mc.isInSingleplayer) {
            mc.server?.serverMotd?.substringAfter(" - ")?.sanitizeWorldName() ?: "Singleplayer"
        } else {
            mc.networkHandler?.serverInfo?.address?.sanitizeWorldName() ?: "Multiplayer"
        }

    fun toggleCapture() {
        if (capturing) stop() else start()
    }

    fun start(customName: String? = null, confirmed: Boolean = false) {
        if (capturing) {
            MessageManager.sendError("worldtools.log.error.already_capturing", currentLevelName)
            return
        }

        if (mc.isInSingleplayer) {
            MessageManager.sendInfo("worldtools.log.info.singleplayer_capture")
        }

        val potentialName = customName?.let { potentialName ->
            if (potentialName.length > MAX_WORLD_NAME_LENGTH) {
                MessageManager.sendError(
                    "worldtools.log.error.world_name_too_long",
                    potentialName,
                    MAX_WORLD_NAME_LENGTH
                )
                return
            }

            potentialName.ifBlank {
                levelName
            }
        } ?: levelName

        if (mc.levelStorage.savesDirectory.resolve(potentialName).toFile().exists() && !confirmed) {
            mc.setScreen(ConfirmScreen(
                { yes ->
                    if (yes) start(potentialName, true)
                    mc.setScreen(null)
                },
                Text.translatable("worldtools.gui.capture.existing_world_confirm.title"),
                Text.translatable("worldtools.gui.capture.existing_world_confirm.message", potentialName)
            ))
            return
        }

        HotCache.clear()
        currentLevelName = potentialName
        MessageManager.sendInfo("worldtools.log.info.started_capture", potentialName)
        if (config.debug.logSettings) logCaptureSettingsState()
        storeJob = StorageFlow.launch(potentialName)
        mc.networkHandler?.sendPacket(ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS))
        capturing = true
    }

    private fun logCaptureSettingsState() {
        WorldTools.GSON.toJson(config, WorldToolsConfig::class.java).let { configJson ->
            LOG.info("Launching Capture With Settings:")
            LOG.info(configJson)
        }
    }

    fun stop() {
        if (!capturing) {
            MessageManager.sendError("worldtools.log.error.not_capturing")
            return
        }

        MessageManager.sendInfo("worldtools.log.info.stopping_capture", currentLevelName)

        // update the stats and trigger writeStats() in StatisticSerializer
        // todo: useless because this is async and response will probably be received after the flow ends
//        mc.networkHandler?.sendPacket(ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS))

        HotCache.chunks.values.forEach { chunk ->
            chunk.emit() // will also write entities in the chunks
        }

        HotCache.players.forEach { player ->
            player.emit()
        }

        MapDataStoreable().emit()
        LevelDataStoreable().emit()
        AdvancementsStoreable().emit()
        MetadataStoreable().emit()
        CompressLevelStoreable().emit()
        EndFlow().emit()
    }

    private fun String.sanitizeWorldName() = replace(":", "_")
}
