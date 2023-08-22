package org.waste.of.time

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.nbt.NbtIo
import net.minecraft.world.storage.RegionBasedStorage
import net.minecraft.world.storage.RegionFile
import net.minecraft.world.storage.StorageIoWorker
import org.waste.of.time.WorldTools.LOGGER
import java.io.*
import java.util.zip.DeflaterOutputStream


object SaveCommand : Command<FabricClientCommandSource> {
    private val regionBasedStorage = CustomRegionBasedStorage(File("WorldTools/region").toPath(), false)

    override fun run(context: CommandContext<FabricClientCommandSource>?): Int {
        WorldTools.cachedChunks.forEach { entry ->
            regionBasedStorage.use { storage ->
                storage.write(entry.value.pos, ClientChunkSerializer.serialize(entry.value))
            }
        }

//        regionBasedStorage.close()

        return 0
    }
}