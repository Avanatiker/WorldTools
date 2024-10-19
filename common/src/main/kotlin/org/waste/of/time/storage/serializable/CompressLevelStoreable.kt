package org.waste.of.time.storage.serializable

import net.minecraft.text.MutableText
import net.minecraft.util.WorldSavePath
import net.minecraft.world.level.storage.LevelStorage
import org.waste.of.time.Utils.toReadableByteCount
import org.waste.of.time.WorldTools.LOG
import org.waste.of.time.WorldTools.config
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.manager.BarManager
import org.waste.of.time.manager.CaptureManager.currentLevelName
import org.waste.of.time.manager.MessageManager
import org.waste.of.time.manager.MessageManager.translateHighlight
import org.waste.of.time.storage.CustomRegionBasedStorage
import org.waste.of.time.storage.StorageFlow
import org.waste.of.time.storage.Storeable
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.name

class CompressLevelStoreable : Storeable() {
    private val zipName: String get() = "$currentLevelName-${System.currentTimeMillis()}.zip"

    override fun shouldStore() = config.general.compressLevel

    override val verboseInfo: MutableText
        get() = translateHighlight("worldtools.capture.saved.compressed", zipName)

    override val anonymizedInfo: MutableText
        get() = verboseInfo

    override fun store(
        session: LevelStorage.Session,
        cachedStorages: MutableMap<String, CustomRegionBasedStorage>
    ) {
        val rootPath = session.getDirectory(WorldSavePath.ROOT)
        val zipPath = mc.levelStorage.savesDirectory.resolve(zipName)
        LOG.info("Zipping $rootPath to $zipPath")

        val totalSize = Files.walk(rootPath).filter { Files.isRegularFile(it) }.mapToLong { Files.size(it) }.sum()

        try {
            var totalZippedSize = 0L

            Files.newOutputStream(zipPath).use { outStream ->
                ZipOutputStream(outStream).use { zipOut ->
                    Files.walkFileTree(rootPath, object : SimpleFileVisitor<Path>() {
                        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                            zipFile(file, rootPath, zipOut)

                            totalZippedSize += Files.size(file)
                            val progress = totalZippedSize.toDouble() / totalSize
                            StorageFlow.lastStoredTimestamp = System.currentTimeMillis()
                            BarManager.progressBar.percent = progress.toFloat()
                            if (config.debug.logZippingProgress) {
                                LOG.info("${"%.2f".format(progress * 100)}% (${totalZippedSize.toReadableByteCount()}/${totalSize.toReadableByteCount()}) Zipping file ${file.name} with size ${Files.size(file).toReadableByteCount()}")
                            }
                            return FileVisitResult.CONTINUE
                        }

                        override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                            MessageManager.sendError("worldtools.log.error.failed_to_visit_file", file, exc.localizedMessage)
                            return FileVisitResult.CONTINUE
                        }
                    })
                }
            }
            LOG.info("Finished zipping $rootPath with size ${totalZippedSize.toReadableByteCount()} to ${zipPath.toAbsolutePath()} with size ${Files.size(zipPath).toReadableByteCount()}")
        } catch (e: IOException) {
            MessageManager.sendError("worldtools.log.error.failed_to_zip", rootPath, e.localizedMessage)
        }
    }

    @Throws(IOException::class)
    private fun zipFile(fileToZip: Path, rootPath: Path, zipOut: ZipOutputStream) {
        if (fileToZip.fileName.toString().contains("session.lock")) return
        val entryName = rootPath.relativize(fileToZip).toString().replace('\\', '/')
        when {
            Files.isHidden(fileToZip) -> return
            Files.isDirectory(fileToZip) -> {
                zipOut.putNextEntry(ZipEntry("$entryName/"))
                zipOut.closeEntry()
            }
            else -> {
                Files.newInputStream(fileToZip).use { inputStream ->
                    zipOut.putNextEntry(ZipEntry(entryName))
                    inputStream.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
        }
    }
}
