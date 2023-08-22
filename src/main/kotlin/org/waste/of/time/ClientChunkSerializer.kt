package org.waste.of.time

import net.minecraft.SharedConstants
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.fluid.Fluid
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtLongArray
import net.minecraft.nbt.NbtOps
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.structure.StructurePiece
import net.minecraft.structure.StructureStart
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.world.ChunkSerializer
import net.minecraft.world.LightType
import net.minecraft.world.biome.BiomeKeys
import net.minecraft.world.chunk.BelowZeroRetrogen
import net.minecraft.world.chunk.ChunkStatus
import net.minecraft.world.chunk.PalettedContainer
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.gen.chunk.BlendingData
import org.waste.of.time.WorldTools.LOGGER

object ClientChunkSerializer {
    private val BLOCK_CODEC = PalettedContainer.createPalettedContainerCodec(
        Block.STATE_IDS,
        BlockState.CODEC,
        PalettedContainer.PaletteProvider.BLOCK_STATE,
        Blocks.AIR.defaultState
    )

    fun serialize(chunk: WorldChunk): NbtCompound {
        val chunkPos = chunk.pos
        val nbt = NbtCompound()

        nbt.putInt("DataVersion", SharedConstants.getGameVersion().saveVersion.id)
        nbt.putInt(ChunkSerializer.X_POS_KEY, chunkPos.x)
        nbt.putInt("yPos", chunk.bottomSectionCoord)
        nbt.putInt(ChunkSerializer.Z_POS_KEY, chunkPos.z)
        nbt.putLong("LastUpdate", chunk.world.time)
        nbt.putLong("InhabitedTime", chunk.inhabitedTime)
        nbt.putString("Status", Registries.CHUNK_STATUS.getId(chunk.status).toString())

        chunk.blendingData?.let { bleedingData ->
            BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, bleedingData).resultOrPartial {
                LOGGER.error(it)
            }.ifPresent {
                nbt.put("blending_data", it)
            }
        }
        chunk.belowZeroRetrogen?.let { belowZeroRetrogen ->
            BelowZeroRetrogen.CODEC.encodeStart(NbtOps.INSTANCE, belowZeroRetrogen).resultOrPartial {
                LOGGER.error(it)
            }.ifPresent {
                nbt.put("below_zero_retrogen", it)
            }
        }

        if (!chunk.upgradeData.isDone()) {
            nbt.put("UpgradeData", chunk.upgradeData.toNbt())
        }

        val biomeRegistry = chunk.world.registryManager.get(RegistryKeys.BIOME)

        val biomeCodec = PalettedContainer.createReadableContainerCodec(
            biomeRegistry.indexedEntries,
            biomeRegistry.createEntryCodec(),
            PalettedContainer.PaletteProvider.BIOME,
            biomeRegistry.entryOf(BiomeKeys.PLAINS)
        )

        val sectionsList = NbtList()
        val lightingProvider = chunk.world.chunkManager.lightingProvider

        (lightingProvider.bottomY until lightingProvider.topY).forEach { y ->
            val sectionCoord = chunk.sectionCoordToIndex(y)
            val inSection = sectionCoord in (0 until chunk.sectionArray.size)
            val blockLightSection = lightingProvider[LightType.BLOCK].getLightSection(ChunkSectionPos.from(chunkPos, y))
            val skyLightSection = lightingProvider[LightType.SKY].getLightSection(ChunkSectionPos.from(chunkPos, y))

            if (!inSection && blockLightSection == null && skyLightSection == null) return@forEach

            val sectionNbt = NbtCompound()
            if (inSection) {
                val chunkSection = chunk.sectionArray[sectionCoord]

                sectionNbt.put(
                    "block_states",
                    BLOCK_CODEC.encodeStart(NbtOps.INSTANCE, chunkSection.blockStateContainer).getOrThrow(
                        false
                    ) { LOGGER.error(it) }
                )
                sectionNbt.put(
                    "biomes",
                    biomeCodec.encodeStart(NbtOps.INSTANCE, chunkSection.biomeContainer).getOrThrow(
                        false
                    ) { LOGGER.error(it) }
                )
            }
            if (blockLightSection != null && !blockLightSection.isUninitialized) {
                sectionNbt.putByteArray(ChunkSerializer.BLOCK_LIGHT_KEY, blockLightSection.asByteArray())
            }
            if (skyLightSection != null && !skyLightSection.isUninitialized) {
                sectionNbt.putByteArray(ChunkSerializer.SKY_LIGHT_KEY, skyLightSection.asByteArray())
            }
            if (sectionNbt.isEmpty) return@forEach
            sectionNbt.putByte("Y", y.toByte())
            sectionsList.add(sectionNbt)
        }

