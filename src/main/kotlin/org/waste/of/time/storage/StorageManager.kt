package org.waste.of.time.storage

import com.mojang.authlib.GameProfile
import net.kyori.adventure.platform.fabric.FabricClientAudiences
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor.color
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.client.toast.SystemToast
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIntArray
import net.minecraft.nbt.NbtList
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket
import net.minecraft.text.Text
import net.minecraft.util.PathUtil
import net.minecraft.util.WorldSavePath
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.BarManager
import org.waste.of.time.BarManager.resetProgressBar
import org.waste.of.time.BarManager.updateCapture
import org.waste.of.time.WorldTools.BRAND
import org.waste.of.time.WorldTools.CREDIT_MESSAGE_MD
import org.waste.of.time.WorldTools.LOGGER
import org.waste.of.time.WorldTools.MOD_NAME
import org.waste.of.time.WorldTools.addAuthor
import org.waste.of.time.WorldTools.cachedBlockEntities
import org.waste.of.time.WorldTools.cachedChunks
import org.waste.of.time.WorldTools.cachedEntities
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.WorldTools.mm
import org.waste.of.time.WorldTools.sendMessage
import org.waste.of.time.WorldTools.serverInfo
import org.waste.of.time.WorldTools.tryWithSession
import org.waste.of.time.serializer.ClientChunkSerializer
import org.waste.of.time.serializer.LevelPropertySerializer.writeLevelDataFile
import org.waste.of.time.serializer.PathTreeNode
import java.net.InetSocketAddress
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.writeBytes

object StorageManager {
    private var stepsDone = 0

    fun save(
        freezeWorld: Boolean = true
    ): Int {
        tryWithSession {
            stepsDone = 0

            val entitySnapshot = cachedEntities.partition { it is PlayerEntity }
            val chunkSnapshot = cachedChunks.toSet()
            val totalSteps = chunkSnapshot.size + entitySnapshot.second.size

            try {
                writeMetaData()
                writeFavicon()
                writeLevelDataFile(freezeWorld = freezeWorld)
                writeAdvancements()

                writePlayers(entitySnapshot.first.filterIsInstance<PlayerEntity>())
                writeChunks(chunkSnapshot, totalSteps)
                writeEntities(freezeWorld, totalSteps, entitySnapshot)

                sendSuccess(entitySnapshot, chunkSnapshot)
                updateCapture()
                resetProgressBar()

                mc.networkHandler?.sendPacket(ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS))
            } catch (exception: Exception) {
                LOGGER.error("Failed to save world.", exception)
                mc.execute {
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
                }
                return@tryWithSession
            }
        }

