package org.waste.of.time.config

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config

@Config(name = "worldtools")
class WorldToolsConfig : ConfigData {
    /**
     * See https://shedaniel.gitbook.io/cloth-config/auto-config/creating-a-config-class
     * for more information on how to create a config class
     *
     * we could create config categories or collapsable stuff here
     */
    var bossBarTimeout: Long = 1500L
    var showToasts = true
    var showChatMessages = true
    var showActionBarMessages = true
    var customWorldGenerator = true
    var freezeWorld = true
    var freezeEntities = false
}
