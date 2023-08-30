package org.waste.of.time.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.platform.fabric.FabricClientAudiences
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextColor.color
import net.minecraft.client.toast.SystemToast
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIntArray
import net.minecraft.nbt.NbtList
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath
import net.minecraft.world.chunk.WorldChunk
import org.waste.of.time.BarManager
import org.waste.of.time.BarManager.resetProgressBar
import org.waste.of.time.BarManager.updateCapture
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.BRAND
import org.waste.of.time.WorldTools.COLOR
import org.waste.of.time.WorldTools.CREDIT_MESSAGE
import org.waste.of.time.WorldTools.LOGGER
import org.waste.of.time.WorldTools.MOD_ID
import org.waste.of.time.WorldTools.VERSION
import org.waste.of.time.WorldTools.cachedBlockEntities
import org.waste.of.time.WorldTools.cachedChunks
import org.waste.of.time.WorldTools.cachedEntities
import org.waste.of.time.WorldTools.creditNbt
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.WorldTools.mm
import org.waste.of.time.WorldTools.saving
import org.waste.of.time.WorldTools.sendMessage
import org.waste.of.time.WorldTools.serverInfo
import org.waste.of.time.WorldTools.session
import org.waste.of.time.serializer.ClientChunkSerializer
import org.waste.of.time.serializer.LevelPropertySerializer
import org.waste.of.time.serializer.LevelPropertySerializer.backupLevelDataFile
import java.net.InetSocketAddress
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.writeBytes

object StorageManager {
    private var stepsDone = 0

    fun save(
        freezeEntities: Boolean = false,
        messageInfo: Boolean = false,
        silent: Boolean = false,
        closeSession: Boolean = false
    ): Int {
        if (saving) {
            LOGGER.warn("Already saving world.")
            return -1
        }

        stepsDone = 0

        saving = true

        val entitySnapshot = cachedEntities.partition { it is PlayerEntity }
        val chunkSnapshot = cachedChunks.toSet()
        val totalSteps = chunkSnapshot.size + entitySnapshot.second.size

        if (messageInfo) messageWorldInfo(chunkSnapshot, entitySnapshot)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                session.getDirectory(WorldSavePath.ROOT)
                    .resolve("$MOD_ID $VERSION metadata.txt")
                    .toFile()
                    .writeText(createMetadata())

                saveFavicon()
                backupLevelDataFile()
                saveChunks(chunkSnapshot, totalSteps)
                savePlayers(entitySnapshot.first.filterIsInstance<PlayerEntity>())
                saveEntities(freezeEntities, totalSteps, entitySnapshot)

                if (!silent) sendSuccess(entitySnapshot, chunkSnapshot)
            } catch (exception: Exception) {
                if (silent) {
                    LOGGER.error("Failed to save world.", exception)
                    return@launch
                }

                val message = Text.of("Save failed: ${exception.localizedMessage}")

                mc.toastManager.add(
                    SystemToast.create(
                        mc,
                        SystemToast.Type.WORLD_ACCESS_FAILURE,
                        BRAND,
                        message
                    )
                )
                sendMessage(message)
                return@launch
            }

            saving = false
            updateCapture()
            resetProgressBar()

