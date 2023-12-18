package org.waste.of.time.fabric

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import net.minecraft.util.ActionResult
import org.waste.of.time.WorldTools
import org.waste.of.time.event.Events
import org.waste.of.time.renderer.MissingChestBlockEntityRenderer

object WorldToolsFabric : ClientModInitializer {
    override fun onInitializeClient() {
        WorldTools.initialize()

        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            dispatcher.register()
        })
        KeyBindingHelper.registerKeyBinding(WorldTools.CAPTURE_KEY)
        ClientEntityEvents.ENTITY_LOAD.register(ClientEntityEvents.Load { entity, _ ->
            Events.onEntityLoad(entity)
        })
        ClientEntityEvents.ENTITY_UNLOAD.register(ClientEntityEvents.Unload { entity, _ ->
            Events.onEntityUnload(entity)
        })
        ClientPlayConnectionEvents.JOIN.register(ClientPlayConnectionEvents.Join { _, _, _ ->
            Events.onClientJoin()
        })
        ClientPlayConnectionEvents.DISCONNECT.register(ClientPlayConnectionEvents.Disconnect { _, _ ->
            Events.onClientDisconnect()
        })
        UseBlockCallback.EVENT.register(UseBlockCallback { _, world, _, hitResult ->
            Events.onInteractBlock(world, hitResult)
            ActionResult.PASS
        })
        BlockEntityRendererFactories.register(BlockEntityType.CHEST) {
            MissingChestBlockEntityRenderer(it)
        }

        WorldTools.LOG.info("WorldTools Fabric initialized")
    }

    private fun CommandDispatcher<FabricClientCommandSource>.register() {
        register(
            ClientCommandManager.literal("worldtools").executes {
                WorldTools.toggleCapture()
                0
            }
        )
    }
}
