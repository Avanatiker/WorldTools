package org.waste.of.time

import dev.architectury.injectables.targets.ArchitecturyTarget
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.nbt.NbtCompound
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW
import org.waste.of.time.config.WorldToolsConfig

object WorldTools {
    const val MOD_NAME = "WorldTools"
    const val MOD_ID = "worldtools"
    private const val URL = "https://github.com/Avanatiker/WorldTools/"
    const val MCA_EXTENSION = ".mca"
    const val DAT_EXTENSION = ".dat"
    const val MAX_LEVEL_NAME_LENGTH = 16
    private const val HIGHLIGHT_COLOR = 0xFFA2C4
    private val VERSION: String by lazy {
        if (ArchitecturyTarget.getCurrentTarget() == "fabric") {
            FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().metadata.version.friendlyString
        } else {
            "1.1.1" // ToDo: Get version from forge loader
        }
    }
    val CREDIT_MESSAGE = "This file was created by $MOD_NAME $VERSION ($URL)"
    val CREDIT_MESSAGE_MD = "This file was created by [$MOD_NAME $VERSION]($URL)"
    val LOG: Logger = LogManager.getLogger()
    var CAPTURE_KEY = KeyBinding(
        "$MOD_ID.key.toggle_capture", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F12,
        "$MOD_ID.key.categories"
    )
    var CONFIG_KEY = KeyBinding(
        "$MOD_ID.key.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F10,
        "$MOD_ID.key.categories"
    )

    val mc: MinecraftClient = MinecraftClient.getInstance()

    lateinit var config: WorldToolsConfig; private set

    fun initialize() {
        LOG.info("Initializing $MOD_NAME $VERSION")
        AutoConfig.register(WorldToolsConfig::class.java, ::GsonConfigSerializer)
        config = AutoConfig.getConfigHolder(WorldToolsConfig::class.java).config
    }

    // Why cant I use the std lib?
    fun Boolean.toByte(): Byte = if (this) 1 else 0
    fun NbtCompound.addAuthor() = apply { putString("Author", CREDIT_MESSAGE) }
}
