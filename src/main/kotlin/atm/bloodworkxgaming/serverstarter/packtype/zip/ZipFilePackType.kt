package atm.bloodworkxgaming.serverstarter.packtype.zip

import atm.bloodworkxgaming.serverstarter.ServerStarter.Companion.LOGGER
import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import atm.bloodworkxgaming.serverstarter.packtype.AbstractZipbasedPackType
import atm.bloodworkxgaming.serverstarter.packtype.writeToFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipFilePackType(configFile: ConfigFile) : AbstractZipbasedPackType(configFile) {
    override fun cleanUrl(url: String): String {
        return url
    }

    override fun postProcessing() {
    }

    private var forgeVersion: String = configFile.install.loaderVersion
    private var mcVersion: String = configFile.install.mcVersion
    private val oldFiles = File(basePath + "OLD_TO_DELETE/")

    override fun getForgeVersion(): String {
        return forgeVersion
    }

    override fun getMCVersion(): String {
        return mcVersion
    }

    @Throws(IOException::class)
    override fun handleZip(file: File, pathMatchers: List<PathMatcher>) {
        // delete old installer folder
        FileUtils.deleteDirectory(oldFiles)

        LOGGER.info("Starting to unzip files.")
        // unzip start
        try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry

                loop@ while (entry != null) {
                    LOGGER.info("Entry in zip: $entry", true)
                    val name = entry.name

                    // overrides
                    val path = entry.name

                    when {
                        pathMatchers.any { it.matches(Paths.get(path)) } ->
                            LOGGER.info("Skipping $path as it is on the ignore List.", true)


                        !name.endsWith("/") -> {
                            val outfile = File(basePath + path)
                            LOGGER.info("Copying zip entry to = $outfile", true)


                            outfile.parentFile?.mkdirs()
                            zis.writeToFile(outfile)
                        }

                        else -> {
                            val newFolder = File(basePath + path)
                            if (newFolder.exists())
                                FileUtils.moveDirectory(newFolder, File(oldFiles, path))

                            LOGGER.info("Folder moved: " + newFolder.absolutePath, true)
                        }
                    }


                    entry = zis.nextEntry
                }


                zis.closeEntry()
            }
        } catch (e: IOException) {
            LOGGER.error("Could not unzip files", e)
        }

        LOGGER.info("Done unzipping the files.")
    }
}