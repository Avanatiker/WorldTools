package org.waste.of.time;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.waste.of.time.command.WorldToolsForgeCommandBuilder;

@Mod("world_tools")
public class WorldToolsForge {
    public static IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;

    public WorldToolsForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(this::onInitializeClient);
            FORGE_EVENT_BUS.addListener(this::onRegisterCommands);
            FORGE_EVENT_BUS.register(modEventBus);
        });
    }

    public void onInitializeClient(final FMLClientSetupEvent event) {
        WorldTools.INSTANCE.initialize();
    }

    public void onRegisterCommands(final RegisterClientCommandsEvent event) {
        WorldToolsForgeCommandBuilder.INSTANCE.register(event.getDispatcher());
    }
}
