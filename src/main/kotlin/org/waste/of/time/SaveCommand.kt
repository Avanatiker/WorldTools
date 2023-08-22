package org.waste.of.time

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import java.io.File


object SaveCommand : Command<FabricClientCommandSource> {
    private val regionBasedStorage = CustomRegionBasedStorage(File("WorldTools/region").toPath(), false)

    override fun run(context: CommandContext<FabricClientCommandSource>?): Int {
        CoroutineScope(Dispatchers.Default).launch {
            regionBasedStorage.use { storage ->
                WorldTools.cachedChunks.forEach { entry ->
                    storage.write(entry.value.pos, ClientChunkSerializer.serialize(entry.value))
                }
            }
            MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("Save completed"))
        }
        return 0
    }
}
