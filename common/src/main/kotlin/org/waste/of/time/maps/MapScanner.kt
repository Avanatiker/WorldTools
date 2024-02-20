package org.waste.of.time.maps

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.world.storage.RegionFile
import org.waste.of.time.Utils.chunkPosList
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.storage.WorldStorage
import java.nio.file.Path

// todo: Merge with MapRemapper to reduce duplicate code
object MapScanner {

    // todo: Return results in a useable form so we can display them in a UI

    fun findMaps(worldName: String) {
        val worldDirectoryPath = mc.levelStorage.savesDirectory.resolve(worldName)
        if (!worldDirectoryPath.toFile().exists()) {
            LOG.error("World $worldName does not exist")
            MessageManager.sendInfo("World $worldName does not exist")
            return
        }

        MessageManager.sendInfo("Listing maps for $worldName")
        val storage = WorldStorage(worldDirectoryPath)
        CoroutineScope(Dispatchers.IO).launch {
            listMapsFromWorldData(storage)
            storage.dimensionPaths.forEach { dimPath ->
                storage.getEntityStorage(dimPath).use { entityStorage ->
                    entityStorage.regionFileFlow().collect { regionFile ->
                        listMapsInEntityRegion(regionFile)
                    }
                }
                storage.getRegionStorage(dimPath).use { regionStorage ->
                    regionStorage.regionFileFlow().collect { regionFile ->
                        listMapsInContainers(regionFile)
                    }
                }
            }
            storage.playerDataStorageFlow().collect {
                val fileName = it.fileName.toString()
                if (fileName.endsWith(".dat")) {
                    listMapsInPlayerData(it)
                }
            }
            listMapsInLevelDat(storage)
        }
    }

    private fun listMapsInLevelDat(storage: WorldStorage) {
        val levelDatPath = storage.getLevelDatPath()
        val levelDatNbt = NbtIo.readCompressed(levelDatPath.toFile())
        val dataNbt = levelDatNbt.getCompound("Data")
        val playerDataNbt = dataNbt.getCompound("Player")
        val inventoryTagList = playerDataNbt.getList("Inventory", 10)
        inventoryTagList.forEach { inventorySlotTag ->
            val inventorySlotNbt = inventorySlotTag as NbtCompound
            val itemId = inventorySlotNbt.getString("id")
            if ("minecraft:filled_map" != itemId) return@forEach
            val itemNbtTag = inventorySlotNbt.getCompound("tag")
            val mapId = itemNbtTag.getInt("map")
            MessageManager.sendInfo("Found map ID: $mapId in player inventory in level.dat")
        }
    }

    private fun listMapsInPlayerData(nbtPath: Path) {
        try {
            // TODO: Search Ender chest data
            //  WorldTools does not write this currently though
            val nbtCompound = NbtIo.readCompressed(nbtPath.toFile()) ?: return
            val inventoryTagList = nbtCompound.getList("Inventory", 10)
            inventoryTagList.forEach { inventorySlotTag ->
                val inventorySlotNbt = inventorySlotTag as NbtCompound
                val itemId = inventorySlotNbt.getString("id")
                if ("minecraft:filled_map" != itemId) return@forEach
                val itemNbtTag = inventorySlotNbt.getCompound("tag")
                val mapId = itemNbtTag.getInt("map")
                MessageManager.sendInfo("Found map ID: $mapId in player inventory: ${nbtPath.fileName}")
            }
        } catch (e: Exception) {
            LOG.error("Failed searching $nbtPath", e)
        }
    }

    private suspend fun listMapsFromWorldData(storage: WorldStorage) {
        storage.worldDataStorageFlow().collect {
            val fileName = it.fileName.toString()
            if (fileName.startsWith("map_") && fileName.endsWith(".dat")) {
                val index1 = fileName.indexOfFirst { c -> c == '_' } + 1
                val index2 = fileName.indexOfLast { c -> c == '.' }
                val mapId = fileName.substring(index1, index2).toInt()
                MessageManager.sendInfo("Found map ID: $mapId")
            }
        }
    }

