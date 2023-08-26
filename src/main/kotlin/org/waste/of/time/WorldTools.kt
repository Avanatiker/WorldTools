package org.waste.of.time

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.ClientBossBar
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.BossBar
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.world.chunk.WorldChunk
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.waste.of.time.command.WorldToolsCommandBuilder
import java.util.*
import java.util.concurrent.ConcurrentHashMap


object WorldTools : ClientModInitializer {
    const val MOD_ID = "WorldTools"
    const val VERSION = "1.0.0"
    private const val URL = "https://github.com/Avanatiker/WorldTools/"
    const val CREDIT_MESSAGE = "This file was created by $MOD_ID $VERSION ($URL)"
    const val MCA_EXTENSION = ".mca"
    const val DAT_EXTENSION = ".dat"

    val LOGGER: Logger = LogManager.getLogger()
    val BRANDING: Text = Text.of(MOD_ID)

    val mc: MinecraftClient = MinecraftClient.getInstance()
    var mm = MiniMessage.miniMessage()

    var capturing = true
    val cachedChunks: ConcurrentHashMap.KeySetView<WorldChunk, Boolean> = ConcurrentHashMap.newKeySet()
    val cachedEntities: ConcurrentHashMap.KeySetView<Entity, Boolean> = ConcurrentHashMap.newKeySet()

    val progressBar = ClientBossBar(
        UUID.randomUUID(),
        Text.of(""),
        0.0f,
        BossBar.Color.GREEN,
        BossBar.Style.PROGRESS,
        false,
        false,
        false
    )

    val credits: NbtCompound
        get() {
            val nbt = NbtCompound()
            nbt.putString("Credits", CREDIT_MESSAGE)
            return nbt
        }

    override fun onInitializeClient() {
        ClientChunkEvents.CHUNK_LOAD.register(ClientChunkEvents.Load { _, worldChunk ->
            if (capturing) cachedChunks.add(worldChunk)
        })

        ClientEntityEvents.ENTITY_LOAD.register(ClientEntityEvents.Load { entity, _ ->
            if (capturing) cachedEntities.add(entity)
        })

        // ToDo: cache lootable tile entities

        // ToDo: cache points of interest

        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            WorldToolsCommandBuilder.register(dispatcher)
        })
    }

    fun flush() {
        cachedChunks.clear()
        cachedEntities.clear()
    }
}