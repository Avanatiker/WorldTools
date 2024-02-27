package org.waste.of.time.fabric

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.autoconfig.AutoConfig
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screen.Screen
import org.waste.of.time.config.WorldToolsConfig

@Environment(EnvType.CLIENT)
class WorldToolsModMenuIntegration : ModMenuApi {

    override fun getModConfigScreenFactory() =
        ConfigScreenFactory { parent: Screen? ->
            AutoConfig.getConfigScreen(WorldToolsConfig::class.java, parent).get()
        }
}
