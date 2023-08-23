package org.waste.of.time

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.text.Text
import net.minecraft.world.chunk.WorldChunk
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.concurrent.ConcurrentHashMap


object WorldTools : ClientModInitializer {
    val LOGGER: Logger = LogManager.getLogger()
    val NAME: Text = Text.of("WorldTools")
    const val MCA_EXTENSION = ".mca"
    val mc: MinecraftClient = MinecraftClient.getInstance()
    val cachedChunks: ConcurrentHashMap.KeySetView<WorldChunk, Boolean> = ConcurrentHashMap.newKeySet()
    val cachedEntities: ConcurrentHashMap.KeySetView<Entity, Boolean> = ConcurrentHashMap.newKeySet()

    override fun onInitializeClient() {
        ClientChunkEvents.CHUNK_LOAD.register(ClientChunkEvents.Load { _, worldChunk ->
            cachedChunks.add(worldChunk)
        })

        ClientEntityEvents.ENTITY_LOAD.register(ClientEntityEvents.Load { entity, _ ->
            cachedEntities.add(entity)
        })

        // ToDo: cache tile entities

        // ToDo: cache points of interest

        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            dispatcher.register(literal("world").then(literal("save").executes(SaveCommand)))
        })
    }
}