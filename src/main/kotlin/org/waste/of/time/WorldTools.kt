package org.waste.of.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.kyori.adventure.platform.fabric.FabricClientAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.Entity
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.level.storage.LevelStorage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW
import org.waste.of.time.command.WorldToolsCommandBuilder
import org.waste.of.time.gui.WorldToolsScreen
import org.waste.of.time.renderer.MissingChestBlockEntityRenderer
import org.waste.of.time.serializer.LevelPropertySerializer.writeLevelDataFile
import org.waste.of.time.storage.StorageManager
import org.waste.of.time.storage.StorageManager.writeFavicon
import java.util.concurrent.ConcurrentHashMap


object WorldTools : ClientModInitializer {
    const val MOD_NAME = "WorldTools"
    private const val MOD_ID = "world_tools"
    private const val VERSION = "1.0.0"
    private const val URL = "https://github.com/Avanatiker/WorldTools/"
    const val CREDIT_MESSAGE = "This file was created by $MOD_NAME $VERSION ($URL)"
    const val CREDIT_MESSAGE_MD = "This file was created by [$MOD_NAME $VERSION]($URL)"
    const val MCA_EXTENSION = ".mca"
    const val DAT_EXTENSION = ".dat"
    const val MAX_CACHE_SIZE = 1000
    const val COLOR = 0xFFA2C4

    val LOGGER: Logger = LogManager.getLogger()
    val BRAND: Text by lazy {
        mm("<color:green>W<color:gray>orld<color:green>T<color:gray>ools<reset>")
    }

    private var GUI_KEY = KeyBinding(
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

    override fun onInitializeClient() {
        ClientChunkEvents.CHUNK_LOAD.register(ClientChunkEvents.Load { _, worldChunk ->
            if (!caching) return@Load

            cachedChunks.add(worldChunk)
            checkCache()
        })

        ClientEntityEvents.ENTITY_LOAD.register(ClientEntityEvents.Load { entity, _ ->
            if (!caching) return@Load

            cachedEntities.add(entity)
            checkCache()
        })

        KeyBindingHelper.registerKeyBinding(GUI_KEY)

        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvents.StartTick { mc ->
            if (GUI_KEY.wasPressed() && mc.world != null && mc.currentScreen == null) {
                mc.setScreen(WorldToolsScreen)
            }
        })

        ClientPlayConnectionEvents.JOIN.register(ClientPlayConnectionEvents.Join { _, _, mc ->
            serverInfo = mc.currentServerEntry ?: return@Join

            tryWithSession {
                writeFavicon()
                writeLevelDataFile()
            }
        })

        // ToDo: delay disconnection until save is complete! maybe use world creation screen?

//        ClientPlayConnectionEvents.DISCONNECT.register(ClientPlayConnectionEvents.Disconnect { _, _ ->
//            StorageManager.save(silent = true)
//        })
//
//        ClientLifecycleEvents.CLIENT_STOPPING.register(ClientLifecycleEvents.ClientStopping {
//            StorageManager.save(silent = true)
//        })

        UseBlockCallback.EVENT.register(UseBlockCallback { _, world, _, hitResult ->
            lastOpenedContainer = null
            val blockEntity = world.getBlockEntity(hitResult.blockPos) ?: return@UseBlockCallback ActionResult.PASS
//            if (blockEntity !is LootableContainerBlockEntity) return@UseBlockCallback ActionResult.PASS
            if (blockEntity !is ChestBlockEntity) return@UseBlockCallback ActionResult.PASS

            lastOpenedContainer = blockEntity

            ActionResult.PASS
        })

        BlockEntityRendererFactories.register(BlockEntityType.CHEST) {
            MissingChestBlockEntityRenderer(it)
        }

        ScreenEvents.AFTER_INIT.register(ScreenEvents.AfterInit { _, screen, _, _ ->
            if (!caching) return@AfterInit

            ChestHandler.register(screen)
        })

        // ToDo: cache points of interest

        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            WorldToolsCommandBuilder.register(dispatcher)
        })
    }

    inline fun tryWithSession(crossinline block: LevelStorage.Session.() -> Unit) {
        if (savingMutex.tryLock().not()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                mc.levelStorage.createSession(serverInfo.address).use { session ->
                    session.block()
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to create session for ${serverInfo.address}", e)
            } finally {
                savingMutex.unlock()
            }
        }
    }

    suspend inline fun withSessionBlocking(crossinline block: LevelStorage.Session.() -> Unit) {
        savingMutex.lock()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                mc.levelStorage.createSession(serverInfo.address).use { session ->
                    session.block()
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to create session for ${serverInfo.address}", e)
            } finally {
                savingMutex.unlock()
            }
        }
    }

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

    fun mm(text: String) = FabricClientAudiences.of().toNative(mm.deserialize(text))

    fun NbtCompound.addAuthor() = apply { putString("Author", CREDIT_MESSAGE) }
}