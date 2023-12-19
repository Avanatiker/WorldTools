package org.waste.of.time

import dev.architectury.injectables.targets.ArchitecturyTarget
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW

object WorldTools {
    const val MOD_NAME = "WorldTools"
    const val MOD_ID = "worldtools"
    private const val URL = "https://github.com/Avanatiker/WorldTools/"
    const val MCA_EXTENSION = ".mca"
    const val DAT_EXTENSION = ".dat"
    private const val HIGHLIGHT_COLOR = 0xFFA2C4
    const val BOSS_BAR_TIMEOUT = 1500L
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
        "key.$MOD_ID.toggle_capture", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F12,
        "key.categories.$MOD_ID"
    )
    var CONFIG_KEY = KeyBinding(
        "key.$MOD_ID.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F11,
        "key.categories.$MOD_ID"
    )

    val mc: MinecraftClient = MinecraftClient.getInstance()
    var mm = MiniMessage.miniMessage()

    // Settings
    var showToasts = true
    var showChatMessages = true
    var showActionBarMessages = true
    var customWorldGenerator = true
    var freezeWorld = true
    var freezeEntities = false

    fun initialize() {
        LOG.info("Initializing $MOD_NAME $VERSION")
    }

    fun String.mm(): Text {
        val component = mm.deserialize(this)
        val json = GsonComponentSerializer.gson().serialize(component)
        return Text.Serializer.fromJson(json) as Text
    }

    fun highlight(string: String) = "<color:#${HIGHLIGHT_COLOR.toString(16)}>$string</color>"

    fun NbtCompound.addAuthor() = apply { putString("Author", CREDIT_MESSAGE) }
}
