package org.waste.of.time

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.GridWidget
import net.minecraft.component.type.MapIdComponent
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World
import net.minecraft.world.chunk.WorldChunk
import org.waste.of.time.Utils.manhattanDistance2d
import org.waste.of.time.WorldTools.CAPTURE_KEY
import org.waste.of.time.WorldTools.CONFIG_KEY
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.gui.ManagerScreen
import org.waste.of.time.manager.BarManager.updateCapture
import org.waste.of.time.manager.CaptureManager
import org.waste.of.time.manager.CaptureManager.capturing
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.storage.StorageFlow
import org.waste.of.time.storage.cache.EntityCacheable
import org.waste.of.time.storage.cache.HotCache
import org.waste.of.time.storage.cache.DataInjectionHandler
import org.waste.of.time.storage.serializable.BlockEntityLoadable
import org.waste.of.time.storage.serializable.PlayerStoreable
import org.waste.of.time.storage.serializable.RegionBasedChunk

object Events {
    fun onChunkLoad(chunk: WorldChunk) {
        if (!capturing) return
        RegionBasedChunk(chunk).cache()
        BlockEntityLoadable(chunk).emit()
    }

    fun onChunkUnload(chunk: WorldChunk) {
        if (!capturing) return
        (HotCache.chunks[chunk.pos] ?: RegionBasedChunk(chunk)).apply {
            emit()
            flush()
        }
    }

    fun onEntityLoad(entity: Entity) {
        if (!capturing) return
        if (entity is PlayerEntity) {
            PlayerStoreable(entity).cache()
        } else {
            EntityCacheable(entity).cache()
        }
    }

    fun onEntityUnload(entity: Entity) {
        if (!capturing) return
        if (entity !is PlayerEntity) return
        PlayerStoreable(entity).apply {
            emit()
            flush()
        }
    }

    fun onClientTickStart() {
        if (CAPTURE_KEY.wasPressed() && mc.world != null && mc.currentScreen == null) {
            CaptureManager.toggleCapture()
        }

        if (CONFIG_KEY.wasPressed() && mc.world != null && mc.currentScreen == null) {
            mc.setScreen(ManagerScreen)
        }

        if (!capturing) return
        updateCapture()
    }

    fun onClientJoin() {
        HotCache.clear()
        StorageFlow.lastStored = null
        StatisticManager.reset()
        if (config.general.autoDownload) CaptureManager.start()
    }

    fun onClientDisconnect() {
        if (!capturing) return
        CaptureManager.stop()
    }

    fun onInteractBlock(world: World, hitResult: BlockHitResult) {
        if (!capturing) return
        val blockEntity = world.getBlockEntity(hitResult.blockPos)
        HotCache.lastInteractedBlockEntity = blockEntity
        HotCache.lastInteractedEntity = null
    }

    fun onInteractEntity(entity: Entity) {
        if (!capturing) return
        HotCache.lastInteractedEntity = entity
        HotCache.lastInteractedBlockEntity = null
    }

    fun onDebugRenderStart(
        cameraX: Double,
        cameraY: Double,
        cameraZ: Double
    ) {
        // Minecraft 1.21.11 changed DebugRenderer.render and no longer exposes
        // MatrixStack/VertexConsumerProvider here. Keep the hook compatible and
        // skip the optional debug overlay instead of crashing during mixin apply.
    }

    fun onGameMenuScreenInitWidgets(adder: GridWidget.Adder) {
        val widget = if (capturing) {
            val label = translateHighlight("worldtools.gui.escape.button.finish_download", currentLevelName)
            ButtonWidget.builder(label) {
                CaptureManager.stop()
                mc.setScreen(null)
            }.width(204).build()
        } else {
            ButtonWidget.builder(MessageManager.brand) {
                MinecraftClient.getInstance().setScreen(ManagerScreen)
            }.width(204).build()
        }

        adder.add(widget, 2)
    }

    fun onScreenRemoved(screen: Screen) {
        if (!capturing) return
        DataInjectionHandler.onScreenRemoved(screen)
        HotCache.lastInteractedBlockEntity = null
    }

    fun onEntityRemoved(entity: Entity, reason: Entity.RemovalReason) {
        if (!capturing) return
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) return

        if (entity is LivingEntity) {
            if (!entity.isDead) return

            val cacheable = EntityCacheable(entity)
            HotCache.entities.entries.find { (_, entities) ->
                entities.contains(cacheable)
            }?.value?.remove(cacheable)
        } else {
            // todo: its actually a bit tricky to differentiate the entity being removed from our world or the server world
            //  need to find a reliable way to determine it
            //  if chunk is loaded, remove the entity? -> doesn't seem to work because server will remove entity before chunk is unloaded
            mc.player?.let { player ->
                if (entity.getEntityPos().manhattanDistance2d(player.getEntityPos()) < 32) { // todo: configurable distance, this should be small enough to be safe for most cases
                    val cacheable = EntityCacheable(entity)
                    HotCache.entities[entity.getChunkPos()]?.remove(cacheable)
                }
            }
        }
    }

    fun onMapStateGet(id: MapIdComponent) {
        if (!capturing) return
        // todo: looks like the server does not send a map update packet for container
        HotCache.mapIDs.add(id.id)
    }
}
