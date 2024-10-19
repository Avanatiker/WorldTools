package org.waste.of.time.storage

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.PathUtil
import net.minecraft.util.ThrowableDeliverer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.storage.RegionFile
import net.minecraft.world.storage.StorageKey
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.MCA_EXTENSION
import org.waste.of.time.WorldTools.MOD_NAME
import org.waste.of.time.WorldTools.mc
import java.io.DataOutput
import java.io.IOException
import java.nio.file.Path


open class CustomRegionBasedStorage internal constructor(
    private val directory: Path,
    private val dsync: Boolean
) : AutoCloseable {
    private val cachedRegionFiles: Long2ObjectLinkedOpenHashMap<RegionFile?> = Long2ObjectLinkedOpenHashMap()

    companion object {
        // Seems to only be used for MC's profiler
        // simpler to just use a default key instead of wiring this all in here
        val defaultStorageKey: StorageKey = StorageKey(MOD_NAME, World.OVERWORLD, "chunk")
    }

    @Throws(IOException::class)
    fun getRegionFile(pos: ChunkPos): RegionFile {
        val longPos = ChunkPos.toLong(pos.regionX, pos.regionZ)
        cachedRegionFiles.getAndMoveToFirst(longPos)?.let { return it }

        if (cachedRegionFiles.size >= 256) {
            cachedRegionFiles.removeLast()?.close()
        }

        PathUtil.createDirectories(directory)
        val path = directory.resolve("r." + pos.regionX + "." + pos.regionZ + MCA_EXTENSION)
        val regionFile = RegionFile(defaultStorageKey, path, directory, dsync)
        cachedRegionFiles.putAndMoveToFirst(longPos, regionFile)
        return regionFile
    }

    @Throws(IOException::class)
    fun write(pos: ChunkPos, nbt: NbtCompound?) {
        val regionFile = getRegionFile(pos)
        if (nbt == null) {
            regionFile.delete(pos)
        } else {
            regionFile.getChunkOutputStream(pos).use { dataOutputStream ->
                NbtIo.write(nbt, dataOutputStream as DataOutput)
            }
        }
    }

    private fun getNbtAt(chunkPos: ChunkPos) =
        getRegionFile(chunkPos).getChunkInputStream(chunkPos)?.use { dataInputStream ->
            NbtIo.readCompound(dataInputStream)
        }

    fun getBlockEntities(chunkPos: ChunkPos): List<BlockEntity> =
        getNbtAt(chunkPos)
            ?.getList("block_entities", 10)
            ?.filterIsInstance<NbtCompound>()
            ?.mapNotNull { compoundTag ->
                val blockPos = BlockPos(compoundTag.getInt("x"), compoundTag.getInt("y"), compoundTag.getInt("z"))
                val blockStateIdentifier = Identifier.of(compoundTag.getString("id"))
                val world = mc.world ?: return@mapNotNull null

                runCatching {
                    val block = Registries.BLOCK.get(blockStateIdentifier)
                    Registries.BLOCK_ENTITY_TYPE
                        .getOrEmpty(blockStateIdentifier)
                        .orElse(null)
                        ?.instantiate(blockPos, block.defaultState)?.apply {
                            read(compoundTag, world.registryManager)
                        }
                }.getOrNull()
            } ?: emptyList()

    @Throws(IOException::class)
    override fun close() {
        val throwableDeliverer = ThrowableDeliverer<IOException>()

        cachedRegionFiles.values.filterNotNull().forEach { regionFile ->
            try {
                regionFile.close()
            } catch (iOException: IOException) {
                throwableDeliverer.add(iOException)
            }
        }

        throwableDeliverer.deliver()
    }
}
