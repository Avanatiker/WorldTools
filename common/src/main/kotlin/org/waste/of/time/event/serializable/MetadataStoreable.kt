package org.waste.of.time.event.serializable

import com.google.gson.GsonBuilder
import com.mojang.authlib.GameProfile
import net.minecraft.SharedConstants
import net.minecraft.advancement.AdvancementProgress
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.PathUtil
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.CaptureManager.currentLevelName
import org.waste.of.time.CaptureManager.serverInfo
import org.waste.of.time.CaptureManager.levelName
import org.waste.of.time.MessageManager.infoToast
import org.waste.of.time.MessageManager.sendInfo
import org.waste.of.time.StatisticManager
import org.waste.of.time.TimeUtils
import org.waste.of.time.WorldTools.CREDIT_MESSAGE_MD
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.MOD_NAME
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.WorldTools.mm
import org.waste.of.time.event.Storeable
import org.waste.of.time.mixin.accessor.AdvancementProgressesAccessor
import org.waste.of.time.serializer.LevelPropertySerializer.writeLevelDataFile
import org.waste.of.time.serializer.PathTreeNode
import org.waste.of.time.storage.CustomRegionBasedStorage
import java.lang.reflect.Type
import java.net.InetSocketAddress
import java.nio.file.Path
import kotlin.io.path.writeBytes

class MetadataStoreable : Storeable {
    override fun toString() = "Metadata"

    override fun shouldStore() = config.capture.metadata

    override val message: String
        get() = "<lang:worldtools.capture.saved.metadata>"

    private val gson = GsonBuilder().registerTypeAdapter(
        AdvancementProgress::class.java as Type,
        AdvancementProgress.Serializer()
    ).registerTypeAdapter(
        Identifier::class.java as Type,
        Identifier.Serializer()
    ).setPrettyPrinting().create()

    override fun store(session: Session, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        session.getDirectory(WorldSavePath.ROOT).resolve(MOD_NAME).apply {
            PathUtil.createDirectories(this)

            writeMetadata()
            writePlayerEntryList()
            writeDimensionTree()
        }

        session.writeIconFile()
        session.writeLevelDataFile()
        session.writeAdvancements()

        session.sendSuccess()
    }

    private fun Path.writeMetadata() {
        resolve("Capture Metadata.md")
            .toFile()
            .writeText(createMetadata())

        LOG.info("Saved capture metadata.")
    }

    private fun Path.writePlayerEntryList() {
        if (mc.isInSingleplayer) return

        mc.networkHandler?.playerList?.let { playerList ->
            if (playerList.isEmpty()) return@let
            resolve("Player Entry List.csv").toFile()
                .writeText(createPlayerEntryList(playerList.toList()))
            LOG.info("Saved ${playerList.size} player entry list entries.")
        }
    }

    private fun Path.writeDimensionTree() {
        mc.networkHandler?.worldKeys?.let { keys ->
            if (keys.isEmpty()) return@let
            resolve("Dimension Tree.txt").toFile()
                .writeText(PathTreeNode.buildTree(keys.map { it.value.path }))
            LOG.info("Saved ${keys.size} dimensions in tree.")
        }
    }

    private fun Session.writeIconFile() {
        if (mc.isInSingleplayer) {
            mc.server?.iconFile?.ifPresent { spIconPath ->
                iconFile.ifPresent {
                    it.writeBytes(spIconPath.toFile().readBytes())
                }
            }
        } else {
            serverInfo.favicon?.let { favicon ->
                iconFile.ifPresent {
                    it.writeBytes(favicon)
                }
            }
        }
        LOG.info("Saved favicon.")
    }

    private fun Session.writeAdvancements() {
        val uuid = mc.player?.uuid ?: return
        val advancements = getDirectory(WorldSavePath.ADVANCEMENTS)
        PathUtil.createDirectories(advancements)

        val progress = (mc.player?.networkHandler?.advancementHandler as? AdvancementProgressesAccessor)?.advancementProgresses ?: return

        val progressMap = LinkedHashMap<Identifier, AdvancementProgress>()
        progress.entries.forEach { (key, advancementProgress) ->
            if (!advancementProgress.isAnyObtained) return@forEach
            progressMap[key.id] = advancementProgress
        }
        val jsonElement = gson.toJsonTree(progressMap)
        jsonElement.asJsonObject.addProperty("DataVersion", SharedConstants.getGameVersion().saveVersion.id)

        advancements.resolve("$uuid.json").toFile().writeText(jsonElement.toString())

        LOG.info("Saved ${progressMap.size} advancements.")
    }

    private fun createMetadata() = StringBuilder().apply {
        if (currentLevelName != levelName) {
            appendLine("# $currentLevelName ($levelName) World Save - Snapshot Details")
        } else {
            appendLine("# $currentLevelName World Save - Snapshot Details")
        }

        if (mc.isInSingleplayer) {
            appendLine("![World Icon](../icon.png)")
        } else {
            appendLine("![Server Icon](../icon.png)")
        }

        appendLine()
        appendLine("- **Time**: `${TimeUtils.getTime()}` (Timestamp: `${System.currentTimeMillis()}`)")
        appendLine("- **Captured By**: `${mc.player?.name?.string}`")

        appendLine()

        if (!mc.isInSingleplayer) {
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

            serverInfo.players?.sample?.let { sample ->
                if (sample.isEmpty()) return@let
                appendLine("- **Short Label**: `${sample.joinToString { it.name }}`")
            }
            serverInfo.playerListSummary?.let {
                if (it.isEmpty()) return@let
                appendLine("- **Full Label**: `${it.joinToString(" ") { str -> str.string }}`")
            }

            appendLine()
            appendLine("## Connection")
            (mc.networkHandler?.connection?.address as? InetSocketAddress)?.let {
                appendLine("- **Host Name**: `${it.address.canonicalHostName}`")
                appendLine("- **Port**: `${it.port}`")
            }
        } else {
            appendLine("## Singleplayer Capture")
            appendLine("- **Source World Name**: `${mc.server?.name}`")
            appendLine("- **Version**: `${mc.server?.version}`")
        }

        mc.networkHandler?.sessionId?.let { id ->
            appendLine("- **Session ID**: `$id`")
        }

        appendLine()
        appendLine(CREDIT_MESSAGE_MD)
    }.toString()

    private fun createPlayerEntryList(listEntries: List<PlayerListEntry>) = StringBuilder().apply {
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
        entry.session?.publicKeyData?.let {
            append("Public Key Data: ${it.data} ")
        }
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

    private fun Session.sendSuccess() {
        val savedPath = getDirectory(WorldSavePath.ROOT).toFile()
        val message = StatisticManager.message

        message.mm().infoToast()
        "$message <lang:capture.in_directory> <click:open_file:${savedPath.path}>$currentLevelName (<lang:capture.click_to_open>)</click>".mm().sendInfo()
    }
}
