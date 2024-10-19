package org.waste.of.time.manager

import kotlinx.coroutines.*
import net.minecraft.client.gui.screen.ConfirmScreen
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.Text
import net.minecraft.world.World
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.config.WorldToolsConfig
import org.waste.of.time.manager.MessageManager.info
import org.waste.of.time.storage.StorageFlow
import org.waste.of.time.storage.cache.EntityCacheable
import org.waste.of.time.storage.cache.HotCache
import org.waste.of.time.storage.serializable.*

object CaptureManager {
    private const val MAX_WORLD_NAME_LENGTH = 64
    var capturing = false
    private var storeJob: Job? = null
    var currentLevelName: String = "Not yet initialized"
    var lastPlayer: ClientPlayerEntity? = null
    var lastWorldKeys = mutableSetOf<RegistryKey<World>>()

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

            potentialName.ifBlank { levelName }
        } ?: levelName

        val worldExists = mc.levelStorage.savesDirectory.resolve(potentialName).toFile().exists()
        if (worldExists && !confirmed) {
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
        lastPlayer = mc.player
        lastWorldKeys.addAll(mc.networkHandler?.worldKeys ?: emptySet())
        MessageManager.sendInfo("worldtools.log.info.started_capture", potentialName)
        if (config.debug.logSettings) logCaptureSettingsState()
        storeJob = StorageFlow.launch(potentialName)
        mc.networkHandler?.sendPacket(ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS))
        capturing = true

        // Need to wait until the storage flow is running before syncing the cache
        CoroutineScope(Dispatchers.IO).launch {
            delay(100L)
            mc.execute {
                syncCacheFromWorldState()
            }
        }
    }

    private fun logCaptureSettingsState() {
        WorldTools.GSON.toJson(config, WorldToolsConfig::class.java).let { configJson ->
            LOG.info("Launching capture with settings:")
            LOG.info(configJson)
        }
    }

    fun stop() {
        if (!capturing) {
            MessageManager.sendError("worldtools.log.error.not_capturing")
            return
        }

        MessageManager.sendInfo("worldtools.log.info.stopping_capture", currentLevelName)

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

    private fun syncCacheFromWorldState() {
        val world = mc.world ?: return
        val diameter = world.chunkManager.chunks.diameter

        repeat(diameter * diameter) { i ->
            world.chunkManager.chunks.getChunk(i)?.let { chunk ->
                RegionBasedChunk(chunk).cache()
                BlockEntityLoadable(chunk).emit()
            }
        }

        world.entities.forEach {
            if (it is PlayerEntity) {
                PlayerStoreable(it).cache()
            } else {
                EntityCacheable(it).cache()
            }
        }
    }

    private fun String.sanitizeWorldName() = replace(":", "_")
}
