package org.waste.of.time.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.RegisterClientCommandsEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.Mod
import org.waste.of.time.WorldTools
import org.waste.of.time.event.Events
import org.waste.of.time.renderer.MissingChestBlockEntityRenderer
import org.waste.of.time.storage.StorageManager
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

@Mod(WorldTools.MOD_ID)
object WorldToolsForge {
    init {
        WorldTools.initialize()
        FORGE_BUS.addListener<RegisterClientCommandsEvent> {
            it.dispatcher.register()
        }
        FORGE_BUS.addListener<RegisterKeyMappingsEvent> {
            it.register(WorldTools.GUI_KEY)
        }
        FORGE_BUS.addListener<ClientPlayerNetworkEvent.LoggingIn> {
            Events.onClientJoin()
        }
        FORGE_BUS.addListener<PlayerInteractEvent.RightClickBlock> {
            Events.onInteractBlock(it.level, it.hitVec)
        }
        FORGE_BUS.addListener<EntityJoinLevelEvent> {
            Events.onEntityLoad(it.entity)
        }
        FORGE_BUS.addListener<ScreenEvent.Init.Post> {
            Events.onAfterInitScreen(it.screen)
        }
        BlockEntityRendererFactories.register(BlockEntityType.CHEST) {
            MissingChestBlockEntityRenderer(it)
        }

        WorldTools.LOGGER.info("WorldTools Forge initialized")
    }

    private fun CommandDispatcher<ServerCommandSource>.register() {
        register(
            CommandManager.literal("worldtools")
                .then(CommandManager.literal("capture")
                    .then(CommandManager.literal("start").executes {
                        WorldTools.startCapture()
                        0
                    }
                    ).then(CommandManager.literal("stop").executes {
                        WorldTools.stopCapture()
                        0
                    }
                    )
                    .then(CommandManager.literal("flush").executes {
                        WorldTools.flush()
                        0
                    }
                    )
                )
                .then(CommandManager.literal("save")
                    .executes {
                        StorageManager.save()
                    }
                    .then(CommandManager.argument("freezeEntities", BoolArgumentType.bool()).executes {
                        StorageManager.save(BoolArgumentType.getBool(it, "freezeEntities"))
                    })
                )
        )
    }
}