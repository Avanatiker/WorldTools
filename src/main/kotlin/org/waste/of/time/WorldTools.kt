package org.waste.of.time

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.minecraft.world.chunk.WorldChunk
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.concurrent.ConcurrentHashMap


object WorldTools : ClientModInitializer {
    val LOGGER: Logger = LogManager.getLogger()
    val cachedChunks: ConcurrentHashMap<Long, WorldChunk> = ConcurrentHashMap()

    override fun onInitializeClient() {
        ClientChunkEvents.CHUNK_LOAD.register(ClientChunkEvents.Load { _, worldChunk ->
            LOGGER.info("Chunk cached: ${worldChunk.pos} (${worldChunk.status}) (${worldChunk.inhabitedTime})")
            cachedChunks[worldChunk.pos.toLong()] = worldChunk
        })

        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            dispatcher.register(literal("worldtools").then(literal("save").executes(SaveCommand)))
        })
    }
}