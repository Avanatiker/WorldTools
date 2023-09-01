package org.waste.of.time;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.waste.of.time.command.WorldToolsForgeCommandBuilder;
import org.waste.of.time.event.Events;
import org.waste.of.time.renderer.MissingChestBlockEntityRenderer;

@Mod("worldtools")
public class WorldToolsForge {
    public static IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;

    public WorldToolsForge() {
        WorldTools.INSTANCE.initialize();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(this::onInitializeClient);
            FORGE_EVENT_BUS.addListener(this::onRegisterCommands);
            FORGE_EVENT_BUS.addListener(this::onRegisterKeybinds);
            FORGE_EVENT_BUS.addListener(this::onPlayerJoin);
            FORGE_EVENT_BUS.addListener(this::onRightClickBlock);
            BlockEntityRendererFactories.register(BlockEntityType.CHEST, MissingChestBlockEntityRenderer::new);
            FORGE_EVENT_BUS.register(modEventBus);
        });

        WorldTools.INSTANCE.getLOGGER().info("WorldTools Forge initialized");
    }

    public void onInitializeClient(final FMLClientSetupEvent event) {
        WorldTools.INSTANCE.initialize();
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
}