            if (closeSession) session.close()
        }

        return 0
    }

    private fun messageWorldInfo(
        chunks: Set<WorldChunk>,
        entityPartition: Pair<List<Entity>, List<Entity>>
    ) {
        val worldInfo = text("").apply {
            append(text("Saving world ", color(COLOR)))
            append(text(serverInfo.address, color(COLOR)))
            append(text(" to disk..."))
            append(newline())
            append(text("Server Brand: "))
            append(text(mc.player?.serverBrand ?: "None", color(COLOR)))
            append(newline())
            append(text("Server MOTD: "))
            append(text(serverInfo.label.string.split("\n").joinToString(" "), color(COLOR)))
            append(newline())
            append(text("Version: "))
            append(text(serverInfo.version.string, color(COLOR)))

            if (serverInfo.playerCountLabel.string.isNotBlank()) {
                append(newline())
                append(text("Player Count Label: "))
                append(text(serverInfo.playerCountLabel.string, color(COLOR)))
            }

            if (serverInfo.name != "Minecraft Server") {
                append(newline())
                append(text("Server List Entry Name: "))
                append(text(serverInfo.name, color(COLOR)))
            }

            if (serverInfo.playerListSummary?.isNotEmpty() == true) {
                append(newline())
                append(text("Player List Summary: "))
                serverInfo.playerListSummary?.forEach {
                    append(it)
                }
            }

            append(newline())
        }

        sendMessage(FabricClientAudiences.of().toNative(worldInfo))

        val downloadInfo = Text.of(
            "Saving ${
                chunks.size
            } chunks, ${
                entityPartition.first.size
            } players and ${
                entityPartition.second.size
            } entities to world ${
                serverInfo.address
            }"
        )

        sendMessage(downloadInfo)
    }

    private fun createMetadata(): String {
        val localDateTime = LocalDateTime.now()
        val zoneId = ZoneId.systemDefault()

        val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)
        val formatter = DateTimeFormatter.RFC_1123_DATE_TIME

        val formattedDateTime = zonedDateTime.format(formatter)

        val infoBuilder = StringBuilder()

        infoBuilder.append("World save of ${serverInfo.address} captured at $formattedDateTime by ${mc.player?.name?.string}\n\n")
        infoBuilder.append("Server Brand: ${mc.player?.serverBrand}\n")
        infoBuilder.append("Server MOTD: ${serverInfo.label.string.split("\n").joinToString(" ")}\n")
        infoBuilder.append("Version: ${serverInfo.version.string}\n")

        if (serverInfo.playerCountLabel.string.isNotBlank()) {
            infoBuilder.append("Player Count Label: ${serverInfo.playerCountLabel.string}\n")
        }

        if (serverInfo.name != "Minecraft Server") {
            infoBuilder.append("Server List Entry Name: ${serverInfo.name}\n")
        }

        (mc.networkHandler?.connection?.address as? InetSocketAddress)?.let {
            infoBuilder.append("Server Host Name: ${it.address.canonicalHostName}\n")
            infoBuilder.append("Server Port: ${it.port}\n")
        }

        mc.networkHandler?.playerList?.let { list ->
            if (list.isEmpty()) return@let
            infoBuilder.append("Online Players: ${list.joinToString { it.profile.name }}\n")
        }

        serverInfo.playerListSummary?.let {
            if (it.isEmpty()) return@let
            infoBuilder.append("Player List Summary: ${it.joinToString(" ")}\n")
        }

        serverInfo.players?.let { players ->
            if (players.sample.isEmpty()) return@let
            infoBuilder.append("Players: ${players.sample.joinToString { it.name }}\n")
        }

        infoBuilder.append("\n")
        infoBuilder.append(CREDIT_MESSAGE)

        return infoBuilder.toString()
    }

    fun saveFavicon() {
        serverInfo.favicon?.let { favicon ->
            session.iconFile.ifPresent {
                it.writeBytes(favicon)
                LOGGER.info("Saved favicon.")
            }
        }
    }

    private fun saveChunks(chunks: Set<WorldChunk>, totalSteps: Int) {
        var savedChunks = 0

        chunks.groupBy { it.world }.forEach { chunkGroup ->
            val dimension = chunkGroup.key.registryKey.value.path
            val folder = dimensionFolder(dimension) + "region"
            val path = session.getDirectory(WorldSavePath.ROOT).resolve(folder)

            chunkGroup.value.forEach { chunk ->
                val chunkNbt = ClientChunkSerializer.serialize(chunk)
                chunkNbt.copyFrom(creditNbt)
                CustomRegionBasedStorage(path, false).write(chunk.pos, chunkNbt)

                cachedChunks.remove(chunk)
                cachedBlockEntities.removeAll(chunk.blockEntities.map { it.value }.toSet())
                updateCapture()
                stepsDone++
                savedChunks++
                BarManager.updateSaveChunk(
                    stepsDone / totalSteps.toFloat(),
                    savedChunks,
                    chunks.size,
                    chunk.pos,
                    dimension
                )
            }
        }
    }

    private fun savePlayers(players: List<PlayerEntity>) {
        players.forEach {
            session.createSaveHandler().savePlayerData(it)
            cachedEntities.remove(it)
        }
    }

    private fun saveEntities(
        freezeEntities: Boolean,
        totalSteps: Int,
        entityPartition: Pair<List<Entity>, List<Entity>>
    ) {
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
                    if (freezeEntities) {
                        entityNbt.putByte("NoAI", 1)
                        entityNbt.putByte("NoGravity", 1)
                        entityNbt.putByte("Invulnerable", 1)
                        entityNbt.putByte("Silent", 1)
                    }
                    entityList.add(entityNbt)

                    cachedEntities.remove(it)
                    updateCapture()
                    stepsDone++
                    savedEntities++
                    BarManager.updateSaveEntity(
                        stepsDone / totalSteps.toFloat(),
                        savedEntities,
                        entityPartition.second.size,
                        it
                    )
                }

                entityMain.put("Entities", entityList)

                entityMain.putInt("DataVersion", 3465)

                val pos = NbtIntArray(intArrayOf(entityGroup.key.x, entityGroup.key.z))
                entityMain.put("Position", pos)

                entityMain.copyFrom(creditNbt)

                CustomRegionBasedStorage(path, false).write(entityGroup.key, entityMain)
            }
        }
    }

    private fun sendSuccess(
        entityPartition: Pair<List<Entity>, List<Entity>>,
        chunks: Set<WorldChunk>
    ) {
        val savedPath = session.getDirectory(WorldSavePath.ROOT).toFile()

        val message = mm.deserialize(
            "Saved <color:#FFA2C4>${
                chunks.size
            }</color> chunks, <color:#FFA2C4>${
                entityPartition.first.size
            }</color> players, and <color:#FFA2C4>${
                entityPartition.second.size
            }</color> entities to saves directory "
        ).append(
            text(
                "${serverInfo.address} (click to open)",
                color(0xFFA2C4)
            ).clickEvent(ClickEvent.openFile(savedPath.path))
        )

        val successMessage = FabricClientAudiences.of().toNative(message)

        mc.toastManager.add(
            SystemToast.create(
                mc,
                SystemToast.Type.WORLD_BACKUP,
                BRAND,
                successMessage
            )
        )
        sendMessage(successMessage)
    }

    private fun dimensionFolder(dimension: String) = when (dimension) {
        "overworld" -> ""
        "the_nether" -> "DIM-1/"
        "the_end" -> "DIM1/"
        else -> "dimensions/minecraft/$dimension/"
    }
}