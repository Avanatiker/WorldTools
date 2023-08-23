package org.waste.of.time

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.client.toast.SystemToast
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath


object SaveCommand : Command<FabricClientCommandSource> {
    override fun run(context: CommandContext<FabricClientCommandSource>?): Int {
        val player = context?.source?.player ?: return 0
        val levelName = player.serverBrand ?: return 0

        CoroutineScope(Dispatchers.IO).launch {
            WorldTools.cachedChunks.groupBy { it.world }.forEach { entry ->
                val session = WorldTools.levelStorage.createSession(levelName)
                session.createSaveHandler().savePlayerData(player)

                LevelPropertySerializer.backupLevelDataFile(
                    session,
                    levelName,
                    player,
                    entry.key
                )

                entry.value.forEach { chunk ->
                    CustomRegionBasedStorage(
                        session.getDirectory(WorldSavePath.ROOT).resolve("region"), false
                    ).write(chunk.pos, ClientChunkSerializer.serialize(chunk))
                }
            }
            MinecraftClient.getInstance().toastManager.add(
                SystemToast.create(
                    MinecraftClient.getInstance(),
                    SystemToast.Type.WORLD_BACKUP,
                    Text.of("WorldTools"),
                    Text.of("Save completed")
                )
            )
            MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("Save completed"))
        }
        return 0
    }
}
