package org.waste.of.time.event

import net.minecraft.client.world.ClientWorld
import net.minecraft.world.chunk.WorldChunk
import org.waste.of.time.WorldTools

object Events {

    fun onChunkLoad(world: ClientWorld, chunk: WorldChunk) {
        if (!WorldTools.caching) return
        WorldTools.cachedChunks.add(chunk)
        WorldTools.checkCache()
    }

    fun onChunkUnload(world: ClientWorld, chunk: WorldChunk) {
        if (!WorldTools.caching) return
        WorldTools.cachedChunks.remove(chunk)
        WorldTools.checkCache()
    }
}
