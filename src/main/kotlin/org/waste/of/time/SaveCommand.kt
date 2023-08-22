package org.waste.of.time

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import java.io.File


object SaveCommand : Command<FabricClientCommandSource> {
    private val regionBasedStorage = CustomRegionBasedStorage(File("WorldTools/region").toPath(), false)

    override fun run(context: CommandContext<FabricClientCommandSource>?): Int {
        regionBasedStorage.use { storage ->
            WorldTools.cachedChunks.forEach { entry ->
                storage.write(entry.value.pos, ClientChunkSerializer.serialize(entry.value))
            }
        }
        return 0
    }
}
