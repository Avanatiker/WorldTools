package org.waste.of.time.fabric

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
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
import org.waste.of.time.storage.StorageManager

object WorldToolsFabric : ClientModInitializer {
    override fun onInitializeClient() {
        WorldTools.initialize()

        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            dispatcher.register()
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
        BlockEntityRendererFactories.register(BlockEntityType.CHEST) {
            MissingChestBlockEntityRenderer(it)
        }

        WorldTools.LOGGER.info("WorldTools Fabric initialized")
    }

    private fun CommandDispatcher<FabricClientCommandSource>.register() {
        register(
            ClientCommandManager.literal("worldtools")
                .then(ClientCommandManager.literal("capture")
                    .then(ClientCommandManager.literal("start").executes {
                        WorldTools.startCapture()
                        0
                    }
                    ).then(ClientCommandManager.literal("stop").executes {
                        WorldTools.stopCapture()
                        0
                    }
                    )
                    .then(ClientCommandManager.literal("flush").executes {
                        WorldTools.flush()
                        0
                    }
                    )
                )
                .then(ClientCommandManager.literal("save")
                    .executes {
                        StorageManager.save()
                    }
                    .then(ClientCommandManager.argument("freezeEntities", BoolArgumentType.bool()).executes {
                        StorageManager.save(BoolArgumentType.getBool(it, "freezeEntities"))
                    })
                )
        )
    }
}
