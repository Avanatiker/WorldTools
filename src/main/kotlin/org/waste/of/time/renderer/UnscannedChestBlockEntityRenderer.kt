package org.waste.of.time.renderer

import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer
import net.minecraft.client.render.model.json.ModelTransformationMode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.math.RotationAxis
import org.waste.of.time.WorldTools.cachedBlockEntities
import org.waste.of.time.WorldTools.mc
import kotlin.math.sin

class UnscannedChestBlockEntityRenderer(
    ctx: BlockEntityRendererFactory.Context
) : ChestBlockEntityRenderer<ChestBlockEntity>(ctx) {
    private val stack = ItemStack(Items.CHEST, 1)

    override fun render(
        entity: ChestBlockEntity?,
        tickDelta: Float,
        matrices: MatrixStack?,
        vertexConsumers: VertexConsumerProvider?,
        light: Int,
        overlay: Int
    ) {
        super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay)
        val world = entity?.world ?: return

        if (!(entity !in cachedBlockEntities && !mc.isInSingleplayer && matrices != null)) {
            return
        }

        matrices.push()

        val offset = sin((world.time + tickDelta) / 8.0) / 4.0
        matrices.translate(0.5, 1.25 + offset, 0.5)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((world.time + tickDelta) * 4))

        mc.itemRenderer.renderItem(
            stack,
            ModelTransformationMode.GROUND,
            WorldRenderer.getLightmapCoordinates(entity.world, entity.pos.up()),
            overlay,
            matrices,
            vertexConsumers,
            entity.world,
            0
        )

        matrices.pop()
    }
}