package org.waste.of.time.event

import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.GridWidget
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World
import net.minecraft.world.chunk.WorldChunk
import org.waste.of.time.BarManager.updateCapture
import org.waste.of.time.CaptureManager
import org.waste.of.time.CaptureManager.capturing
import org.waste.of.time.CaptureManager.currentLevelName
import org.waste.of.time.MessageManager
import org.waste.of.time.StatisticManager
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.CAPTURE_KEY
import org.waste.of.time.WorldTools.CONFIG_KEY
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.WorldTools.mm
import org.waste.of.time.event.serializable.EntityCacheable
import org.waste.of.time.event.serializable.PlayerStoreable
import org.waste.of.time.event.serializable.RegionBasedChunk
import org.waste.of.time.gui.ManagerScreen
import java.awt.Color

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
    }

    fun onClientDisconnect() {
        CaptureManager.stop()
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
        if (!capturing || !config.advanced.renderNotYetCachedContainers) return

        val world = mc.world ?: return
        val vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines()) ?: return

        HotCache.chunks.values
            .flatMap { it.chunk.blockEntities.values }
            .filterIsInstance<LockableContainerBlockEntity>()
            .filter { it !in HotCache.blockEntities }
            .forEach { blockEntity ->
                val blockPos = blockEntity.pos
                val blockState = blockEntity.cachedState
                val color = Color(222, 0, 0, 100)

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
            val label = "<lang:worldtools.gui.escape.button.finish_download:$currentLevelName>".mm()
            ButtonWidget.builder(label) {
                CaptureManager.stop()
                mc.setScreen(null)
            }.width(204).build()
        } else {
            ButtonWidget.builder(MessageManager.BRAND.mm()) {
                MinecraftClient.getInstance().setScreen(ManagerScreen)
            }.width(204).build()
        }

        adder.add(widget, 2)
    }
}