        return 0
    }

    private fun Session.writeMetaData() {
        getDirectory(WorldSavePath.ROOT).resolve(MOD_NAME).apply {
            PathUtil.createDirectories(this)

            resolve("Capture Metadata.md").toFile()
                .writeText(createMetadataMd())

            LOGGER.info("Saved capture metadata.")

            mc.networkHandler?.playerList?.let { playerList ->
                if (playerList.isEmpty()) return@let
                resolve("Player Entry List.csv").toFile()
                    .writeText(createPlayerListEntry(playerList.toList()))
            }

            LOGGER.info("Saved player entry list.")

            mc.networkHandler?.worldKeys?.let { keys ->
                if (keys.isEmpty()) return@let
                resolve("Dimension Tree.txt").toFile()
                    .writeText(PathTreeNode.buildTree(keys.map { it.value.path }))
            }

            LOGGER.info("Saved dimension tree.")
        }
    }

    private fun createMetadataMd() = StringBuilder().apply {
        val localDateTime = LocalDateTime.now()
        val zoneId = ZoneId.systemDefault()

        val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)
        val formatter = DateTimeFormatter.RFC_1123_DATE_TIME

        val formattedDateTime = zonedDateTime.format(formatter)

        appendLine("# ${serverInfo.address} World Save - Snapshot Details")
        appendLine()
        appendLine("- **Time**: `$formattedDateTime` (`${System.currentTimeMillis()}`)")
        appendLine("- **Captured By**: `${mc.player?.name?.string}`")

        appendLine("## Server")
        if (serverInfo.name != "Minecraft Server") {
            appendLine("- **List Entry Name**: `${serverInfo.name}`")
        }
        appendLine("- **IP**: `${serverInfo.address}`")
        if (serverInfo.playerCountLabel.string.isNotBlank()) {
            appendLine("- **Capacity**: `${serverInfo.playerCountLabel.string}`")
        }
        appendLine("- **Brand**: `${mc.player?.serverBrand}`")
        appendLine("- **MOTD**: `${serverInfo.label.string.split("\n").joinToString(" ")}`")
        appendLine("- **Version**: `${serverInfo.version.string}`")
        appendLine()
        serverInfo.players?.sample?.let { sample ->
            if (sample.isEmpty()) return@let
            appendLine("- **Short Label**: `${sample.joinToString { it.name }}`")
        }
        serverInfo.playerListSummary?.let {
            if (it.isEmpty()) return@let
            appendLine("- **Full Label**: `${it.joinToString(" ") { str -> str.string }}`")
        }

        appendLine("## Connection")
        (mc.networkHandler?.connection?.address as? InetSocketAddress)?.let {
            appendLine("- **Host Name**: `${it.address.canonicalHostName}`")
            appendLine("- **Port**: `${it.port}`")
        }
        mc.networkHandler?.sessionId?.let { id ->
            appendLine("- **Session ID**: `$id`")
        }

        appendLine()
        appendLine(CREDIT_MESSAGE_MD)
    }.toString()

    private fun createPlayerListEntry(listEntries: List<PlayerListEntry>) = StringBuilder().apply {
        appendLine("Name, ID, Legacy, Complete, Properties, Game Mode, Latency, Session ID, Scoreboard Team, Model")
        listEntries.forEach {
            serializePlayerListEntry(it)
        }
    }.toString()

    private fun StringBuilder.serializePlayerListEntry(entry: PlayerListEntry) {
        serializeGameProfile(entry.profile)
        append("${entry.gameMode.name}, ")
        append("${entry.latency}, ")
        append("${entry.session?.sessionId}, ")
//        entry.session?.publicKeyData?.let {
//            append("Public Key Data: ${it.data} ")
//        }
        append("${entry.scoreboardTeam?.name}, ")
        appendLine(entry.model)
    }

    private fun StringBuilder.serializeGameProfile(gameProfile: GameProfile) {
        append(gameProfile.name)
        append(", ")
        append(gameProfile.id)
        append(", ")
        append(gameProfile.isLegacy)
        append(", ")
        append(gameProfile.isComplete)
        append(", ")
        gameProfile.properties.forEach { t, u ->
            append("[key: $t | name: ${u.name} | value: ${u.value} | signature: ${u.signature}] ")
        }
        append(", ")
    }

    fun Session.writeFavicon() {
        serverInfo.favicon?.let { favicon ->
            iconFile.ifPresent {
                it.writeBytes(favicon)
                LOGGER.info("Saved favicon.")
            }
        }
    }

    private fun Session.writeAdvancements() {
        val advancements = getDirectory(WorldSavePath.ADVANCEMENTS).toFile()
    }

    private fun Session.writeChunks(chunks: Set<WorldChunk>, totalSteps: Int) {
        var savedChunks = 0

        chunks.groupBy { it.world }.forEach { chunkGroup ->
            val dimension = chunkGroup.key.registryKey.value.path
            val folder = dimensionFolder(dimension) + "region"
            val path = getDirectory(WorldSavePath.ROOT).resolve(folder)

            chunkGroup.value.forEach { chunk ->
                CustomRegionBasedStorage(path, false)
                    .write(chunk.pos, ClientChunkSerializer.serialize(chunk))

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

        LOGGER.info("Saved ${chunks.size} chunks.")
    }

    private fun Session.writePlayers(players: List<PlayerEntity>) {
        players.forEach {
            createSaveHandler().savePlayerData(it)
            cachedEntities.remove(it)
        }

        LOGGER.info("Saved ${players.size} players.")
    }

    private fun Session.writeEntities(
        freezeEntities: Boolean,
        totalSteps: Int,
        entityPartition: Pair<List<Entity>, List<Entity>>
    ) {
        var savedEntities = 0

        entityPartition.second.groupBy { it.world }.forEach { worldGroup ->
            val folder = dimensionFolder(worldGroup.key.registryKey.value.path) + "entities"

            val path = getDirectory(WorldSavePath.ROOT).resolve(folder)

            worldGroup.value.groupBy { it.chunkPos }.forEach { entityGroup ->
                CustomRegionBasedStorage(path, false).write(entityGroup.key, NbtCompound().apply {
                    put("Entities", NbtList().apply {
                        entityGroup.value.forEach {
                            add(NbtCompound().apply {
                                addAuthor()

                                it.saveSelfNbt(this)

                                if (freezeEntities) {
                                    putByte("NoAI", 1)
                                    putByte("NoGravity", 1)
                                    putByte("Invulnerable", 1)
                                    putByte("Silent", 1)
                                }
                            })

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
                    })

                    putInt("DataVersion", 3465)
                    put("Position", NbtIntArray(intArrayOf(entityGroup.key.x, entityGroup.key.z)))
                })
            }
        }

        LOGGER.info("Saved ${entityPartition.second.size} entities.")
    }

    private fun Session.sendSuccess(
        entityPartition: Pair<List<Entity>, List<Entity>>,
        chunks: Set<WorldChunk>
    ) {
        val savedPath = getDirectory(WorldSavePath.ROOT).toFile()

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

        mc.execute {
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
    }

    private fun dimensionFolder(dimension: String) = when (dimension) {
        "overworld" -> ""
        "the_nether" -> "DIM-1/"
        "the_end" -> "DIM1/"
        else -> "dimensions/minecraft/$dimension/"
    }
}