package org.waste.of.time.maps

import net.minecraft.util.math.Vec3d

data class MapSource(
    val src: MapSourceId,
    val srcId: String? = null, // i.e. the entity or container identifier the map was found in
    val pos: Vec3d? = null
) {}
