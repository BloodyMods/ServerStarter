package atm.bloodworkxgaming.serverstarter.packtype.zip

import atm.bloodworkxgaming.serverstarter.ServerStarter.Companion.LOGGER
import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import atm.bloodworkxgaming.serverstarter.packtype.IPackType
import org.apache.commons.io.FileUtils
import java.io.*
import java.nio.file.*
import java.util.zip.*

class ZipFilePackType(private val configFile: ConfigFile) : IPackType {

    private val basePath = configFile.install.baseInstallPath
    private var forgeVersion: String = configFile.install.forgeVersion
    private var mcVersion: String = configFile.install.mcVersion
    private val oldFiles = File(basePath + "OLD_TO_DELETE/")


    override fun installPack() {
        try {
            val patterns = configFile.install.ignoreFiles
                    .map {
                        val s = if (it.startsWith("glob:") || it.startsWith("regex:"))
                            it
                        else
                            "glob:$it"

                        FileSystems.getDefault().getPathMatcher(s)
                    }

            unzipFile(File(configFile.install.modpackUrl), patterns)
            // unzipFile(new File(basePath + "modpack-download.zip"));

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getForgeVersion(): String {
        return forgeVersion
    }

    override fun getMCVersion(): String {
        return mcVersion
    }

    @Throws(IOException::class)
    private fun unzipFile(downloadedPack: File, patterns: List<PathMatcher>) {
        // delete old installer folder
        FileUtils.deleteDirectory(oldFiles)

        // start with deleting the mods folder as it is not guaranteed to have override mods
        val modsFolder = File(basePath + "mods/")

        if (modsFolder.exists())
            FileUtils.moveDirectory(modsFolder, File(oldFiles, "mods"))
        LOGGER.info("Moved the mods folder")

        LOGGER.info("Starting to unzip files.")
        // unzip start
        try {
            ZipInputStream(FileInputStream(downloadedPack)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry

                loop@ while (entry != null) {
                    LOGGER.info("Entry in zip: $entry", true)
                    val name = entry.name

                    // special manifest treatment
                    if (name == "manifest.json")
                        zis.writeToFile(File(basePath + "manifest.json"))


                    // overrides
                    if (name.startsWith("overrides/")) {
                        val path = entry.name.substring(10)

                        when {
                            patterns.any { it.matches(Paths.get(path)) } ->
                                LOGGER.info("Skipping $path as it is on the ignore List.", true)


                            !name.endsWith("/") -> {
                                val outfile = File(basePath + path)
                                LOGGER.info("Copying zip entry to = $outfile", true)


                                outfile.parent?.let { File(it).mkdirs() }

                                zis.writeToFile(outfile)
                            }

                            name != "overrides/" -> {
                                val newFolder = File(basePath + path)
                                if (newFolder.exists())
                                    FileUtils.moveDirectory(newFolder, File(oldFiles, path))

                                LOGGER.info("Folder moved: " + newFolder.absolutePath, true)
                            }
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

fun ZipInputStream.writeToFile(file: File) {
    file.outputStream().use { fos ->
        val bytes = this.readBytes()
        fos.write(bytes, 0, bytes.size)
    }
}