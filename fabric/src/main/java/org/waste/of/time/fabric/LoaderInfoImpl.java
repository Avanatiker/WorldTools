package org.waste.of.time.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class LoaderInfoImpl {
    public static String getVersion() {
        return FabricLoader.getInstance().getModContainer("worldtools").orElseThrow().getMetadata().getVersion().getFriendlyString();
    }
}
