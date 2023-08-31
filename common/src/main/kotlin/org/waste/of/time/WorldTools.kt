package org.waste.of.time

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.world.chunk.WorldChunk
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.waste.of.time.storage.StorageManager
import java.util.concurrent.ConcurrentHashMap


object WorldTools {
    const val MOD_ID = "WorldTools"
    const val VERSION = "1.0.0"
    private const val URL = "https://github.com/Avanatiker/WorldTools/"
    const val CREDIT_MESSAGE = "This file was created by $MOD_ID $VERSION ($URL)"
    const val MCA_EXTENSION = ".mca"
    const val DAT_EXTENSION = ".dat"
    const val MAX_CACHE_SIZE = 1000

    val LOGGER: Logger = LogManager.getLogger()
    val BRAND: Text by lazy {
        mm("<color:green>W<color:gray>orld<color:green>T<color:gray>ools<reset>")
    }

    val mc: MinecraftClient = MinecraftClient.getInstance()
    var mm = MiniMessage.miniMessage()

    var saving = false
    private var capturing = true
    val caching: Boolean
        get() = capturing && !mc.isInSingleplayer
    val cachedChunks: ConcurrentHashMap.KeySetView<WorldChunk, Boolean> = ConcurrentHashMap.newKeySet()
    val cachedEntities: ConcurrentHashMap.KeySetView<Entity, Boolean> = ConcurrentHashMap.newKeySet()

    val creditNbt: NbtCompound
        get() = NbtCompound().apply { putString("author", CREDIT_MESSAGE) }

    fun initialize() {
        LOGGER.info("Initializing $MOD_ID $VERSION")
    }

    fun checkCache() {
        BarManager.updateCapture()
        if ((cachedChunks.size < MAX_CACHE_SIZE && cachedEntities.size < MAX_CACHE_SIZE) || saving) return

        StorageManager.save()
    }

    fun startCapture() {
        capturing = true
        BarManager.startCapture()
    }

    fun stopCapture() {
        capturing = false
        BarManager.stopCapture()
    }

    fun flush() {
        cachedChunks.clear()
        cachedEntities.clear()
        BarManager.updateCapture()
    }

    fun sendMessage(text: Text) =
        mc.inGameHud.chatHud.addMessage(Text.of("[").copy().append(BRAND).copy().append("] ").append(text))

    fun mm(text: String): Text {
        val component = mm.deserialize(text)
        val json = GsonComponentSerializer.gson().serialize(component)
        return Text.Serializer.fromJson(json) as Text
    }
}
