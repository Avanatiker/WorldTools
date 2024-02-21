package org.waste.of.time.maps

import com.google.common.io.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.Vec3d
import net.minecraft.world.storage.RegionFile
import org.waste.of.time.Utils.chunkPosList
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.storage.WorldStorage
import java.nio.file.Path

object MapsRemapper {

    // TODO: error handling and checking if the remap is valid before writing anything
    //  i.e. don't remap onto an existing map ID
    //  decide if we should continue the remap if one step were to fail writing/reading NBT

    private fun verifyWorld(worldName: String, worldDirectoryPath: Path): Boolean {
        if (!worldDirectoryPath.toFile().exists()) {
            LOG.error("World $worldName does not exist")
            MessageManager.sendInfo("World $worldName does not exist")
            return false
        }
        if (isWorldCurrentlyOpen(worldName)) {
            MessageManager.sendError("World: $worldName is currently open")
            return false
        }
        return true
    }

    private fun verifyRemap(remap: Map<Int, Int>): Boolean {
        // todo: verify 1:1 relationships
        //  i.e. not remapping 2 map ID's to the same new map ID

        // todo: verify the map ID's actually exist?
        return true
    }

    fun remapMaps(worldName: String, remap: Map<Int, Int>) {
        val worldDirectoryPath = mc.levelStorage.savesDirectory.resolve(worldName)
        if (!verifyWorld(worldName, worldDirectoryPath)) return
        MessageManager.sendInfo("Remapping maps in $worldName")
        val ctx = MapScanContext(worldName, WorldStorage(worldDirectoryPath), remap)
        CoroutineScope(Dispatchers.IO).launch {
            scanMaps(ctx)
            val remapCount = ctx.foundMaps.count { ctx.remap.containsKey(it.mapId) }
            MessageManager.sendInfo("Remapped $remapCount map instances")
        }
    }

    fun findMaps(worldName: String) {
        val worldDirectoryPath = mc.levelStorage.savesDirectory.resolve(worldName)
        if (!verifyWorld(worldName, worldDirectoryPath)) return
        MessageManager.sendInfo("Finding maps in $worldName")
        val ctx = MapScanContext(worldName, WorldStorage(worldDirectoryPath), emptyMap())
        CoroutineScope(Dispatchers.IO).launch {
            scanMaps(ctx)
            val foundMaps = ctx.foundMaps
            // todo: write maps to CSV
            //  dedupe list on unique map id?
            val uniqueMaps = foundMaps
                .map { it.mapId }
                .distinct()
                .sorted()
            val uniqueMapCount = uniqueMaps
                .count()
            MessageManager.sendInfo("Found $uniqueMapCount map ID's: $uniqueMaps")
//            foundMaps.groupingBy { it.source.src }.eachCount().forEach {
//                MessageManager.sendInfo("Count: [${it.key}] ${it.value}")
//            }
//            foundMaps.groupingBy { it.source.src }.aggregate { key, accumulator: StringBuilder?, element, first ->
//                if (first)
//                    StringBuilder().append("[$key] ").append(element.mapId)
//                else
//                    accumulator!!.append(", ${element.mapId}")
//            }.forEach { (_, maps) ->
//                MessageManager.sendInfo("$maps")
//            }
        }
    }

    private suspend fun scanMaps(ctx: MapScanContext): MutableList<FoundMap> {
        val foundMaps = mutableListOf<FoundMap>()

        listMapsFromWorldData(ctx)
        ctx.storage.dimensionPaths.forEach { dimPath ->
            ctx.storage.getEntityStorage(dimPath).use { entityStorage ->
                entityStorage.regionFileFlow().collect { regionFile ->
                    listMapsInEntityRegion(regionFile, ctx)
                }
            }
            ctx.storage.getRegionStorage(dimPath).use { regionStorage ->
                regionStorage.regionFileFlow().collect { regionFile ->
                    listMapsInContainers(regionFile, ctx)
                }
            }
        }
        ctx.storage.playerDataStorageFlow().collect {
            val fileName = it.fileName.toString()
            if (fileName.endsWith(".dat")) {
                listMapsInPlayerData(it, ctx)
            }
        }
        listMapsInLevelDat(ctx)
        return foundMaps
    }