        nbt.put(ChunkSerializer.SECTIONS_KEY, sectionsList)

        if (chunk.isLightOn) {
            nbt.putBoolean(ChunkSerializer.IS_LIGHT_ON_KEY, true)
        }

        val entityNbt = NbtList()
        chunk.blockEntityPositions.mapNotNull { chunk.getPackedBlockEntityNbt(it) }.forEach { entityNbt.add(it) }

        nbt.put("block_entities", entityNbt)

        if (chunk.status.chunkType == ChunkStatus.ChunkType.PROTOCHUNK) {
            // TODO: Figure out if protochunks are needed
        }

        val tickSchedulers = chunk.tickSchedulers

        val time = chunk.world.levelProperties.time

        nbt.put("block_ticks", tickSchedulers.blocks().toNbt(time) { block: Block? ->
            Registries.BLOCK.getId(block).toString()
        })
        nbt.put("fluid_ticks", tickSchedulers.fluids().toNbt(time) { fluid: Fluid? ->
            Registries.FLUID.getId(fluid).toString()
        })

        nbt.put("PostProcessing", ChunkSerializer.toNbt(chunk.postProcessingLists))

        val heightMaps = NbtCompound()

        chunk.heightmaps.filter {
            chunk.status.heightmapTypes.contains(it.key)
        }.forEach { (key, value) ->
            heightMaps.put(key.getName(), NbtLongArray(value.asLongArray()))
        }

        nbt.put(ChunkSerializer.HEIGHTMAPS_KEY, heightMaps)

        // ToDo: no structureRegistry because of missing structure context, maybe create a custom structure context
//        val structures = NbtCompound()
//        val structureRegistry = chunk.world.registryManager.get(RegistryKeys.STRUCTURE)
//
//        chunk.structureStarts.forEach { (structure, structureStart) ->
//            structureRegistry.getId(structure)?.let { registryKey ->
//                structures.put(registryKey.toString(), structureStart.toNbt(registryKey, chunkPos))
//            }
//        }
//
//        nbt.put(STRUCTURES_KEY, structures)

        return nbt
    }

    private fun StructureStart.toNbt(identifier: Identifier, chunkPos: ChunkPos): NbtCompound {
        val structureNbt = NbtCompound()
        if (!hasChildren()) {
            structureNbt.putString("id", StructureStart.INVALID)
            return structureNbt
        }

        structureNbt.putString("id", identifier.toString())
        structureNbt.putInt("ChunkX", chunkPos.x)
        structureNbt.putInt("ChunkZ", chunkPos.z)
        structureNbt.putInt("references", references)

        val childrenStructures = NbtList()

        children.forEach {
            childrenStructures.add(it.toNbt())
        }

        structureNbt.put("Children", childrenStructures)

        return structureNbt
    }

    private fun StructurePiece.toNbt(): NbtCompound {
        val pieceNbt = NbtCompound()
        pieceNbt.putString("id", Registries.STRUCTURE_PIECE.getId(type).toString())
        BlockBox.CODEC.encodeStart(NbtOps.INSTANCE, boundingBox).resultOrPartial {
            LOGGER.error(it)
        }.ifPresent {
            pieceNbt.put("BB", it)
        }
        pieceNbt.putInt("O", facing?.horizontal ?: -1)
        pieceNbt.putInt("GD", chainLength)
        // ToDo: writeNbt() missing because of missing structure context

        return pieceNbt
    }
}