package org.waste.of.time.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.toast.SystemToast
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIntArray
import net.minecraft.nbt.NbtList
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.CREDIT_MESSAGE
import org.waste.of.time.WorldTools.LOGGER
import org.waste.of.time.WorldTools.MOD_ID
import org.waste.of.time.WorldTools.VERSION
import org.waste.of.time.WorldTools.credits
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.WorldTools.progressBar
import org.waste.of.time.mixin.BossBarHudAccessor
import org.waste.of.time.serializer.ClientChunkSerializer
import org.waste.of.time.serializer.LevelPropertySerializer
import org.waste.of.time.storage.CustomRegionBasedStorage
import java.net.InetSocketAddress
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.writeBytes


class SaveCommand : Command<FabricClientCommandSource> {
    private val NOT_FOUND_EXCEPTION = DynamicCommandExceptionType { arg: Any? ->
        Text.translatable(
            "commands.calias.notFound",
            arg
        )
    }

    private val bossBars = (mc.inGameHud.bossBarHud as BossBarHudAccessor).getBossBars()
    private val entityPartition = WorldTools.cachedEntities.partition { it is PlayerEntity }
    private val totalSteps = WorldTools.cachedChunks.size + entityPartition.second.size
    private val serverEntry = mc.currentServerEntry ?: throw NOT_FOUND_EXCEPTION.create("server")
    private var stepsDone = 0
    private val percentage: Float
        get() = stepsDone.toFloat() / totalSteps.toFloat()

    override fun run(context: CommandContext<FabricClientCommandSource>?): Int {
        val noAi = BoolArgumentType.getBool(context, "freezeEntities")
        val player = context?.source?.player ?: return -1

        messageWorldInfo(player)
        bossBars.putIfAbsent(progressBar.uuid, progressBar)

        CoroutineScope(Dispatchers.IO).launch {
            val session = mc.levelStorage.createSession(serverEntry.address)

            try {
                session.getDirectory(WorldSavePath.ROOT)
                    .resolve("$MOD_ID $VERSION metadata.txt")
                    .toFile()
                    .writeText(createMetadata(serverEntry, player))

                saveFavicon(session)
                LevelPropertySerializer.backupLevelDataFile(session, serverEntry.address, player)
                saveChunks(session)
                savePlayers(session, entityPartition.first.filterIsInstance<PlayerEntity>())
                saveEntities(session, noAi)

                session.close()
            } catch (exception: Exception) {
                val message = Text.of("Save failed: ${exception.localizedMessage}")

                mc.toastManager.add(
                    SystemToast.create(
                        mc,
                        SystemToast.Type.WORLD_ACCESS_FAILURE,
                        WorldTools.BRANDING,
                        message
                    )
                )
                mc.inGameHud.chatHud.addMessage(message)
                return@launch
            }

            sendSuccess()
            WorldTools.flush()
            bossBars.remove(progressBar.uuid)
        }
        return 0
    }

    private fun messageWorldInfo(player: ClientPlayerEntity) {
        val worldInfo = Text.of(
            "\nSaving world ${
                serverEntry.address
            } to disk...\nServer Brand: ${
                player.serverBrand
            }"
        ).copy()

        worldInfo.append(Text.of("\nServer motd:\n"))
        worldInfo.append(serverEntry.label)
        worldInfo.append(Text.of("\nVersion: "))
        worldInfo.append(serverEntry.version)

        if (serverEntry.playerCountLabel.string.isNotBlank()) {
            worldInfo.append(Text.of("\nPlayer Count Label: "))
            worldInfo.append(serverEntry.playerCountLabel)
        }

        worldInfo.append(Text.of("\nServer List Entry Name: "))
        worldInfo.append(serverEntry.name)

        if (serverEntry.playerListSummary?.isNotEmpty() == true) {
            worldInfo.append(Text.of("\nPlayer List Summary: "))
            serverEntry.playerListSummary?.forEach {
                worldInfo.append(it)
            }
        }

        worldInfo.append("\n")

        mc.inGameHud.chatHud.addMessage(worldInfo)

