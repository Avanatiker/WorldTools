package org.waste.of.time.serializer

import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.mc
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object WorldSaveZipper {

    fun zipWorld(levelName: String) {
        LOG.info("Zipping $levelName")
        try {
            zipWorldInternal(
                mc.levelStorage.savesDirectory.resolve(levelName).toFile(),
                mc.levelStorage.savesDirectory.resolve("$levelName.zip")
            )
            LOG.info("Finished zipping $levelName")
        } catch (e: Exception) {
            LOG.error("Failed to zip $levelName", e)
        }
    }

    private fun zipWorldInternal(inputWorldDirectory: File, outputZipFile: Path) {
        ZipOutputStream(Files.newOutputStream(outputZipFile)).use { zipOut ->
            inputWorldDirectory.listFiles()?.forEach { file -> zipFile(file, file.name, zipOut) }
        }
    }

    private fun zipFile(fileToZip: File, fileName: String, zipOut: ZipOutputStream) {
        if (fileToZip.isHidden) return
        if (fileToZip.isDirectory) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(ZipEntry(fileName))
                zipOut.closeEntry()
            } else {
                zipOut.putNextEntry(ZipEntry("$fileName/"))
                zipOut.closeEntry()
            }
            fileToZip.listFiles()?.forEach { childFile -> zipFile(childFile, "$fileName/${childFile.name}", zipOut) }
            return
        }
        Files.newInputStream(fileToZip.toPath()).use { inputStream ->
            val zipEntry = ZipEntry(fileName)
            zipOut.putNextEntry(zipEntry)
            inputStream.copyTo(zipOut)
        }
    }
}
