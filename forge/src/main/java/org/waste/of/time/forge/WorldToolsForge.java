package org.waste.of.time.forge;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.waste.of.time.WorldTools;
import org.waste.of.time.command.WorldToolsForgeCommandBuilder;
import org.waste.of.time.event.Events;
import org.waste.of.time.renderer.MissingChestBlockEntityRenderer;

@Mod("worldtools")
public class WorldToolsForge {
    private static final IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;
    public WorldToolsForge() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            WorldTools.INSTANCE.initialize();
            IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
            modEventBus.addListener(this::onInitializeClient);
            FORGE_EVENT_BUS.addListener(this::onRegisterCommands);
            FORGE_EVENT_BUS.addListener(this::onRegisterKeybinds);
            FORGE_EVENT_BUS.addListener(this::onPlayerJoin);
            FORGE_EVENT_BUS.addListener(this::onRightClickBlock);
            FORGE_EVENT_BUS.addListener(this::onEntityJoinLevel);
            FORGE_EVENT_BUS.addListener(this::onAfterScreenInit);
            BlockEntityRendererFactories.register(BlockEntityType.CHEST, MissingChestBlockEntityRenderer::new);
            FORGE_EVENT_BUS.register(modEventBus);
            WorldTools.INSTANCE.getLOGGER().info("WorldTools Forge initialized");
        });
    }

    public void onInitializeClient(final FMLClientSetupEvent event) {
    }

    public void onRegisterCommands(final RegisterClientCommandsEvent event) {
        WorldToolsForgeCommandBuilder.INSTANCE.register(event.getDispatcher());
    }

    public void onRegisterKeybinds(final RegisterKeyMappingsEvent event) {
        event.register(WorldTools.INSTANCE.getGUI_KEY());
    }

    public void onPlayerJoin(final ClientPlayerNetworkEvent.LoggingIn event) {
        Events.INSTANCE.onClientJoin();
    }

    public void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        Events.INSTANCE.onInteractBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
    }

    public void onEntityJoinLevel(final EntityJoinLevelEvent event) {
        Events.INSTANCE.onEntityLoad(event.getEntity());
    }

    public void onAfterScreenInit(final ScreenEvent.Init.Post event) {
        Events.INSTANCE.onAfterInitScreen(event.getScreen());
    }
}
