package org.waste.of.time.storage.serializable

import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import net.minecraft.advancement.PlayerAdvancementTracker
import net.minecraft.datafixer.DataFixTypes
import net.minecraft.text.MutableText
import java.nio.charset.StandardCharsets
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.WorldTools.CURRENT_VERSION
import org.waste.of.time.WorldTools.GSON
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.Storeable
import java.nio.file.Path

class AdvancementsStoreable : Storeable() {
    override fun shouldStore() = config.general.capture.advancements

    override val verboseInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.advancements",
            mc.player?.name ?: "Unknown"
        )

    override val anonymizedInfo: MutableText
        get() = verboseInfo

    private val progressMapCodec =
        DataFixTypes.ADVANCEMENTS.createDataFixingCodec(
            PlayerAdvancementTracker.ProgressMap.CODEC, mc.dataFixer, CURRENT_VERSION
        )

    override fun store(
        session: LevelStorage.Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        val uuid = mc.player?.uuid ?: return
        val progress = mc.player
            ?.networkHandler
            ?.advancementHandler
            ?.advancementProgresses ?: return
        val progressMap = progress.entries
            .filter { it.value.isAnyObtained }
            .associate {
                it.key.id to it.value
            }
        val jsonElement =
            progressMapCodec.encodeStart(
                JsonOps.INSTANCE,
                PlayerAdvancementTracker.ProgressMap(progressMap)
            ).getOrThrow() as JsonElement


        val advancements: Path = session.getDirectory(WorldSavePath.ADVANCEMENTS)
        java.nio.file.Files.createDirectories(advancements)
        java.nio.file.Files.newBufferedWriter(
            advancements.resolve("$uuid.json"),
            StandardCharsets.UTF_8
        ).use { writer ->
            GSON.toJson(jsonElement, writer)
        }

        LOG.info("Saved ${progressMap.size} advancements.")
    }
}
