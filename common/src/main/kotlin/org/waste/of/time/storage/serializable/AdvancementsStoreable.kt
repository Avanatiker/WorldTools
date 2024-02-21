package org.waste.of.time.storage.serializable

import com.google.gson.GsonBuilder
import net.minecraft.SharedConstants
import net.minecraft.advancement.AdvancementProgress
import net.minecraft.text.MutableText
import net.minecraft.util.Identifier
import net.minecraft.util.PathUtil
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.Storeable
import java.lang.reflect.Type

class AdvancementsStoreable : Storeable {
    override fun shouldStore() = config.general.capture.advancements

    override val verboseInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.advancements",
            mc.player?.name ?: "Unknown"
        )

    override val anonymizedInfo: MutableText
        get() = verboseInfo

    private val gson = GsonBuilder().registerTypeAdapter(
        AdvancementProgress::class.java as Type,
        AdvancementProgress.Serializer()
    ).registerTypeAdapter(
        Identifier::class.java as Type,
        Identifier.Serializer()
    ).setPrettyPrinting().create()

    override fun store(
        session: LevelStorage.Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        val uuid = mc.player?.uuid ?: return
        val advancements = session.getDirectory(WorldSavePath.ADVANCEMENTS)
        PathUtil.createDirectories(advancements)

        val progress = mc.player?.networkHandler?.advancementHandler?.advancementProgresses ?: return

        val progressMap = LinkedHashMap<Identifier, AdvancementProgress>()
        progress.entries.forEach { (key, advancementProgress) ->
            if (!advancementProgress.isAnyObtained) return@forEach
            progressMap[key.id] = advancementProgress
        }
        val jsonElement = gson.toJsonTree(progressMap)
        jsonElement.asJsonObject.addProperty("DataVersion", SharedConstants.getGameVersion().saveVersion.id)

        advancements.resolve("$uuid.json").toFile().writeText(jsonElement.toString())

        WorldTools.LOG.info("Saved ${progressMap.size} advancements.")
    }
}
