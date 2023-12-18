package org.waste.of.time.event

import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.GridWidget
import net.minecraft.client.render.Camera
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
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
import org.waste.of.time.event.serializable.EntityCacheable
import org.waste.of.time.event.serializable.PlayerStoreable
import org.waste.of.time.event.serializable.RegionBasedChunk
import org.waste.of.time.gui.WorldToolsScreen
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
//            mc.setScreen(WorldToolsScreen)
            WorldTools.toggleCapture()
//            mc.toastManager.add(WorldToolsScreen.CAPTURE_TOAST)
        }

        updateCapture()
    }

    fun onClientJoin() {
        HotCache.clear()
        StorageFlow.lastStored = null
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

    fun onBlockOutline(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, camera: Camera) {
        if (!caching) return

        val world = mc.world ?: return
        val vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines()) ?: return

        HotCache.chunks.values
            .flatMap { it.chunk.blockEntities.values }
            .filter { it !in HotCache.blockEntities }
            .forEach { blockEntity ->
                val blockPos = blockEntity.pos
                val blockState = blockEntity.cachedState
                val color = Color(222, 0, 0, 100)

                val voxelShape = blockState.getOutlineShape(world, blockPos)
                val offsetShape = voxelShape.offset(
                    blockPos.x.toDouble(),
                    blockPos.y.toDouble(),
                    blockPos.z.toDouble()
                )

                WorldRenderer.drawShapeOutline(
                    matrices,
                    vertexConsumer,
                    offsetShape,
                    -camera.pos.x,
                    -camera.pos.y,
                    -camera.pos.z,
                    color.red / 255.0f,
                    color.green / 255.0f,
                    color.blue / 255.0f,
                    1.0f,
                    false,
                )
            }
    }

    fun onGameMenuScreenInitWidgets(adder: GridWidget.Adder) {
        if (WorldTools.caching) {
            adder.add(
                ButtonWidget.builder(
                    Text.of("Save WorldTools Capture")) {
                        WorldTools.stopCapture()
                        mc.setScreen(null)
                    }
                    .width(204)
                    .build(),
                2
            )
        } else {
            adder.add(
                ButtonWidget.builder(
                    Text.of("WorldTools")) { MinecraftClient.getInstance().setScreen(WorldToolsScreen) }
                    .width(204)
                    .build(),
                2
            )
        }
    }
}
