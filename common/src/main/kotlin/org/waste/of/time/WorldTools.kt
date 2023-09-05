package org.waste.of.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.Entity
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.level.storage.LevelStorage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW
import org.waste.of.time.storage.StorageManager
import java.util.concurrent.ConcurrentHashMap

object WorldTools {
    const val MOD_NAME = "WorldTools"
    const val MOD_ID = "worldtools"
    private const val URL = "https://github.com/Avanatiker/WorldTools/"
    const val MCA_EXTENSION = ".mca"
    const val DAT_EXTENSION = ".dat"
    const val MAX_CACHE_SIZE = 1000
    const val COLOR = 0xFFA2C4

    private val VERSION by lazy {
        FabricLoader.getInstance().allMods.first { it.metadata.id == MOD_ID }.metadata.version
    }
    val CREDIT_MESSAGE = "This file was created by $MOD_NAME $VERSION ($URL)"
    val CREDIT_MESSAGE_MD = "This file was created by [$MOD_NAME $VERSION]($URL)"

    val LOGGER: Logger = LogManager.getLogger()
    val BRAND: Text by lazy {
        "<color:green>W<color:gray>orld<color:green>T<color:gray>ools<reset>".mm()
    }

    var GUI_KEY = KeyBinding(
        "key.$MOD_ID.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F12,
        "key.categories.$MOD_ID"
    )

    val mc: MinecraftClient = MinecraftClient.getInstance()
    var mm = MiniMessage.miniMessage()

    val savingMutex = Mutex()
    private var capturing = true
    val caching: Boolean
        get() = capturing && !mc.isInSingleplayer
    val cachedChunks: ConcurrentHashMap.KeySetView<WorldChunk, Boolean> = ConcurrentHashMap.newKeySet()
    val cachedEntities: ConcurrentHashMap.KeySetView<Entity, Boolean> = ConcurrentHashMap.newKeySet()
    val cachedBlockEntities: ConcurrentHashMap.KeySetView<ChestBlockEntity, Boolean> = ConcurrentHashMap.newKeySet()
    var lastOpenedContainer: ChestBlockEntity? = null

    lateinit var serverInfo: ServerInfo

    fun initialize() {
        LOGGER.info("Initializing $MOD_NAME $VERSION")
    }

    inline fun tryWithSession(crossinline block: LevelStorage.Session.() -> Unit) {
        if (savingMutex.tryLock().not()) return

        dispatchSessionThread { block() }
    }

    suspend inline fun withSessionBlocking(crossinline block: LevelStorage.Session.() -> Unit) {
        savingMutex.lock()

        dispatchSessionThread { block() }
    }

    fun dispatchSessionThread(block: LevelStorage.Session.() -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mc.levelStorage.createSession(serverInfo.address.sanitize()).use { session ->
                    session.block()
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to create session for ${serverInfo.address}", e)
            } finally {
                savingMutex.unlock()
            }
        }
    }

    private fun String.sanitize() = replace(":", "_")

    fun checkCache() {
        BarManager.updateCapture()
        if (cachedChunks.size + cachedEntities.size + cachedBlockEntities.size < MAX_CACHE_SIZE) return

        StorageManager.save()
    }

    fun startCapture() {
        capturing = true
    }

    fun stopCapture() {
        capturing = false
    }

    fun flush() {
        cachedChunks.clear()
        cachedEntities.clear()
        BarManager.updateCapture()
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
