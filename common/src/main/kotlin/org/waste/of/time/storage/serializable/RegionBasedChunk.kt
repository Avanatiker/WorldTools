package org.waste.of.time.storage.serializable

import net.minecraft.SharedConstants
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.fluid.Fluid
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtLongArray
import net.minecraft.nbt.NbtOps
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.MutableText
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.world.ChunkSerializer
import net.minecraft.world.LightType
import net.minecraft.world.biome.BiomeKeys
import net.minecraft.world.chunk.BelowZeroRetrogen
import net.minecraft.world.chunk.PalettedContainer
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.gen.chunk.BlendingData
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.Utils.addAuthor
import org.waste.of.time.WorldTools
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.TIMESTAMP_KEY
import org.waste.of.time.WorldTools.config
import org.waste.of.time.extension.IPalettedContainerExtension
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.manager.StatisticManager
import org.waste.of.time.storage.Cacheable
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.RegionBased
import org.waste.of.time.storage.cache.HotCache

open class RegionBasedChunk(
    val chunk: WorldChunk,
) : RegionBased(chunk.pos, chunk.world, "region"), Cacheable {
    // storing a reference to the block entities in the chunk to prevent them from being unloaded
    val cachedBlockEntities = mutableMapOf<BlockPos, BlockEntity>()

    init {
        cachedBlockEntities.putAll(chunk.blockEntities)

        cachedBlockEntities.values.associateWith { fresh ->
            HotCache.scannedContainers[fresh.pos]
        }.forEach { (fresh, cached) ->
            if (cached == null) return@forEach
            cachedBlockEntities[fresh.pos] = cached
        }
    }

    override fun shouldStore() = config.general.capture.chunks

    override val verboseInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.chunks",
            chunkPos,
            dimension
        )

    override val anonymizedInfo: MutableText
        get() = translateHighlight(
            "worldtools.capture.saved.chunks.anonymized",
            dimension
        )

    private val stateIdContainer = PalettedContainer.createPalettedContainerCodec(
        Block.STATE_IDS,
        BlockState.CODEC,
        PalettedContainer.PaletteProvider.BLOCK_STATE,
        Blocks.AIR.defaultState
    )

    override fun cache() {
        HotCache.chunks[chunkPos] = this
        HotCache.savedChunks.add(chunkPos.toLong())
    }

    override fun flush() {
        HotCache.chunks.remove(chunkPos)
    }

    override fun incrementStats() {
        StatisticManager.chunks++
        StatisticManager.dimensions.add(dimension)
    }

    override fun writeToStorage(
        session: LevelStorage.Session,
        storage: CustomRegionBasedStorage,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        // avoiding `emit` here due to flow order issues when capture is stopped
        // i.e., if EndFlow is emitted before this,
        // these are not written because they're behind it in the flow
        HotCache.getEntitySerializableForChunk(chunkPos, world)
            ?.store(session, cachedStorages)
            ?: run {
                // remove any previously stored entities in this chunk in case there are no entities to store
                RegionBasedEntities(chunkPos, emptySet(), world).store(session, cachedStorages)
        }
        if (chunk.isEmpty) return
        super.writeToStorage(session, storage, cachedStorages)
    }

    /**
     * See [net.minecraft.world.ChunkSerializer.serialize]
     */
    override fun compound() = NbtCompound().apply {
        if (config.world.metadata.captureTimestamp) {
            putLong(TIMESTAMP_KEY, System.currentTimeMillis())
        }

        if (config.world.metadata.waterMark) {
            addAuthor()
        }

        putInt("DataVersion", SharedConstants.getGameVersion().saveVersion.id)
        putInt(ChunkSerializer.X_POS_KEY, chunk.pos.x)
        putInt("yPos", chunk.bottomSectionCoord)
        putInt(ChunkSerializer.Z_POS_KEY, chunk.pos.z)
        putLong("LastUpdate", chunk.world.time)
        putLong("InhabitedTime", chunk.inhabitedTime)
        putString("Status", Registries.CHUNK_STATUS.getId(chunk.status).toString())

        genBackwardsCompat(chunk)

        if (!chunk.upgradeData.isDone) {
            put("UpgradeData", chunk.upgradeData.toNbt())
        }

        put(ChunkSerializer.SECTIONS_KEY, generateSections(chunk))

        if (chunk.isLightOn) {
            putBoolean(ChunkSerializer.IS_LIGHT_ON_KEY, true)
        }

        put("block_entities", NbtList().apply {
            upsertBlockEntities()
        })

        getTickSchedulers(chunk)
        genPostProcessing(chunk)

        // skip structures
        if (config.debug.logSavedChunks)
            LOG.info("Chunk saved: $chunkPos ($dimension)")
    }

    private fun NbtList.upsertBlockEntities() {
        cachedBlockEntities.entries.map { (_, blockEntity) ->
            blockEntity.createNbtWithIdentifyingData(WorldTools.mc.world!!.registryManager).apply {
                putBoolean("keepPacked", false)
            }
        }.apply {
            addAll(this)
        }
    }

    private fun generateSections(chunk: WorldChunk) = NbtList().apply {
        val biomeRegistry = chunk.world.registryManager.get(RegistryKeys.BIOME)
        val biomeCodec = PalettedContainer.createReadableContainerCodec(
            biomeRegistry.indexedEntries,
            biomeRegistry.entryCodec,
            PalettedContainer.PaletteProvider.BIOME,
            biomeRegistry.entryOf(BiomeKeys.PLAINS)
        )
        val lightingProvider = chunk.world.chunkManager.lightingProvider

        (lightingProvider.bottomY until lightingProvider.topY).forEach { y ->
            val sectionCoord = chunk.sectionCoordToIndex(y)
            val inSection = sectionCoord in (0 until chunk.sectionArray.size)
            val blockLightSection =
                lightingProvider[LightType.BLOCK].getLightSection(ChunkSectionPos.from(chunk.pos, y))
            val skyLightSection =
                lightingProvider[LightType.SKY].getLightSection(ChunkSectionPos.from(chunk.pos, y))

            if (!inSection && blockLightSection == null && skyLightSection == null) return@forEach

            add(NbtCompound().apply {
                if (inSection) {
                    val chunkSection = chunk.sectionArray[sectionCoord]
                    /**
                     * Mods like Bobby may also try serializing chunk data concurrently on separate threads
                     * PalettedContainer contains a lock that is acquired during read/write operations
                     *
                     * Force disabling checking the lock's status here as it should be safe to
                     * read here, no write operations should happen after the chunk is unloaded
                     */
                    (chunkSection.blockStateContainer as IPalettedContainerExtension).setWTIgnoreLock(true);
                    (chunkSection.biomeContainer as IPalettedContainerExtension).setWTIgnoreLock(true);
                    put(
                        "block_states",
                        stateIdContainer.encodeStart(NbtOps.INSTANCE, chunkSection.blockStateContainer).getOrThrow()
                    )
                    put(
                        "biomes",
                        biomeCodec.encodeStart(NbtOps.INSTANCE, chunkSection.biomeContainer).getOrThrow()
                    )
                }
                if (blockLightSection != null && !blockLightSection.isUninitialized) {
                    putByteArray(ChunkSerializer.BLOCK_LIGHT_KEY, blockLightSection.asByteArray())
                }
                if (skyLightSection != null && !skyLightSection.isUninitialized) {
                    putByteArray(ChunkSerializer.SKY_LIGHT_KEY, skyLightSection.asByteArray())
                }
                if (isEmpty) return@forEach
                putByte("Y", y.toByte())
            })
        }
    }

    private fun NbtCompound.genBackwardsCompat(chunk: WorldChunk) {
        chunk.blendingData?.let { bleedingData ->
            BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, bleedingData).resultOrPartial {
                LOG.error(it)
            }.ifPresent {
                put("blending_data", it)
            }
        }

        chunk.belowZeroRetrogen?.let { belowZeroRetrogen ->
            BelowZeroRetrogen.CODEC.encodeStart(NbtOps.INSTANCE, belowZeroRetrogen).resultOrPartial {
                LOG.error(it)
            }.ifPresent {
                put("below_zero_retrogen", it)
            }
        }
    }

    private fun NbtCompound.getTickSchedulers(chunk: WorldChunk) {
        val tickSchedulers = chunk.tickSchedulers
        val time = chunk.world.levelProperties.time

        put("block_ticks", tickSchedulers.blocks().toNbt(time) { block: Block? ->
            Registries.BLOCK.getId(block).toString()
        })
        put("fluid_ticks", tickSchedulers.fluids().toNbt(time) { fluid: Fluid? ->
            Registries.FLUID.getId(fluid).toString()
        })
    }

    private fun NbtCompound.genPostProcessing(chunk: WorldChunk) {
        put("PostProcessing", ChunkSerializer.toNbt(chunk.postProcessingLists))

        put(ChunkSerializer.HEIGHTMAPS_KEY, NbtCompound().apply {
            chunk.heightmaps.filter {
                chunk.status.heightmapTypes.contains(it.key)
            }.forEach { (key, value) ->
                put(key.getName(), NbtLongArray(value.asLongArray()))
            }
        })
    }
}
