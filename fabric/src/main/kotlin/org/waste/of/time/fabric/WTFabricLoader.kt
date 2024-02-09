package org.waste.of.time.fabric

import net.fabricmc.loader.api.FabricLoader
import org.waste.of.time.ILoader

class WTFabricLoader : ILoader {
    override fun getVersion(): String {
        return FabricLoader.getInstance().getModContainer("worldtools").orElseThrow().metadata.version.friendlyString
    }
}
