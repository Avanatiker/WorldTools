package org.waste.of.time.event

import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World
import net.minecraft.world.chunk.WorldChunk
import org.waste.of.time.ChestHandler
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.GUI_KEY
import org.waste.of.time.WorldTools.LOGGER
import org.waste.of.time.WorldTools.cachedEntities
import org.waste.of.time.WorldTools.caching
import org.waste.of.time.WorldTools.checkCache
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.WorldTools.serverInfo
import org.waste.of.time.WorldTools.tryWithSession
import org.waste.of.time.gui.WorldToolsScreen
import org.waste.of.time.serializer.LevelPropertySerializer.writeLevelDataFile
import org.waste.of.time.storage.StorageManager.writeFavicon

object Events {

    fun onChunkLoad(chunk: WorldChunk) {
        if (!caching) return
        WorldTools.cachedChunks.add(chunk)
        checkCache()
    }

    fun onEntityLoad(entity: Entity) {
        if (!caching) return

        cachedEntities.add(entity)
        checkCache()
    }

    fun onClientTickStart() {
        if (GUI_KEY.wasPressed() && mc.world != null && mc.currentScreen == null) {
            mc.setScreen(WorldToolsScreen)
        }
    }

    fun onClientJoin() {
        if (!caching) return
        serverInfo = mc.currentServerEntry ?: return

        tryWithSession {
            writeFavicon()
            writeLevelDataFile()
        }
    }

    fun onInteractBlock(world: World, hitResult: BlockHitResult) {
        if (!caching) return

        val blockEntity = (world.getBlockEntity(hitResult.blockPos) as? ChestBlockEntity) ?: return
        WorldTools.lastOpenedContainer = blockEntity
    }
}
