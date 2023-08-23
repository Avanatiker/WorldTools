package org.waste.of.time

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.toast.SystemToast
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath
import net.minecraft.world.World
import net.minecraft.world.chunk.WorldChunk
import java.io.IOException
import java.net.InetSocketAddress


object SaveCommand : Command<FabricClientCommandSource> {
    override fun run(context: CommandContext<FabricClientCommandSource>?): Int {
        val mc = MinecraftClient.getInstance()
        val player = context?.source?.player ?: return 0
        val levelName = (mc.networkHandler?.connection?.address as? InetSocketAddress)?.hostName.toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                WorldTools.cachedChunks.groupBy { it.world }.forEach { entry ->
                    saveWorldChunk(levelName, entry.key, entry.value, player)
                }
            } catch (exception: Exception) {
                val message = Text.of("Save failed: ${exception.localizedMessage}")

                mc.toastManager.add(
                    SystemToast.create(
                        mc,
                        SystemToast.Type.WORLD_ACCESS_FAILURE,
                        WorldTools.NAME,
                        message
                    )
                )
                mc.inGameHud.chatHud.addMessage(message)
                return@launch
            }

            val successMessage = "Saved ${WorldTools.cachedChunks.size} chunks to world $levelName"

            mc.toastManager.add(
                SystemToast.create(
                    mc,
                    SystemToast.Type.WORLD_BACKUP,
                    WorldTools.NAME,
                    Text.of(successMessage)
                )
            )
            mc.inGameHud.chatHud.addMessage(Text.of(successMessage))
        }
        return 0
    }

    /**
     * Saves all chunks of a world.
     * @param levelName the name of the world
     * @param world the world
     * @param player the player
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    private fun saveWorldChunk(
        levelName: String,
        world: World,
        worldChunks: List<WorldChunk>,
        player: ClientPlayerEntity
    ) {
        val session = MinecraftClient.getInstance().levelStorage.createSession(levelName)
        session.createSaveHandler().savePlayerData(player)

        LevelPropertySerializer.backupLevelDataFile(session, levelName, player, world)

        // ToDo: most likely there is a better way to get the path
        val path = when (val dimension = world.registryKey.value.path) {
            "overworld" -> "region"
            "the_nether" -> "DIM-1/region"
            "the_end" -> "DIM1/region"
            else -> "$dimension/region"
        }

        worldChunks.forEach { chunk ->
            CustomRegionBasedStorage(
                session.getDirectory(WorldSavePath.ROOT).resolve(path), false
            ).write(chunk.pos, ClientChunkSerializer.serialize(chunk))
        }

        session.close()
    }
}