    private fun isWorldCurrentlyOpen(worldName: String): Boolean {
        return mc.isInSingleplayer && mc.server?.saveProperties?.levelName.equals(worldName)
    }

    private fun listMapsInLevelDat(ctx: MapScanContext) {
        val levelDatPath = ctx.storage.getLevelDatPath()
        val levelDatNbt = NbtIo.readCompressed(levelDatPath.toFile())
        val dataNbt = levelDatNbt.getCompound("Data")
        val playerDataNbt = dataNbt.getCompound("Player")
        val inventoryTagList = playerDataNbt.getList("Inventory", 10)
        val invDirty = searchInventory(inventoryTagList, MapSource(MapSourceId.LEVEL_DAT, "Inventory"), ctx)
        val enderChestTagList = playerDataNbt.getList("EnderItems", 10)
        val echestDirty = searchInventory(enderChestTagList, MapSource(MapSourceId.LEVEL_DAT, "Ender Chest"), ctx)
        if (invDirty || echestDirty) {
            NbtIo.writeCompressed(levelDatNbt, levelDatPath.toFile())
        }
    }

    private fun searchInventory(
        inventoryTagList: NbtList,
        mapSource: MapSource,
        ctx: MapScanContext
    ) : Boolean {
        var dirty = false
        inventoryTagList.forEach { inventorySlotTag ->
            val inventorySlotNbt = inventorySlotTag as NbtCompound
            val itemId = inventorySlotNbt.getString("id")
            if ("minecraft:filled_map" != itemId) return@forEach
            val itemNbtTag = inventorySlotNbt.getCompound("tag")
            val mapId = itemNbtTag.getInt("map")
            ctx.foundMaps.add(FoundMap(mapId, mapSource))
            if (ctx.remap.containsKey(mapId)) {
                itemNbtTag.putInt("map", ctx.remap[mapId]!!)
                dirty = true
            }
        }
        return dirty
    }

    private fun listMapsInPlayerData(nbtPath: Path, ctx: MapScanContext) {
        val nbtCompound = NbtIo.readCompressed(nbtPath.toFile()) ?: return
        val inventoryTagList = nbtCompound.getList("Inventory", 10)
        val invDirty = searchInventory(inventoryTagList, MapSource(MapSourceId.PLAYER_DATA, "Inventory"), ctx)
        val enderChestTagList = nbtCompound.getList("EnderItems", 10)
        val echestDirty = searchInventory(enderChestTagList, MapSource(MapSourceId.PLAYER_DATA, "Ender Chest"), ctx)
        if (invDirty || echestDirty) {
            NbtIo.writeCompressed(nbtCompound, nbtPath.toFile())
        }
    }

    private suspend fun listMapsFromWorldData(ctx: MapScanContext) {
        ctx.storage.worldDataStorageFlow().collect {
            val fileName = it.fileName.toString()
            if (fileName.startsWith("map_") && fileName.endsWith(".dat")) {
                val index1 = fileName.indexOfFirst { c -> c == '_' } + 1
                val index2 = fileName.indexOfLast { c -> c == '.' }
                val mapId = fileName.substring(index1, index2).toInt()
                ctx.foundMaps.add(FoundMap(mapId, MapSource(MapSourceId.WORLD_DATA)))
                if (ctx.remap.containsKey(mapId)) {
                    Files.move(it.toFile(), it.resolveSibling("map_${ctx.remap[mapId]!!}.dat").toFile())
                }
            }
        }
    }

