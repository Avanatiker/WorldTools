package org.waste.of.time.storage.serializable

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.NbtIo
import net.minecraft.text.MutableText
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.CaptureManager
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.Storeable

class MapDataStoreable() : Storeable {
    override fun shouldStore() = config.capture.maps
    override val verboseInfo: MutableText
        get() = MessageManager.translateHighlight(
            "worldtools.capture.saved.mapData",
            CaptureManager.currentLevelName
        )
    override val anonymizedInfo: MutableText
        get() = verboseInfo

    override fun store(session: LevelStorage.Session, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        // this map doesn't seem to be cleared until the world closes
        val dataDirectory = session.getDirectory(WorldSavePath.ROOT).resolve("data")
        if (!dataDirectory.toFile().exists()) {
            dataDirectory.toFile().mkdirs()
        }
        mc.world?.mapStates?.forEach { (mapName, mapState) ->
            val nbtCompound = NbtCompound()
            nbtCompound.put("data", mapState.writeNbt(NbtCompound()))
            NbtHelper.putDataVersion(nbtCompound)

            val mapFile = dataDirectory.resolve("$mapName${WorldTools.DAT_EXTENSION}").toFile()
            if (!mapFile.exists()) {
                mapFile.createNewFile()
            }
            NbtIo.writeCompressed(nbtCompound, mapFile)
        }
    }
}
