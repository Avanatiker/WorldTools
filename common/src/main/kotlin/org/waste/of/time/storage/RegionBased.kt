package org.waste.of.time.storage

import net.minecraft.nbt.NbtCompound
import net.minecraft.util.WorldSavePath
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.storage.cache.HotCache
import org.waste.of.time.storage.serializable.RegionBasedChunk

interface RegionBased : Storeable {
    val chunkPos: ChunkPos
    val world: World
    val suffix: String

    val dimension: String
        get() = world.registryKey.value.path

    val dimensionPath
        get() = when (dimension) {
            "overworld" -> ""
            "the_nether" -> "DIM-1/"
            "the_end" -> "DIM1/"
            else -> "dimensions/minecraft/$dimension/"
        }

    fun compound(storage: CustomRegionBasedStorage): NbtCompound

    fun incrementStats()

    override fun store(session: LevelStorage.Session, cachedStorages: MutableMap<String, CustomRegionBasedStorage>) {
        val path = session.getDirectory(WorldSavePath.ROOT)
            .resolve(dimensionPath)
            .resolve(suffix)
        val storage = cachedStorages.getOrPut(path.toString()) {
            CustomRegionBasedStorage(path, false)
        }

        if (this is RegionBasedChunk) {
            HotCache.getEntitySerializableForChunk(chunkPos)?.let {
                it.emit()
                it.incrementStats()
            }
        }

        try {
            storage.write(
                chunkPos,
                compound(storage)
            )
            incrementStats()
        } catch (e: Exception) {
            LOG.error("Failed to store $this", e)
        }
    }
}