    private fun listMapsInEntityRegion(regionFile: RegionFile, ctx: MapScanContext) {
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
                    val posNbt = entityNbt.getList("Pos", 6)
                    val x = posNbt.getDouble(0)
                    val y = posNbt.getDouble(1)
                    val z = posNbt.getDouble(2)
                    val itemFramesDirty = searchEntityNbtForMapsInItemFrames(entityNbt, id, x, y, z, ctx)
                    val itemEntitiesDirty = searchEntityNbtForMapsInItemEntities(entityNbt, id, x, y, z, ctx)
                    val entityInvDirty = searchEntityNbtForMapsInEntityInventories(entityNbt, id, x, y, z, ctx)
                    dirty = dirty || itemFramesDirty || itemEntitiesDirty || entityInvDirty
                    if (dirty) chunkNbt = nbt
                }
            }
            if (dirty) {
                chunkNbt?.let { nbt ->
                    regionFile.getChunkOutputStream(chunkPos).use { chunkOutputStream ->
                        NbtIo.write(nbt, chunkOutputStream)
                    }
                }
            }
        }
    }

    private fun searchEntityNbtForMapsInItemFrames(
        entityNbt: NbtCompound,
        id: String,
        x: Double,
        y: Double,
        z: Double,
        ctx: MapScanContext
    ) : Boolean {
        if ("minecraft:item_frame" != id) return false
        val itemTag = entityNbt.getCompound("Item")
        val itemId = itemTag.getString("id")
        if ("minecraft:filled_map" != itemId) return false
        val mapTag = itemTag.getCompound("tag")
        val mapId = mapTag.getInt("map")
        ctx.foundMaps.add(FoundMap(mapId, MapSource(MapSourceId.ITEM_FRAME, id, Vec3d(x, y, z))))
        if (ctx.remap.containsKey(mapId)) {
            mapTag.putInt("map", ctx.remap[mapId]!!)
            return true
        }
        return false
    }

    private fun searchEntityNbtForMapsInItemEntities(
        entityNbt: NbtCompound,
        id: String,
        x: Double,
        y: Double,
        z: Double,
        ctx: MapScanContext
    ) : Boolean {
        if ("minecraft:item" != id) return false
        val itemTag = entityNbt.getCompound("Item")
        val itemId = itemTag.getString("id")
        if ("minecraft:filled_map" != itemId) return false
        val mapTag = itemTag.getCompound("tag")
        val mapId = mapTag.getInt("map")
        ctx.foundMaps.add(FoundMap(mapId, MapSource(MapSourceId.ITEM_ENTITY, id, Vec3d(x, y, z))))
        if (ctx.remap.containsKey(mapId)) {
            mapTag.putInt("map", ctx.remap[mapId]!!)
            return true
        }
        return false
    }

    private fun searchEntityNbtForMapsInEntityInventories(
        entityNbt: NbtCompound,
        id: String,
        x: Double,
        y: Double,
        z: Double,
        ctx: MapScanContext
    ) : Boolean {
        if (!entityNbt.contains("Items", 10)) return false
        var dirty = false
        entityNbt.getList("Items", 10).forEach { itemTag ->
            val itemId = (itemTag as NbtCompound).getString("id")
            if ("minecraft:filled_map" != itemId) return@forEach
            val mapTag = itemTag.getCompound("tag")
            val mapId = mapTag.getInt("map")
            ctx.foundMaps.add(FoundMap(mapId, MapSource(MapSourceId.ENTITY_INVENTORY, id, Vec3d(x, y, z))))
            if (ctx.remap.containsKey(mapId)) {
                dirty = true
                mapTag.putInt("map", ctx.remap[mapId]!!)
            }
        }
        return dirty
    }

    private fun listMapsInContainers(regionFile: RegionFile, ctx: MapScanContext) {
        val chunkPosList = regionFile.chunkPosList()
        chunkPosList.forEach { chunkPos ->
            var dirty = false
            var chunkNbt: NbtCompound? = null
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
                        ctx.foundMaps.add(FoundMap(mapId, MapSource(MapSourceId.CONTAINER, containerBlockId, Vec3d(x.toDouble(), y.toDouble(), z.toDouble()))))
                        if (ctx.remap.containsKey(mapId)) {
                            mapTag.putInt("map", ctx.remap[mapId]!!)
                            dirty = true
                            chunkNbt = nbt
                        }
                    }
                }
            }
            if (dirty) {
                chunkNbt?.let { nbt ->
                    regionFile.getChunkOutputStream(chunkPos).use { chunkOutputStream ->
                        NbtIo.write(nbt, chunkOutputStream)
                    }
                }
            }
        }
    }
}
