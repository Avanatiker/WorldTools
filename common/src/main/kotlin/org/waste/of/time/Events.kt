package org.waste.of.time

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.GridWidget
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.ChunkPos
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
import org.waste.of.time.storage.cache.LootableInjectionHandler
import org.waste.of.time.storage.serializable.BlockEntityLoadable
import org.waste.of.time.storage.serializable.PlayerStoreable
import org.waste.of.time.storage.serializable.RegionBasedChunk
import java.awt.Color

object Events {
    fun onChunkLoad(chunk: WorldChunk) {
        if (!capturing) return
        RegionBasedChunk(chunk).cache()
        BlockEntityLoadable(chunk).tryEmit()
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
    }

    fun onDebugRenderStart(
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider.Immediate,
        cameraX: Double,
        cameraY: Double,
        cameraZ: Double
    ) {
        if (!capturing || !config.render.renderNotYetCachedContainers) return

        val vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines()) ?: return

        HotCache.unscannedContainers
            .forEach { blockEntity ->
                val blockPos = blockEntity.pos
                val x1 = (blockPos.x - cameraX).toFloat()
                val y1 = (blockPos.y - cameraY).toFloat()
                val z1 = (blockPos.z - cameraZ).toFloat()
                val x2 = x1 + 1
                val z2 = z1 + 1
                val color = Color(config.render.containerColor)
                val r = color.red / 255.0f
                val g = color.green / 255.0f
                val b = color.blue / 255.0f
                val a = 1.0f
                val positionMat = matrices.peek().positionMatrix
                val normMat = matrices.peek().normalMatrix
                vertexConsumer.vertex(positionMat, x1, y1, z1).color(r, g, b, a).normal(normMat, 1.0f, 0.0f, 0.0f).next()
                vertexConsumer.vertex(positionMat, x2, y1, z1).color(r, g, b, a).normal(normMat, 1.0f, 0.0f, 0.0f).next()
                vertexConsumer.vertex(positionMat, x1, y1, z1).color(r, g, b, a).normal(normMat, 0.0f, 0.0f, 1.0f).next()
                vertexConsumer.vertex(positionMat, x1, y1, z2).color(r, g, b, a).normal(normMat, 0.0f, 0.0f, 1.0f).next()
                vertexConsumer.vertex(positionMat, x1, y1, z2).color(r, g, b, a).normal(normMat, 1.0f, 0.0f, 0.0f).next()
                vertexConsumer.vertex(positionMat, x2, y1, z2).color(r, g, b, a).normal(normMat, 1.0f, 0.0f, 0.0f).next()
                vertexConsumer.vertex(positionMat, x2, y1, z2).color(r, g, b, a).normal(normMat, 0.0f, 0.0f, -1.0f).next()
                vertexConsumer.vertex(positionMat, x2, y1, z1).color(r, g, b, a).normal(normMat, 0.0f, 0.0f, -1.0f).next()
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
        if (!capturing) return
        LootableInjectionHandler.onScreenRemoved(screen)
        HotCache.lastInteractedBlockEntity = null
    }

    fun onEntityRemoved(entity: Entity, reason: Entity.RemovalReason) {
        if (!capturing) return
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
            if (entity is LivingEntity) {
                if (entity.isDead) {
                    val cacheable = EntityCacheable(entity)
                    // not particularly efficient, but fine
                    var foundEntry: MutableMap.MutableEntry<ChunkPos, MutableSet<EntityCacheable>>? = null
                    for (entry in HotCache.entities) {
                        if (entry.value.contains(cacheable)) {
                            foundEntry = entry
                            break
                        }
                    }
                    foundEntry?.value?.remove(cacheable)
                }
            } else {
                // todo: its actually a bit tricky to differentiate the entity being removed from our world or the server world
                //  need to find a reliable way to determine it
                //  if chunk is loaded, remove the entity? -> doesn't seem to work because server will remove entity before chunk is unloaded
                mc.player?.let { player ->
                    if (entity.pos.manhattanDistance2d(player.pos) < 32) { // todo: configurable distance, this should be small enough to be safe for most cases
                        val cacheable = EntityCacheable(entity)
                        HotCache.entities[entity.chunkPos]?.remove(cacheable)
                    }
                }
            }
        }
    }

    fun onMapStateGet(id: String) {
        if (!capturing) return
        // todo: check if this also populates maps when we open a container that contains a map but don't hold it
        HotCache.mapIDs.add(id)
    }
}
