package org.waste.of.time.storage.serializable

import com.google.gson.GsonBuilder
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import net.minecraft.advancement.AdvancementProgress
import net.minecraft.advancement.PlayerAdvancementTracker
import net.minecraft.datafixer.DataFixTypes
import net.minecraft.text.MutableText
import net.minecraft.util.Identifier
import net.minecraft.util.PathUtil
import net.minecraft.util.Util
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.Storeable
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class AdvancementsStoreable : Storeable {
    override fun shouldStore() = config.capture.advancements

    override val verboseInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.advancements",
            mc.player?.name ?: "Unknown"
        )

    override val anonymizedInfo: MutableText
        get() = verboseInfo

    private val progressMapCodec: Codec<PlayerAdvancementTracker.ProgressMap> =
        DataFixTypes.ADVANCEMENTS.createDataFixingCodec(PlayerAdvancementTracker.ProgressMap.CODEC, mc.dataFixer, 1343);

    private val gson = GsonBuilder().setPrettyPrinting().create()

    override fun store(
        session: LevelStorage.Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        val uuid = mc.player?.uuid ?: return
        val advancements = session.getDirectory(WorldSavePath.ADVANCEMENTS)
        PathUtil.createDirectories(advancements)

        val progress = (mc.player?.networkHandler?.advancementHandler)?.advancementProgresses ?: return

        val progressMapTemp = LinkedHashMap<Identifier, AdvancementProgress>()
        progress.entries.forEach { (key, advancementProgress) ->
            if (!advancementProgress.isAnyObtained) return@forEach
            progressMapTemp[key.id()] = advancementProgress
        }
        val progressMap = PlayerAdvancementTracker.ProgressMap(progressMapTemp)
        val jsonElement = Util.getResult(
            progressMapCodec.encodeStart(JsonOps.INSTANCE, progressMap)
        ) { s: String? -> IllegalStateException(s) }

        val path = advancements.resolve("$uuid.json")

        Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { writer ->
            gson.toJson(jsonElement, writer)
        }

        WorldTools.LOG.info("Saved ${progressMap.map().size} advancements.")
    }
}
