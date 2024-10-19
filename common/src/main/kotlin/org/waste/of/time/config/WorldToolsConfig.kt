package org.waste.of.time.config

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.CollapsibleObject
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Excluded
import net.minecraft.entity.boss.BossBar

/**
 * See [Cloth Config Documentation](https://shedaniel.gitbook.io/cloth-config/auto-config/creating-a-config-class)
 * for more information on how to create a config class
 */
@Config(name = "worldtools")
class WorldToolsConfig : ConfigData {
    @TransitiveObject
    @Category("General")
    val general = General()

    @TransitiveObject
    @Category("World")
    val world = World()

    @TransitiveObject
    @Category("Entity")
    val entity = Entity()

    @TransitiveObject
    @Category("Render")
    val render = Render()

    @TransitiveObject
    @Category("Advanced")
    val advanced = Advanced()

    @TransitiveObject
    @Category("Debug")
    @ConfigEntry.Gui.PrefixText
    val debug = Debug()

    class General {
        @Tooltip
        var autoDownload = false
        @Tooltip
        var compressLevel = true
        @Excluded
        var reloadBlockEntities = true

        @CollapsibleObject(startExpanded = true)
        @Tooltip
        var capture = Capture()

        class Capture {
            var chunks = true
            var entities = true
            var players = true
            var statistics = true
            var levelData = true
            var advancements = true
            var metadata = true
            var maps = true
        }
    }

    class World {
        @CollapsibleObject(startExpanded = true)
        val worldGenerator = WorldGenerator()

        @CollapsibleObject(startExpanded = true)
        val gameRules = GameRules()

        @CollapsibleObject(startExpanded = true)
        val metadata = Metadata()

        @CollapsibleObject(startExpanded = true)
        @Excluded
        val censor = Censor()

        class WorldGenerator {
            @Tooltip
            var type = GeneratorType.VOID
            val bonusChest = false
            val generateFeatures = false
            var seed = 0L

            enum class GeneratorType {
                VOID,
                DEFAULT,
                FLAT
            }
        }

        class GameRules {
            @Tooltip
            var modifyGameRules = true

            var doWardenSpawning = false
            var doFireTick = false
            var doVinesSpread = false
            var doMobSpawning = false
            var doDaylightCycle = false
            var doWeatherCycle = false
            var keepInventory = true
            var doMobGriefing = false
            var doTraderSpawning = false
            var doPatrolSpawning = false
        }

        class Metadata {
            var captureTimestamp = true
        }

        class Censor {
            // ToDo: Add censoring options
        }
    }

    class Entity {
        @CollapsibleObject(startExpanded = true)
        val behavior = Behavior()

        @CollapsibleObject(startExpanded = true)
        val metadata = Metadata()

        @CollapsibleObject(startExpanded = true)
        val censor = Censor()

        class Behavior {
            @Tooltip
            var modifyEntityBehavior = false

            var noAI = true
            var noGravity = true
            var invulnerable = true
            var silent = true
        }

        class Metadata {
            var captureTimestamp = true
        }

        class Censor {
            // ToDo: Add censoring options

            @Excluded
            var names = false
            @Excluded
            var owner = false
            @Tooltip
            var lastDeathLocation = true
        }
    }

    class Render {
        var renderNotYetCachedContainers = true
        @ConfigEntry.ColorPicker
        var unscannedContainerColor = 0xDE0000
        @ConfigEntry.ColorPicker
        var fromCacheLoadedContainerColor = 0xFFA500
        @ConfigEntry.ColorPicker
        var unscannedEntityColor = 0xDE0000
        @ConfigEntry.ColorPicker
        var fromCacheLoadedEntityColor = 0xFFA500
        @ConfigEntry.ColorPicker
        var accentColor = 0xA2FF4C
        var captureBarColor = BossBar.Color.PINK
        var captureBarStyle = BossBar.Style.NOTCHED_10
        var progressBarColor = BossBar.Color.GREEN
        var progressBarStyle = BossBar.Style.PROGRESS
        @ConfigEntry.BoundedDiscrete(min = 50, max = 60000)
        var progressBarTimeout = 3000L
    }

    class Advanced {
        @Tooltip
        var anonymousMode = false
        @Tooltip
        var hideExperimentalWorldGui = true // See IntegratedServerLoaderMixin
        var showToasts = true
        var showChatMessages = true
        var keepEnderChestContents = false
    }

    class Debug {
        var logSettings = false
        var logSavedChunks = false
        var logSavedEntities = false
        var logSavedContainers = false
        var logSavedMaps = false
        var logZippingProgress = false
    }
}
