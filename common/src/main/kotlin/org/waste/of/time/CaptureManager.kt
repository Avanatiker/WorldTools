package org.waste.of.time

import kotlinx.coroutines.Job
import net.minecraft.client.network.ServerInfo
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.event.HotCache
import org.waste.of.time.event.StorageFlow
import org.waste.of.time.event.serializable.MetadataStoreable

object CaptureManager {
    private const val MAX_WORLD_NAME_LENGTH = 64
    var capturing = false
    private var storeJob: Job? = null
    var currentLevelName: String = "Not yet initialized"
    val serverInfo: ServerInfo
        get() = mc.networkHandler?.serverInfo ?: throw IllegalStateException("Server info should not be null")

    val levelName: String
        get() = if (mc.isInSingleplayer) {
            mc.server?.name?.sanitizeWorldName() ?: "SinglePlayer"
        } else {
            serverInfo.address.sanitizeWorldName()
        }

    fun toggleCapture() {
        if (capturing) stop() else start()
    }

    fun start(customName: String? = null) {
        if (capturing) {
            MessageManager.sendError("worldtools.log.error.already_capturing", currentLevelName)
            return
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

        currentLevelName = potentialName
        // todo: validate if a world already exists with this name. we need user to decide whether to merge or replace
        MessageManager.sendInfo("worldtools.log.info.started_capture", potentialName)

        storeJob = StorageFlow.launch(potentialName)
        capturing = true
    }

    fun stop() {
        if (!capturing) {
            MessageManager.sendError("worldtools.log.error.not_capturing")
            return
        }

        MessageManager.sendInfo("worldtools.log.info.stopping_capture", currentLevelName)

        // update the stats and trigger writeStats() in StatisticSerializer
        mc.networkHandler?.sendPacket(ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS))

        HotCache.chunks.values.forEach { chunk ->
            chunk.emit()
        }

        HotCache.convertEntities().forEach { entity ->
            entity.emit()
        }

        HotCache.players.forEach { player ->
            player.emit()
        }

        MetadataStoreable().emit()

        capturing = false
    }

    private fun String.sanitizeWorldName() = replace(":", "_")
}