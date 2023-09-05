package org.waste.of.time.command

import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
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
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

@Mod(WorldTools.MOD_ID)
object WorldToolsForge {
    init {
        WorldTools.initialize()
        FORGE_BUS.addListener<RegisterClientCommandsEvent> {
            WorldToolsForgeCommandBuilder.register(it.dispatcher)
        }
        FORGE_BUS.addListener<RegisterKeyMappingsEvent> {
            it.register(WorldTools.GUI_KEY)
        }
        FORGE_BUS.addListener<ClientPlayerNetworkEvent.LoggingIn> {
            Events.onClientJoin()
        }
        FORGE_BUS.addListener<PlayerInteractEvent.RightClickBlock> {
            Events.onInteractBlock(it.entity, it.level, it.hand, it.hitVec)
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
    }
}