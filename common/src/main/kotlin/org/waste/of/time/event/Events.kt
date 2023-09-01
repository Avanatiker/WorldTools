package org.waste.of.time.event

import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World
import net.minecraft.world.chunk.WorldChunk
import org.waste.of.time.ChestHandler
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.caching
import org.waste.of.time.WorldTools.serverInfo
import org.waste.of.time.WorldTools.tryWithSession
import org.waste.of.time.serializer.LevelPropertySerializer.writeLevelDataFile
import org.waste.of.time.storage.StorageManager.writeFavicon

object Events {

    fun onChunkLoad(world: ClientWorld, chunk: WorldChunk) {
        if (!caching) return
        WorldTools.cachedChunks.add(chunk)
        WorldTools.checkCache()
    }

    fun onChunkUnload(world: ClientWorld, chunk: WorldChunk) {
        if (!caching) return
        WorldTools.cachedChunks.remove(chunk)
        WorldTools.checkCache()
    }

    fun onClientTickStart() {
        val mc = MinecraftClient.getInstance()
//        if (GUI_KEY.wasPressed() && mc.world != null && mc.currentScreen == null) {
//                mc.setScreen(WorldToolsScreen)
//        }
    }

    fun onClientJoin() {
        val mc = MinecraftClient.getInstance()
        serverInfo = mc.currentServerEntry ?: return

        tryWithSession {
            writeFavicon()
            writeLevelDataFile()
        }
    }

    fun onInteractBlock(player: PlayerEntity, world: World, hand: Hand, hitResult: BlockHitResult) {
        if (!caching) return
        val blockEntity = world.getBlockEntity(hitResult.blockPos) ?: return
        if (blockEntity !is ChestBlockEntity) return
        WorldTools.lastOpenedContainer = blockEntity
    }

    fun onAfterInitScreen(screen: Screen) {
        // todo: don't think this is needed anymore
//        if (!caching) return
//        ChestHandler.register(screen)
    }

    fun onScreenRemoved(screen: Screen) {
        ChestHandler.onScreenRemoved(screen)
    }
}
