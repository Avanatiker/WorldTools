package org.waste.of.time.storage

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.PathUtil
import net.minecraft.util.ThrowableDeliverer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.storage.RegionFile
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.MCA_EXTENSION
import java.io.DataOutput
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path


open class CustomRegionBasedStorage internal constructor(
    private val directory: Path,
    private val dsync: Boolean
) : AutoCloseable {
    private val cachedRegionFiles: Long2ObjectLinkedOpenHashMap<RegionFile?> = Long2ObjectLinkedOpenHashMap()

    @Throws(IOException::class)
    fun getRegionFile(pos: ChunkPos): RegionFile {
        val longPos = ChunkPos.toLong(pos.regionX, pos.regionZ)
        cachedRegionFiles.getAndMoveToFirst(longPos)?.let { return it }

        if (cachedRegionFiles.size >= 256) {
            cachedRegionFiles.removeLast()?.close()
        }

        PathUtil.createDirectories(directory)
        val path = directory.resolve("r." + pos.regionX + "." + pos.regionZ + MCA_EXTENSION)
        val regionFile = RegionFile(path, directory, dsync)
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

    // note: this does not use the region file cache mechanism at all
    fun regionFileFlow(): Flow<RegionFile> = flow {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) return@flow
        Files.newDirectoryStream(directory, "r.*$MCA_EXTENSION").use { stream ->
            stream.forEach { path ->
                emit(RegionFile(path, directory, dsync))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun getChunkDataTag(chunkPos: ChunkPos): NbtCompound? {
        val regionFile = getRegionFile(chunkPos)
        var nbt: NbtCompound? = null
        regionFile.getChunkInputStream(chunkPos)?.use { dataInputStream ->
            nbt = NbtIo.read(dataInputStream)
        }
        return nbt
    }

    fun getBlockEntities(chunkPos: ChunkPos): List<BlockEntity> {
        val blockEntities = mutableListOf<BlockEntity>()
        getChunkDataTag(chunkPos)?.let { chunkNbt ->
            val compoundTagsList: NbtList = chunkNbt.getList("block_entities", 10)
            compoundTagsList.filterIsInstance<NbtCompound>().forEach { compoundTag ->
                try {
                    val blockPos = BlockPos(compoundTag.getInt("x"), compoundTag.getInt("y"), compoundTag.getInt("z"))
                    val blockStateIdentifier = Identifier(compoundTag.getString("id"))
                    Registries.BLOCK.get(blockStateIdentifier).let { block ->
                        // todo: read block state from section data NBT?
                        //  doesn't seem necessary rn because we're just using this to get the pos and container contents
                        //  might be needed for certain container types with multiple block states, like chest vs double chest?
                        try {
                            Registries.BLOCK_ENTITY_TYPE.getOrEmpty(blockStateIdentifier).ifPresent {
                                it.instantiate(blockPos, block.defaultState)?.let { blockEntity ->
                                    blockEntity.readNbt(compoundTag)
                                    blockEntities.add(blockEntity)
                                }
                            }
                        } catch (e: Exception) {
                            LOG.debug("Error creating block entity: {} from NBT at chunk: {}", blockStateIdentifier, chunkPos)
                        }
                    }
                } catch (e: Exception) {
                    LOG.debug("Error reading existing block entity from chunk: {}", chunkPos)
                }
            }
        }
        return blockEntities
    }

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

    @Throws(IOException::class)
    fun sync() {
        cachedRegionFiles.values.filterNotNull().forEach { it.sync() }
    }
}
