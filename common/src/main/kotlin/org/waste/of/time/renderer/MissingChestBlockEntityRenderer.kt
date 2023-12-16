package org.waste.of.time.renderer

import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer

class MissingChestBlockEntityRenderer(
    ctx: BlockEntityRendererFactory.Context
) : ChestBlockEntityRenderer<ChestBlockEntity>(ctx)