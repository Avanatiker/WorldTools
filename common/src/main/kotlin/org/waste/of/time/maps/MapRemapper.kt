package org.waste.of.time.maps

import com.google.common.io.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.world.storage.RegionFile
import org.waste.of.time.Utils.chunkPosList
import org.waste.of.time.WorldTools
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.storage.WorldStorage
import java.nio.file.Path

object MapRemapper {

    fun remap(worldName: String, existingId: Int, newId: Int) {
        val worldDirectoryPath = WorldTools.mc.levelStorage.savesDirectory.resolve(worldName)
        if (!worldDirectoryPath.toFile().exists()) {
            WorldTools.LOG.error("World $worldName does not exist")
            MessageManager.sendInfo("World $worldName does not exist")
            return
        }
        // todo: Fail if we have this world currently open in-game
        // todo: Enforce there is only one remap/search job running concurrently

        MessageManager.sendInfo("Remapping map $existingId to $newId in $worldName")
        val storage = WorldStorage(worldDirectoryPath)

        CoroutineScope(Dispatchers.IO).launch {
            remapWorldDataMaps(storage, existingId, newId)
            remapLevelDatMaps(storage, existingId, newId)
            remapPlayerDataMaps(storage, existingId, newId)
            storage.dimensionPaths.forEach { dimPath ->
                storage.getEntityStorage(dimPath).use { entityStorage ->
                    entityStorage.regionFileFlow().collect { regionFile ->
                        remapEntityRegionFile(regionFile, existingId, newId)
                    }
                }
                storage.getRegionStorage(dimPath).use { regionStorage ->
                    regionStorage.regionFileFlow().collect { regionFile ->
                        remapWorldDataRegionFile(regionFile, existingId, newId)
                    }
                }
            }
        }
    }

    private fun remapEntityRegionFile(regionFile: RegionFile, existingId: Int, newId: Int) {
        val chunkPosList = regionFile.chunkPosList()
        chunkPosList.forEach { chunkPos ->
            var dirty = false
            var chunkNbt: NbtCompound? = null
            regionFile.getChunkInputStream(chunkPos).use {
                val nbt = NbtIo.read(it)
                val entities = nbt.getList("Entities", 10)
                entities.forEach { entity ->
                    val entityNbt = entity as NbtCompound
                    val id = entityNbt.getString("id")
                    dirty = remapEntityNbtForMapsInItemFrames(entityNbt, id, existingId, newId)
                            || remapEntityNbtForMapsInItemEntities(entityNbt, id, existingId, newId)
                            || remapEntityNbtForMapsInEntityInventories(entityNbt, existingId, newId)
                    if (dirty) chunkNbt = nbt
                }
            }
            if (dirty) {
                chunkNbt?.let { nbt ->
                    regionFile.getChunkOutputStream(chunkPos).use { chunkOutputStream ->
                        NbtIo.write(nbt, chunkOutputStream)
                    }
                }
                MessageManager.sendInfo("Map $existingId remapped to $newId in entity chunk $chunkPos")
            }
        }
    }

    private fun remapEntityNbtForMapsInEntityInventories(
        entityNbt: NbtCompound,
        existingId: Int,
        newId: Int
    ): Boolean {
        if (!entityNbt.contains("Items", 10)) return false
        var dirty = false
        entityNbt.getList("Items", 10).forEach { itemTag ->
            val itemId = (itemTag as NbtCompound).getString("id")
            if ("minecraft:filled_map" != itemId) return@forEach
            val mapTag = itemTag.getCompound("tag")
            val mapId = mapTag.getInt("map")
            if (mapId != existingId) return@forEach
            mapTag.putInt("map", newId)
            dirty = true
        }
        return dirty
    }

    private fun remapEntityNbtForMapsInItemEntities(
        entityNbt: NbtCompound,
        id: String,
        existingId: Int,
        newId: Int
    ): Boolean {
        if ("minecraft:item" != id) return false
        val itemTag = entityNbt.getCompound("Item")
        val itemId = itemTag.getString("id")
        if ("minecraft:filled_map" != itemId) return false
        val mapTag = itemTag.getCompound("tag")
        val mapId = mapTag.getInt("map")
        if (mapId != existingId) return false
        mapTag.putInt("map", newId)
        return true
    }

    private fun remapEntityNbtForMapsInItemFrames(
        entityNbt: NbtCompound,
        id: String,
        existingId: Int,
        newId: Int
    ): Boolean {
        if ("minecraft:item_frame" != id) return false
        val itemTag = entityNbt.getCompound("Item")
        val itemId = itemTag.getString("id")
        if ("minecraft:filled_map" != itemId) return false
        val mapTag = itemTag.getCompound("tag")
        val mapId = mapTag.getInt("map")
        if (mapId != existingId) return false
        mapTag.putInt("map", newId)
        return true
    }

