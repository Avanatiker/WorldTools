package org.waste.of.time.maps

import org.waste.of.time.storage.WorldStorage

data class MapScanContext(val worldName: String, val storage: WorldStorage, val remap: Map<Int, Int>) {
    val foundMaps: MutableList<FoundMap> = mutableListOf()
}