    private fun listMapsInEntityRegion(regionFile: RegionFile) {
        val chunkPosList = regionFile.chunkPosList()
        chunkPosList.forEach { chunkPos ->
            regionFile.getChunkInputStream(chunkPos).use {
                val nbt = NbtIo.read(it)
                val entities = nbt.getList("Entities", 10)
                entities.forEach { entity ->
                    val entityNbt = entity as NbtCompound
                    val id = entityNbt.getString("id")
                    val posNbt = entityNbt.getList("Pos", 6)
                    val x = posNbt.getDouble(0)
                    val y = posNbt.getDouble(1)
                    val z = posNbt.getDouble(2)
                    searchEntityNbtForMapsInItemFrames(entityNbt, id, x, y, z)
                    searchEntityNbtForMapsInItemEntities(entityNbt, id, x, y, z)
                    searchEntityNbtForMapsInEntityInventories(entityNbt, id, x, y, z)
                }
            }
        }
    }

    private fun searchEntityNbtForMapsInItemFrames(entityNbt: NbtCompound, id: String, x: Double, y: Double, z: Double) {
        if ("minecraft:item_frame" != id) return
        val itemTag = entityNbt.getCompound("Item")
        val itemId = itemTag.getString("id")
        if ("minecraft:filled_map" != itemId) return
        val mapTag = itemTag.getCompound("tag")
        val mapId = mapTag.getInt("map")
        MessageManager.sendInfo("Found MapId: $mapId in item frame at: ($x, $y, $z)")
    }

    private fun searchEntityNbtForMapsInItemEntities(entityNbt: NbtCompound, id: String, x: Double, y: Double, z: Double) {
        if ("minecraft:item" != id) return
        val itemTag = entityNbt.getCompound("Item")
        val itemId = itemTag.getString("id")
        if ("minecraft:filled_map" != itemId) return
        val mapTag = itemTag.getCompound("tag")
        val mapId = mapTag.getInt("map")
        MessageManager.sendInfo("Found MapId: $mapId in item entity at: ($x, $y, $z)")
    }

    private fun searchEntityNbtForMapsInEntityInventories(entityNbt: NbtCompound, id: String, x: Double, y: Double, z: Double) {
        if (!entityNbt.contains("Items", 10)) return
        entityNbt.getList("Items", 10).forEach { itemTag ->
            val itemId = (itemTag as NbtCompound).getString("id")
            if ("minecraft:filled_map" != itemId) return@forEach
            val mapTag = itemTag.getCompound("tag")
            val mapId = mapTag.getInt("map")
            MessageManager.sendInfo("Found MapId: $mapId in $id inventory at: ($x, $y, $z)")
        }
    }

    private fun listMapsInContainers(regionFile: RegionFile) {
        val chunkPosList = regionFile.chunkPosList()
        chunkPosList.forEach { chunkPos ->
            regionFile.getChunkInputStream(chunkPos).use {
                val nbt = NbtIo.read(it)
                val blockEntities = nbt.getList("block_entities", 10)
                blockEntities.forEach blockEntityLoop@ { blockEntity ->
                    val blockEntityNbt = blockEntity as NbtCompound
                    val containerBlockId = blockEntityNbt.getString("id")
                    val x = blockEntityNbt.getInt("x")
                    val y = blockEntityNbt.getInt("y")
                    val z = blockEntityNbt.getInt("z")
                    if (!blockEntityNbt.contains("Items")) return@blockEntityLoop
                    val itemsListTag = blockEntityNbt.getList("Items", 10)
                    itemsListTag.forEach itemsLoop@ { itemCompoundTag ->
                        val itemTag = itemCompoundTag as NbtCompound
                        val itemId = itemTag.getString("id")
                        if ("minecraft:filled_map" != itemId) return@blockEntityLoop
                        val mapTag = itemTag.getCompound("tag")
                        val mapId = mapTag.getInt("map")
                        MessageManager.sendInfo("Found MapId: $mapId in container $containerBlockId at: ($x, $y, $z)")
                    }

                }
            }
        }
    }
}
