package org.waste.of.time.fabric

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.string
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.util.ActionResult
import org.waste.of.time.Events
import org.waste.of.time.LoaderData
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.manager.CaptureManager

object WorldToolsFabric : ClientModInitializer {
    override fun onInitializeClient() {
        LoaderData.loader = WTFabricLoader()
        WorldTools.initialize()

        KeyBindingHelper.registerKeyBinding(WorldTools.CAPTURE_KEY)
        KeyBindingHelper.registerKeyBinding(WorldTools.CONFIG_KEY)

        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            dispatcher.register()
        })
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
        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvents.StartTick {
            Events.onClientTickStart()
        })

        LOG.info("WorldTools Fabric initialized")
    }

    private fun CommandDispatcher<FabricClientCommandSource>.register() {
        register(
            literal("worldtools")
                .then(literal("capture")
                    .then(argument("name", string()).executes {
                        CaptureManager.start(it.getArgument("name", String::class.java))
                        0
                    })
                    .then(literal("start").executes {
                        CaptureManager.start()
                        0
                    })
                    .then(literal("stop").executes {
                        CaptureManager.stop()
                        0
                    })
                    .executes {
                        CaptureManager.toggleCapture()
                        0
                    }
                )
        )
    }
}
