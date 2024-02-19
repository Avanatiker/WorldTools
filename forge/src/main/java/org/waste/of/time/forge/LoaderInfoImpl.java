package org.waste.of.time.forge;

import net.minecraftforge.fml.loading.FMLLoader;

public class LoaderInfoImpl {
    public static String getVersion() {
        return FMLLoader.getLoadingModList().getModFileById("worldtools").versionString();
    }

}
