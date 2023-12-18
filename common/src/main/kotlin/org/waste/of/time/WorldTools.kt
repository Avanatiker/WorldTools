package org.waste.of.time

import dev.architectury.injectables.targets.ArchitecturyTarget
import kotlinx.coroutines.Job
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket
import net.minecraft.text.Text
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW
import org.waste.of.time.event.HotCache
import org.waste.of.time.event.StorageFlow
import org.waste.of.time.event.serializable.MetadataStoreable

object WorldTools {
    const val MOD_NAME = "WorldTools"
    const val MOD_ID = "worldtools"
    private const val URL = "https://github.com/Avanatiker/WorldTools/"
    const val MCA_EXTENSION = ".mca"
    const val DAT_EXTENSION = ".dat"
    const val COLOR = 0xFFA2C4

    fun highlight(string: String) = "<color:#FFA2C4>$string</color>"

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
    val BRAND: Text by lazy {
        "<color:green>W<color:gray>orld<color:green>T<color:gray>ools<reset>".mm()
    }

    var CAPTURE_KEY = KeyBinding(
        "key.$MOD_ID.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F12,
        "key.categories.$MOD_ID"
    )

//    var CAPTURE_KEY = KeyBinding(
//        "key.$MOD_ID.capture", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F11,
//        "key.categories.$MOD_ID"
//    )

    val mc: MinecraftClient = MinecraftClient.getInstance()
    var mm = MiniMessage.miniMessage()

    val caching: Boolean
        get() = capturing && !mc.isInSingleplayer
    val serverInfo: ServerInfo
        get() = mc.networkHandler?.serverInfo ?: throw IllegalStateException("Server info should not be null")
    private var storeJob: Job? = null

    // Settings
    var freezeWorld = true
    private var capturing = false
    var capturingWorldName: String? = null

    fun initialize() {
        LOG.info("Initializing $MOD_NAME $VERSION")
    }

    fun toggleCapture() {
        if (mc.isInSingleplayer) {
            sendMessage(Text.of("$MOD_NAME is not available in singleplayer"))
            return
        }

        if (capturing) stopCapture() else startCapture()
    }

    private fun startCapture() {
    fun startCapture(worldName: String? = null) {
        val potentialWorldName = sanitizeWorldName(worldName ?: serverInfo.address)
        if (potentialWorldName.isBlank() || potentialWorldName.length > 64) {
            sendMessage(Text.of("Invalid world name"))
            return
        }
        capturingWorldName = potentialWorldName
        capturing = true
        // todo: validate if a world already exists with this name. we need user to decide whether to merge or replace
        sendMessage(Text.of("Started capturing $capturingWorldName..."))

        storeJob = StorageFlow.launch(capturingWorldName!!)
    }

    private fun sanitizeWorldName(worldName: String): String {
        return worldName.replace(":", "_")
    }

    fun stopCapture() {
        sendMessage(Text.of("Saving $capturingWorldName..."))

        // update the stats and trigger writeStats() in StatisticSerializer
        mc.networkHandler?.sendPacket(ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS))

        HotCache.chunks.values.forEach { chunk ->
            chunk.emit()
        }

        HotCache.convertEntities().forEach { entity ->
            entity.emit()
        }

        HotCache.players.forEach { player ->
            player.emit()
        }

        MetadataStoreable().emit()

        capturing = false
    }

    fun sendMessage(text: Text) =
        mc.inGameHud.chatHud.addMessage(Text.of("[").copy().append(BRAND).copy().append("] ").append(text))

    fun String.mm(): Text {
        val component = mm.deserialize(this)
        val json = GsonComponentSerializer.gson().serialize(component)
        return Text.Serializer.fromJson(json) as Text
    }

    fun NbtCompound.addAuthor() = apply { putString("Author", CREDIT_MESSAGE) }
}