        val downloadInfo = Text.of(
            "Saving ${
                WorldTools.cachedChunks.size
            } chunks, ${
                entityPartition.first.size
            } players and ${
                entityPartition.second.size
            } entities to world ${
                serverEntry.address
            }"
        )

        mc.inGameHud.chatHud.addMessage(downloadInfo)
    }

    private fun createMetadata(serverEntry: ServerInfo, player: ClientPlayerEntity): String {
        val localDateTime = LocalDateTime.now()
        val zoneId = ZoneId.systemDefault() // or specify the desired zone

        val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)
        val formatter = DateTimeFormatter.RFC_1123_DATE_TIME

        val formattedDateTime = zonedDateTime.format(formatter)

        val infoBuilder = StringBuilder()

        infoBuilder.append("World save of ${serverEntry.address} captured at $formattedDateTime\n\n")
        infoBuilder.append("Server Brand: ${player.serverBrand}\n")
        infoBuilder.append("Server MOTD: ${serverEntry.label.string.split("\n").joinToString(" ")}\n")
        infoBuilder.append("Version: ${serverEntry.version.string}\n")

        if (serverEntry.playerCountLabel.string.isNotBlank()) {
            infoBuilder.append("Player Count Label: ${serverEntry.playerCountLabel.string}\n")
        }

        if (serverEntry.name != "Minecraft Server") {
            infoBuilder.append("Server List Entry Name: ${serverEntry.name}\n")
        }

        mc.networkHandler?.connection?.address?.let { (it as InetSocketAddress)
            infoBuilder.append("Server Host Name: ${it.address.canonicalHostName}\n")
            infoBuilder.append("Server Port: ${it.port}\n")
        }

        mc.networkHandler?.playerList?.let { list ->
            if (list.isEmpty()) return@let
            infoBuilder.append("Online Players: ${list.joinToString { it.profile.name }}\n")
        }

        serverEntry.playerListSummary?.let {
            if (it.isEmpty()) return@let
            infoBuilder.append("Player List Summary: ${it.joinToString(" ")}\n")
        }

        serverEntry.players?.let { players ->
            if (players.sample.isEmpty()) return@let
            infoBuilder.append("Players: ${players.sample.joinToString { it.name } }\n")
        }

        infoBuilder.append("\n")
        infoBuilder.append(CREDIT_MESSAGE)

        return infoBuilder.toString()
    }

    private fun saveFavicon(session: LevelStorage.Session) {
        serverEntry.favicon?.let { favicon ->
            session.iconFile.ifPresent {
                it.writeBytes(favicon)
                LOGGER.info("Saved favicon.")
            }
        }
    }

    private fun saveChunks(session: LevelStorage.Session) {
        var savedChunks = 0

        WorldTools.cachedChunks.groupBy { it.world }.forEach { chunkGroup ->
            val dimension = chunkGroup.key.registryKey.value.path
            val folder = dimensionFolder(dimension) + "region"
            val path = session.getDirectory(WorldSavePath.ROOT).resolve(folder)

            chunkGroup.value.forEach { chunk ->
                val chunkNbt = ClientChunkSerializer.serialize(chunk)
                chunkNbt.copyFrom(credits)
                CustomRegionBasedStorage(path, false).write(chunk.pos, chunkNbt)

                stepsDone++
                savedChunks++
                progressBar.percent = percentage
                progressBar.name = Text.of(
                    "${".2f".format(percentage * 100)}% - Saving chunk (${
                        savedChunks
                    }/${
                        WorldTools.cachedChunks.size
                    }) at ${
                        chunk.pos
                    } in dimension ${dimension}..."
                )
            }
        }
    }

    private fun savePlayers(session: LevelStorage.Session, players: List<PlayerEntity>) {
        players.forEach {
            session.createSaveHandler().savePlayerData(it)
        }
    }

    private fun saveEntities(session: Session, freezeEntities: Boolean) {
        var savedEntities = 0

        entityPartition.second.groupBy { it.world }.forEach { worldGroup ->
            val folder = dimensionFolder(worldGroup.key.registryKey.value.path) + "entities"

            val path = session.getDirectory(WorldSavePath.ROOT).resolve(folder)

            worldGroup.value.groupBy { it.chunkPos }.forEach { entityGroup ->
                val entityMain = NbtCompound()
                val entityList = NbtList()

                entityGroup.value.forEach {
                    val entityNbt = NbtCompound()
                    it.saveSelfNbt(entityNbt)
                    if (freezeEntities) entityNbt.putByte("NoAI", 1)
                    entityList.add(entityNbt)
                    stepsDone++
                    savedEntities++
                    progressBar.percent = stepsDone.toFloat() / totalSteps.toFloat()
                    progressBar.name = Text.of(
                        "${".2f".format(percentage * 100)}% - Saving ${it.name.string} (${
                            savedEntities
                        }/${
                            entityPartition.second.size
                        }) at ${it.blockPos.toShortString()}..."
                    )
                }

                entityMain.put("Entities", entityList)

                entityMain.putInt("DataVersion", 3465)

                val pos = NbtIntArray(intArrayOf(entityGroup.key.x, entityGroup.key.z))
                entityMain.put("Position", pos)

                entityMain.copyFrom(credits)

                CustomRegionBasedStorage(path, false).write(entityGroup.key, entityMain)
            }
        }
    }

    private fun sendSuccess() {
        val successMessage = Text.of(
            "Saved ${
                WorldTools.cachedChunks.size
            } chunks, ${entityPartition.first.size} players and ${
                entityPartition.second.size
            } entities to world ${serverEntry.address}"
        )

        mc.toastManager.add(
            SystemToast.create(
                mc,
                SystemToast.Type.WORLD_BACKUP,
                WorldTools.BRANDING,
                successMessage
            )
        )
        mc.inGameHud.chatHud.addMessage(successMessage)
    }

    private fun dimensionFolder(dimension: String) = when (dimension) {
        "overworld" -> ""
        "the_nether" -> "DIM-1/"
        "the_end" -> "DIM1/"
        else -> "$dimension/"
    }
}
