package org.waste.of.time.fabric

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import net.minecraft.util.ActionResult
import org.waste.of.time.WorldTools
import org.waste.of.time.event.Events
import org.waste.of.time.fabric.command.WorldToolsFabricCommandBuilder
import org.waste.of.time.renderer.MissingChestBlockEntityRenderer

object WorldToolsFabric : ClientModInitializer {
    override fun onInitializeClient() {
        WorldTools.initialize()
        KeyBindingHelper.registerKeyBinding(WorldTools.GUI_KEY)
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            WorldToolsFabricCommandBuilder.register(dispatcher)
        })
        ClientPlayConnectionEvents.JOIN.register(ClientPlayConnectionEvents.Join { handler, sender, mc ->
            Events.onClientJoin()
        })
        UseBlockCallback.EVENT.register(UseBlockCallback { player, world, hand, hitResult ->
            Events.onInteractBlock(player, world, hand, hitResult)
            ActionResult.PASS
        })
        BlockEntityRendererFactories.register(BlockEntityType.CHEST) {
            MissingChestBlockEntityRenderer(it)
        }
        WorldTools.LOGGER.info("WorldTools Fabric initialized")
    }
}
