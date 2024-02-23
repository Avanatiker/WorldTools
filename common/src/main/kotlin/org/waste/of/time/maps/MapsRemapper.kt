package org.waste.of.time.maps

import com.google.common.io.Files
import kotlinx.coroutines.CoroutineExceptionHandler
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

    fun remapMaps(worldName: String, remap: Map<Int, Int>, force: Boolean = false) {
        val worldDirectoryPath = mc.levelStorage.savesDirectory.resolve(worldName)
        if (!verifyWorld(worldName, worldDirectoryPath)) return
        MessageManager.sendInfo("Remapping maps in $worldName")
        CoroutineScope(Dispatchers.IO).launch(mapScanErrorHandler) {
            val storage = WorldStorage(worldDirectoryPath)
            if (!force && !verifyRemap(worldName, storage, remap)) return@launch
            val ctx = MapScanContext(worldName, storage, remap)
            scanMaps(ctx)
            val remapCount = ctx.foundMaps.count { ctx.remap.containsKey(it.mapId) }
            MessageManager.sendInfo("Remapped $remapCount maps")
        }
    }

    fun remapMapsWithRemapFile(worldName: String) {
        val worldDirectoryPath = mc.levelStorage.savesDirectory.resolve(worldName)
        if (!verifyWorld(worldName, worldDirectoryPath)) return
        val storage = WorldStorage(worldDirectoryPath)
        val remap = MapRemapSerializer.deserializeRemaps(storage)
        MessageManager.sendInfo("Loaded ${remap.size} remaps from disk")
        remapMaps(worldName, remap)
    }

    fun findMaps(worldName: String) {
        val worldDirectoryPath = mc.levelStorage.savesDirectory.resolve(worldName)
        if (!verifyWorld(worldName, worldDirectoryPath)) return
        MessageManager.sendInfo("Finding maps in $worldName")
        val ctx = MapScanContext(worldName, WorldStorage(worldDirectoryPath))
        CoroutineScope(Dispatchers.IO).launch(mapScanErrorHandler) {
            scanMaps(ctx)
            val foundMaps = ctx.foundMaps
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
            foundMaps.groupingBy { it.source.src }.aggregate { key, accumulator: StringBuilder?, element, first ->
                if (first)
                    StringBuilder().append("[$key] ").append(element.mapId)
                else
                    accumulator!!.append(", ${element.mapId}")
            }.forEach { (_, maps) ->
                MessageManager.sendInfo("$maps")
            }
            MapRemapSerializer.serialize(ctx)
        }
    }

    private val mapScanErrorHandler = CoroutineExceptionHandler { _, exception ->
        MessageManager.sendError("Error during map scan: $exception")
        LOG.error("Error during map scan", exception)
    }

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

    private suspend fun verifyRemap(worldName: String, storage: WorldStorage, remap: Map<Int, Int>): Boolean {
        if (remap.values.distinct().size != remap.size) {
            MessageManager.sendError("Duplicate remap ID")
            return false
        }

        //  we have to find existing maps to determine if remap is valid T.T
        val ctx = MapScanContext(worldName, storage)
        scanMaps(ctx)

        // verify the existing map ID's actually exist
        val notFoundExistingIds = remap.keys.filter { ctx.foundMaps.all { foundMap -> foundMap.mapId != it } }

        if (notFoundExistingIds.isNotEmpty()) {
            MessageManager.sendError("Map ID's not found: $notFoundExistingIds")
            return false
        }
        // verify there are no maps with new ID's
        val newIdsFound = remap.values.filter { ctx.foundMaps.any { foundMap -> foundMap.mapId == it } }
        if (newIdsFound.isNotEmpty()) {
            MessageManager.sendError("Map ID's already exist: $newIdsFound")
            return false
        }
        return true
    }

    private suspend fun scanMaps(ctx: MapScanContext): MutableList<FoundMap> {
        val foundMaps = mutableListOf<FoundMap>()
        listMapsFromWorldData(ctx)
        ctx.storage.dimensionPaths.forEach { dimPath ->
            ctx.storage.getEntityStorage(dimPath).use { entityStorage ->
                entityStorage.regionFileFlow().collect { regionFile ->
                    regionFile.use {
                        listMapsInEntityRegion(regionFile, ctx)
                    }
                }
            }
            ctx.storage.getRegionStorage(dimPath).use { regionStorage ->
                regionStorage.regionFileFlow().collect { regionFile ->
                    regionFile.use {
                        listMapsInContainers(regionFile, ctx)
                    }
                }
            }
        }
        ctx.storage.playerDataStorageFlow().collect {
            searchPlayerDataMaps(it, ctx)
        }
        searchLevelDatMaps(ctx)
        return foundMaps
    }

    private fun isWorldCurrentlyOpen(worldName: String): Boolean {
        return mc.isInSingleplayer && mc.server?.saveProperties?.levelName.equals(worldName)
    }

    private fun searchLevelDatMaps(ctx: MapScanContext) {
        val levelDatPath = ctx.storage.getLevelDatPath()
        val levelDatNbt = NbtIo.readCompressed(levelDatPath.toFile()) ?: return
        val playerDataNbt = levelDatNbt.getCompound("Data").getCompound("Player")
        if (searchPlayerDataMapsBase(playerDataNbt, MapSourceId.LEVEL_DAT, ctx)) {
            NbtIo.writeCompressed(levelDatNbt, levelDatPath.toFile())
        }
    }

    private fun searchPlayerDataMaps(nbtPath: Path, ctx: MapScanContext) {
        val nbt = NbtIo.readCompressed(nbtPath.toFile()) ?: return
        if (searchPlayerDataMapsBase(nbt, MapSourceId.PLAYER_DATA, ctx)) {
            NbtIo.writeCompressed(nbt, nbtPath.toFile())
        }
    }

    private fun searchPlayerDataMapsBase(nbt: NbtCompound, mapSourceId: MapSourceId, ctx: MapScanContext): Boolean {
        return searchInventory(nbt.getList("Inventory", 10), MapSource(mapSourceId, "Inventory"), ctx) or
                searchInventory(nbt.getList("EnderItems", 10), MapSource(mapSourceId, "Ender Chest"), ctx)
    }

    private suspend fun listMapsFromWorldData(ctx: MapScanContext) {
        ctx.storage.worldDataStorageFlow().collect {
            val fileName = it.fileName.toString()
            if (fileName.startsWith("map_") && fileName.endsWith(".dat")) {
                val mapId = fileName.substring( // map_123.dat -> 123
                    fileName.indexOfFirst { c -> c == '_' } + 1,
                    fileName.indexOfLast { c -> c == '.' })
                    .toInt()
                ctx.foundMaps.add(FoundMap(mapId, MapSource(MapSourceId.WORLD_DATA)))
                if (ctx.remap.containsKey(mapId)) {
                    Files.move(it.toFile(), it.resolveSibling("map_${ctx.remap[mapId]!!}.dat").toFile())
                }
            }
        }
    }

    private fun listMapsInEntityRegion(regionFile: RegionFile, ctx: MapScanContext) {
        regionFile.chunkPosList().forEach { chunkPos ->
            var dirty = false
            var chunkNbt: NbtCompound?
            regionFile.getChunkInputStream(chunkPos).use {
                val nbt = NbtIo.read(it)
                chunkNbt = nbt
                val entities = nbt.getList("Entities", 10)
                entities.forEach { entity ->
                    val entityNbt = entity as NbtCompound
                    val id = entityNbt.getString("id")
                    val posNbt = entityNbt.getList("Pos", 6)
                    val pos = Vec3d(
                        posNbt.getDouble(0),
                        posNbt.getDouble(1),
                        posNbt.getDouble(2)
                    )
                    dirty = dirty or
                            searchEntityNbtForMapsInItemFrames(entityNbt, id, pos, ctx) or
                            searchEntityNbtForMapsInItemEntities(entityNbt, id, pos, ctx) or
                            searchEntityNbtForMapsInEntityInventories(entityNbt, id, pos, ctx)
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
        pos: Vec3d,
        ctx: MapScanContext
    ) : Boolean {
        if ("minecraft:item_frame" != id) return false
        return searchItemNbtForMap(entityNbt.getCompound("Item"), MapSource(MapSourceId.ITEM_FRAME, id, pos), ctx)
    }

    private fun searchItemNbtForMap(nbt: NbtCompound, mapSource: MapSource, ctx: MapScanContext): Boolean {
        if ("minecraft:filled_map" != nbt.getString("id")) return false
        val mapTag = nbt.getCompound("tag")
        val mapId = mapTag.getInt("map")
        ctx.foundMaps.add(FoundMap(mapId, mapSource))
        if (ctx.remap.containsKey(mapId)) {
            mapTag.putInt("map", ctx.remap[mapId]!!)
            return true
        }
        return false
    }

    private fun searchEntityNbtForMapsInItemEntities(
        entityNbt: NbtCompound,
        id: String,
        pos: Vec3d,
        ctx: MapScanContext
    ) : Boolean {
        if ("minecraft:item" != id) return false
        return searchItemNbtForMap(entityNbt.getCompound("Item"), MapSource(MapSourceId.ITEM_ENTITY, id, pos), ctx)
    }

    private fun searchEntityNbtForMapsInEntityInventories(
        entityNbt: NbtCompound,
        id: String,
        pos: Vec3d,
        ctx: MapScanContext
    ) : Boolean {
        return searchInventory(
            // todo: there may be other nbt tags to search on different entity types
            entityNbt.getList("Inventory", 10), // entities that implement InventoryOwner
            MapSource(MapSourceId.ENTITY_INVENTORY, id, pos),
            ctx
        )
    }

    private fun searchInventory(
        inventoryTagList: NbtList,
        mapSource: MapSource,
        ctx: MapScanContext
    ) : Boolean {
        var dirty = false
        inventoryTagList.forEach { inventorySlotTag ->
            dirty = dirty or searchItemNbtForMap(inventorySlotTag as NbtCompound, mapSource, ctx)
        }
        return dirty
    }

    private fun listMapsInContainers(regionFile: RegionFile, ctx: MapScanContext) {
        regionFile.chunkPosList().forEach { chunkPos ->
            var dirty = false
            var chunkNbt: NbtCompound?
            regionFile.getChunkInputStream(chunkPos).use {
                val nbt = NbtIo.read(it)
                chunkNbt = nbt
                val blockEntities = nbt.getList("block_entities", 10)
                blockEntities.forEach blockEntityLoop@ { blockEntity ->
                    val blockEntityNbt = blockEntity as NbtCompound
                    val containerBlockId = blockEntityNbt.getString("id")
                    val pos = Vec3d(
                        blockEntityNbt.getInt("x").toDouble(),
                        blockEntityNbt.getInt("y").toDouble(),
                        blockEntityNbt.getInt("z").toDouble()
                    )
                    if (!blockEntityNbt.contains("Items")) return@blockEntityLoop
                    val itemsListTag = blockEntityNbt.getList("Items", 10)
                    dirty = dirty or
                            searchInventory(itemsListTag, MapSource(MapSourceId.CONTAINER, containerBlockId, pos), ctx)
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
