package org.waste.of.time.fabric

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
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

        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            WorldToolsFabricCommandBuilder.register(dispatcher)
        })
        KeyBindingHelper.registerKeyBinding(WorldTools.GUI_KEY)
        ClientEntityEvents.ENTITY_LOAD.register(ClientEntityEvents.Load { entity, _ ->
            Events.onEntityLoad(entity)
        })

        ClientPlayConnectionEvents.JOIN.register(ClientPlayConnectionEvents.Join { _, _, _ ->
            Events.onClientJoin()
        })
        UseBlockCallback.EVENT.register(UseBlockCallback { _, world, _, hitResult ->
            Events.onInteractBlock(world, hitResult)
            ActionResult.PASS
        })
        ScreenEvents.AFTER_INIT.register(ScreenEvents.AfterInit { _, screen, _, _ ->
            Events.onAfterInitScreen(screen)
        })
        BlockEntityRendererFactories.register(BlockEntityType.CHEST) {
            MissingChestBlockEntityRenderer(it)
        }

        WorldTools.LOGGER.info("WorldTools Fabric initialized")
    }
}
