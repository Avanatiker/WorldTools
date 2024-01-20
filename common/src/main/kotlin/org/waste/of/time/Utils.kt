package org.waste.of.time

import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.Vec3d
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object Utils {
    // Why cant I use the std lib?
    fun Boolean.toByte(): Byte = if (this) 1 else 0
    fun Vec3d.asString() = "(%.2f, %.2f, %.2f)".format(x, y, z)
    fun NbtCompound.addAuthor() = apply { putString("Author", WorldTools.CREDIT_MESSAGE) }

    fun getTime(): String {
        val localDateTime = LocalDateTime.now()
        val zoneId = ZoneId.systemDefault()

        val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)
        val formatter = DateTimeFormatter.RFC_1123_DATE_TIME

        return zonedDateTime.format(formatter)
    }
}