package org.waste.of.time.renderer

import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.event.HotCache
import java.awt.Color

class MissingChestBlockEntityRenderer(
    ctx: BlockEntityRendererFactory.Context
) : ChestBlockEntityRenderer<ChestBlockEntity>(ctx) {
    override fun render(
        entity: ChestBlockEntity?,
        tickDelta: Float,
        matrices: MatrixStack?,
        vertexConsumers: VertexConsumerProvider?,
        light: Int,
        overlay: Int
    ) {
        super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay)

        if (HotCache.blockEntities.contains(entity)) {
            return
        }

        val vertexConsumer = vertexConsumers?.getBuffer(RenderLayer.getLines()) ?: return
        val camera = mc.gameRenderer.camera
        val world = mc.world ?: return
        val blockPos = entity?.pos ?: return
        val blockState = entity.cachedState
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