    private fun remapWorldDataRegionFile(regionFile: RegionFile, existingId: Int, newId: Int) {
        val chunkPosList = regionFile.chunkPosList()
        chunkPosList.forEach { chunkPos ->
            var dirty = false
            var chunkNbt: NbtCompound? = null
            regionFile.getChunkInputStream(chunkPos).use {
                val nbt = NbtIo.read(it)
                val blockEntities = nbt.getList("block_entities", 10)
                blockEntities.forEach blockEntityLoop@ { blockEntity ->
                    val blockEntityNbt = blockEntity as NbtCompound
                    if (!blockEntityNbt.contains("Items")) return@blockEntityLoop
                    val itemsListTag = blockEntityNbt.getList("Items", 10)
                    itemsListTag.forEach itemsLoop@ { itemCompoundTag ->
                        val itemTag = itemCompoundTag as NbtCompound
                        val itemId = itemTag.getString("id")
                        if ("minecraft:filled_map" != itemId) return@itemsLoop
                        val mapTag = itemTag.getCompound("tag")
                        val mapId = mapTag.getInt("map")
                        if (mapId != existingId) return@itemsLoop
                        mapTag.putInt("map", newId)
                        dirty = true
                        chunkNbt = nbt
                    }
                }
            }
            if (dirty) {
                chunkNbt?.let { nbt ->
                    regionFile.getChunkOutputStream(chunkPos).use { chunkOutputStream ->
                        NbtIo.write(nbt, chunkOutputStream)
                    }
                }
                MessageManager.sendInfo("Map $existingId remapped to $newId in chunk $chunkPos")
            }
        }
    }

    private suspend fun remapPlayerDataMaps(storage: WorldStorage, existingId: Int, newId: Int) {
        storage.playerDataStorageFlow().collect {
            val fileName = it.fileName.toString()
            if (fileName.endsWith(".dat")) {
                remapPlayerData(it, existingId, newId)
            }
        }
    }

    private fun remapPlayerData(nbtPath: Path, existingId: Int, newId: Int) {
        // TODO: Search Ender chest data
        //  WorldTools does not write this currently though
        val nbtCompound = NbtIo.readCompressed(nbtPath.toFile()) ?: return
        val inventoryTagList = nbtCompound.getList("Inventory", 10)
        var dirty = false
        inventoryTagList.forEach { inventorySlotTag ->
            val inventorySlotNbt = inventorySlotTag as NbtCompound
            val itemId = inventorySlotNbt.getString("id")
            if ("minecraft:filled_map" != itemId) return@forEach
            val itemNbtTag = inventorySlotNbt.getCompound("tag")
            val mapId = itemNbtTag.getInt("map")
            if (mapId != existingId) return@forEach
            itemNbtTag.putInt("map", newId)
            dirty = true
        }
        if (dirty) {
            NbtIo.writeCompressed(nbtCompound, nbtPath.toFile())
            MessageManager.sendInfo("Map $existingId remapped to $newId in $nbtPath")
        }
    }

    private fun remapLevelDatMaps(storage: WorldStorage, existingId: Int, newId: Int) {
        val levelDatPath = storage.getLevelDatPath()
        val levelDatNbt = NbtIo.readCompressed(levelDatPath.toFile())
        val dataNbt = levelDatNbt.getCompound("Data")
        val playerDataNbt = dataNbt.getCompound("Player")
        val inventoryTagList = playerDataNbt.getList("Inventory", 10)
        var dirty = false
        inventoryTagList.forEach { inventorySlotTag ->
            val inventorySlotNbt = inventorySlotTag as NbtCompound
            val itemId = inventorySlotNbt.getString("id")
            if ("minecraft:filled_map" != itemId) return@forEach
            val itemNbtTag = inventorySlotNbt.getCompound("tag")
            val mapId = itemNbtTag.getInt("map")
            if (mapId != existingId) return@forEach
            itemNbtTag.putInt("map", newId)
            dirty = true
        }
        if (dirty) {
            NbtIo.writeCompressed(levelDatNbt, levelDatPath.toFile())
            MessageManager.sendInfo("Map $existingId remapped to $newId in level.dat")
        }
    }

    private suspend fun remapWorldDataMaps(storage: WorldStorage, existingId: Int, newId: Int) {
        val existingIdMap: MutableMap<Int, Path> = mutableMapOf()
        storage.worldDataStorageFlow().collect {
            val fileName = it.fileName.toString()
            if (fileName.startsWith("map_") && fileName.endsWith(".dat")) {
                val index1 = fileName.indexOfFirst { c -> c == '_' } + 1
                val index2 = fileName.indexOfLast { c -> c == '.' }
                val mapId = fileName.substring(index1, index2).toInt()
                existingIdMap[mapId] = it
            }
        }
        if (!existingIdMap.contains(existingId)) {
            MessageManager.sendInfo("Map $existingId not found in world data")
            return
        }
        if (existingIdMap.contains(newId)) {
            MessageManager.sendInfo("Map $newId already exists in world data")
            return
        }
        existingIdMap[existingId]?.let {
            Files.move(it.toFile(), it.resolveSibling("map_$newId.dat").toFile())
            MessageManager.sendInfo("Map $existingId remapped to $newId in world data")
        }
    }
}
