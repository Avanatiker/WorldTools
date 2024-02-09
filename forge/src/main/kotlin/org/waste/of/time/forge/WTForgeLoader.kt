package org.waste.of.time.forge

import net.minecraftforge.fml.loading.FMLLoader
import org.waste.of.time.ILoader

class WTForgeLoader : ILoader {
    override fun getVersion(): String {
        return FMLLoader.getLoadingModList().getModFileById("worldtools").versionString()
    }
}
