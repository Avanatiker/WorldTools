package org.waste.of.time

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.level.storage.LevelStorage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.util.concurrent.ConcurrentLinkedDeque


object WorldTools : ClientModInitializer {
    val LOGGER: Logger = LogManager.getLogger()
    val levelStorage: LevelStorage = LevelStorage.create(File("WorldTools").toPath())
    val cachedChunks: ConcurrentLinkedDeque<WorldChunk> = ConcurrentLinkedDeque()
    private val cachedPlayers: ConcurrentLinkedDeque<PlayerEntity> = ConcurrentLinkedDeque()

    override fun onInitializeClient() {
        ClientChunkEvents.CHUNK_LOAD.register(ClientChunkEvents.Load { _, worldChunk ->
            cachedChunks.add(worldChunk)
        })

        // ToDo: cache players

        // ToDo: cache entities

        // ToDo: cache tile entities

        // ToDo: cache point of interests

        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            dispatcher.register(literal("world").then(literal("save").executes(SaveCommand)))
        })
    }
}