package org.waste.of.time.event

import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World
import net.minecraft.world.chunk.WorldChunk
import org.waste.of.time.BarManager.updateCapture
import org.waste.of.time.StatisticManager
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.CAPTURE_KEY
import org.waste.of.time.WorldTools.caching
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.WorldTools.stopCapture
import org.waste.of.time.event.serializable.RegionBasedChunk
import org.waste.of.time.event.serializable.EntityCacheable
import org.waste.of.time.event.serializable.PlayerStoreable

object Events {
    fun onChunkLoad(chunk: WorldChunk) {
        RegionBasedChunk(chunk).cache()
    }

    fun onChunkUnload(chunk: WorldChunk) {
        val chunkSerializable = RegionBasedChunk(chunk)
        chunkSerializable.emit()
        chunkSerializable.flush()
    }

    fun onEntityLoad(entity: Entity) {
        if (entity is PlayerEntity) {
            PlayerStoreable(entity).cache()
        } else {
            EntityCacheable(entity).cache()
        }
    }

    fun onEntityUnload(entity: Entity) {
        if (entity is PlayerEntity) {
            val playerStoreable = PlayerStoreable(entity)
            playerStoreable.emit()
            playerStoreable.flush()
        }
    }

    fun onClientTickStart() {
        if (CAPTURE_KEY.wasPressed() && mc.world != null && mc.currentScreen == null) {
//            mc.setScreen(WorldToolsScreen)
            WorldTools.toggleCapture()
//            mc.toastManager.add(WorldToolsScreen.CAPTURE_TOAST)
        }

        updateCapture()
    }

    fun onClientJoin() {
        HotCache.clear()
        StorageFlow.currentStoreable = null
        StatisticManager.reset()
    }

    fun onClientDisconnect() {
        if (!caching) return

        stopCapture()
    }

    fun onInteractBlock(world: World, hitResult: BlockHitResult) {
        if (!caching) return

        val blockEntity = (world.getBlockEntity(hitResult.blockPos) as? ChestBlockEntity) ?: return
        HotCache.lastOpenedContainer = blockEntity
    }
}
