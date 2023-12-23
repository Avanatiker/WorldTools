package org.waste.of.time.forge

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.RegisterClientCommandsEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.entity.EntityLeaveLevelEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.Mod
import org.waste.of.time.Events
import org.waste.of.time.manager.CaptureManager
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.LOG
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

@Mod(WorldTools.MOD_ID)
object WorldToolsForge {
    init {
        WorldTools.initialize()
        FORGE_BUS.addListener<RegisterKeyMappingsEvent> {
            it.register(WorldTools.CAPTURE_KEY)
            it.register(WorldTools.CONFIG_KEY)
        }
        FORGE_BUS.addListener<RegisterClientCommandsEvent> {
            it.dispatcher.register()
        }
        FORGE_BUS.addListener<ClientPlayerNetworkEvent.LoggingIn> {
            Events.onClientJoin()
        }
        FORGE_BUS.addListener<ClientPlayerNetworkEvent.LoggingOut> {
            Events.onClientDisconnect()
        }
        FORGE_BUS.addListener<PlayerInteractEvent.RightClickBlock> {
            Events.onInteractBlock(it.level, it.hitVec)
        }
        FORGE_BUS.addListener<EntityJoinLevelEvent> {
            Events.onEntityLoad(it.entity)
        }
        FORGE_BUS.addListener<EntityLeaveLevelEvent> {
            Events.onEntityUnload(it.entity)
        }

        LOG.info("WorldTools Forge initialized")
    }

    private fun CommandDispatcher<ServerCommandSource>.register() {
        register(
            literal("worldtools")
                .then(literal("capture")
                    .then(argument("name", StringArgumentType.string()).executes {
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
