package org.waste.of.time.storage.serializable

import com.mojang.authlib.GameProfile
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.text.MutableText
import net.minecraft.util.PathUtil
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage.Session
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.CaptureManager.levelName
import org.waste.of.time.manager.CaptureManager.serverInfo
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.Utils
import org.waste.of.time.WorldTools.CREDIT_MESSAGE_MD
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.MOD_NAME
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.storage.Storeable
import org.waste.of.time.storage.PathTreeNode
import org.waste.of.time.storage.CustomRegionBasedStorage
import java.net.InetSocketAddress
import java.nio.file.Path
import kotlin.io.path.writeBytes

class MetadataStoreable : Storeable {
    override fun shouldStore() = config.capture.metadata

    override val verboseInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.metadata",
            currentLevelName
        )

    override val anonymizedInfo: MutableText
        get() = verboseInfo



    override fun store(session: Session, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        session.writeIconFile()

        session.getDirectory(WorldSavePath.ROOT).resolve(MOD_NAME).apply {
            PathUtil.createDirectories(this)

            writePlayerEntryList()
            writeDimensionTree()
            writeMetadata()
        }
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
        appendLine("- **Time**: `${Utils.getTime()}` (Timestamp: `${System.currentTimeMillis()}`)")
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
}
