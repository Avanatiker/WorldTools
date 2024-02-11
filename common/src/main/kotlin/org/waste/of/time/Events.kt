package org.waste.of.time

import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.GridWidget
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World
import net.minecraft.world.chunk.WorldChunk
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
import org.waste.of.time.storage.cache.LootableInjectionHandler
import org.waste.of.time.storage.serializable.PlayerStoreable
import org.waste.of.time.storage.serializable.RegionBasedChunk
import java.awt.Color

object Events {
    fun onChunkLoad(chunk: WorldChunk) {
        if (!CaptureManager.capturing) return
        RegionBasedChunk(chunk).cache()
    }

    fun onChunkUnload(chunk: WorldChunk) {
        if (!CaptureManager.capturing) return
        val regionBasedChunk = HotCache.chunks[chunk.pos] ?: RegionBasedChunk(chunk)
        regionBasedChunk.apply {
            cacheBlockEntities()
            emit()
            flush()
        }
    }

    fun onEntityLoad(entity: Entity) {
        if (!CaptureManager.capturing) return
        if (entity is PlayerEntity) {
            PlayerStoreable(entity).cache()
        } else {
            EntityCacheable(entity).cache()
        }
    }

    fun onEntityUnload(entity: Entity) {
        if (!CaptureManager.capturing) return
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

        updateCapture()
    }

    fun onClientJoin() {
        HotCache.clear()
        StorageFlow.lastStored = null
        StatisticManager.reset()
        if (config.capture.autoDownload) CaptureManager.start()
    }

    fun onClientDisconnect() {
        if (capturing) CaptureManager.stop()
    }

    fun onInteractBlock(world: World, hitResult: BlockHitResult) {
        if (!capturing) return

        val blockEntity = (world.getBlockEntity(hitResult.blockPos) as? LockableContainerBlockEntity) ?: return
        HotCache.lastOpenedContainer = blockEntity
    }

    fun onDebugRenderStart(
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider.Immediate,
        cameraX: Double,
        cameraY: Double,
        cameraZ: Double
    ) {
        if (!capturing || !config.capture.renderNotYetCachedContainers) return

        val world = mc.world ?: return
        val vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines()) ?: return

        HotCache.cachedMissingContainers
            .forEach { blockEntity ->
                val blockPos = blockEntity.pos
                val blockState = blockEntity.cachedState
                val color = Color(config.capture.containerColor)

                val voxelShape = blockState.getCollisionShape(world, blockPos)
                val offsetShape = voxelShape.offset(
                    blockPos.x.toDouble(),
                    blockPos.y.toDouble(),
                    blockPos.z.toDouble()
                )

                WorldRenderer.drawShapeOutline(
                    matrices,
                    vertexConsumer,
                    offsetShape,
                    -cameraX,
                    -cameraY,
                    -cameraZ,
                    color.red / 255.0f,
                    color.green / 255.0f,
                    color.blue / 255.0f,
                    1.0f,
                    false,
                )
            }
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
        LootableInjectionHandler.onScreenRemoved(screen)
    }

    fun onEntityRemoved(entity: Entity, reason: Entity.RemovalReason) {
        if (!capturing) return
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
            if (entity is LivingEntity) {
                if (entity.isDead) {
                    val cacheable = EntityCacheable(entity)
                    // not particularly efficient, but fine
                    HotCache.entities.values.forEach { it.remove(cacheable) }
                }
            } else {
                // todo: its actually a bit tricky to differentiate the entity being removed from our world or the server world
                //  need to find a reliable way to determine it
                //  check if the entity is within a set distance from us?
                //  if chunk is loaded, remove the entity? -> doesn't seem to work because server will remove entity before chunk is unloaded
            }
        }
    }
}
