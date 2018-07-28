package atm.bloodworkxgaming.serverstarter.packtype

import atm.bloodworkxgaming.serverstarter.InternetManager
import atm.bloodworkxgaming.serverstarter.ServerStarter
import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.util.zip.ZipInputStream

abstract class AbstractZipbasedPackType(private val configFile: ConfigFile) : IPackType {
    protected val basePath = configFile.install.baseInstallPath

    override fun installPack() {
        if (!configFile.install.modpackUrl.isEmpty()) {
            val url = configFile.install.modpackUrl

            try {
                val patterns = configFile.install.ignoreFiles
                        .map {
                            val s = if (it.startsWith("glob:") || it.startsWith("regex:"))
                                it
                            else
                                "glob:$it"

                            FileSystems.getDefault().getPathMatcher(s)
                        }

                handleZip(obtainZipFile(url), patterns)
                postProcessing()
            } catch (e: IOException) {
                ServerStarter.LOGGER.error("Error while installing pack", e)
            }
        }
    }

    /**
     * Downloads the modpack from the given url if it
     *
     * @param url URL to download from
     * @return File of the saved modpack zip
     * @throws IOException if something went wrong while downloading
     */
    @Throws(IOException::class)
    protected fun obtainZipFile(url: String): File {
        return if (url.startsWith("file://")) {
            File(url.substring(7))
        } else {
            downloadFile(cleanUrl(url))
        }
    }

    @Throws(IOException::class)
    private fun downloadFile(url: String): File {
        ServerStarter.LOGGER.info("Attempting to download modpack Zip.")
        try {
            val to = File(basePath + "modpack-download.zip")

            InternetManager.downloadToFile(url, to)
            ServerStarter.LOGGER.info("Downloaded the modpack zip file to " + to.absolutePath)

            return to

        } catch (e: IOException) {
            ServerStarter.LOGGER.error("Pack could not be downloaded")
            throw e
        }
    }

    /**
     * Overwrite this function to clean the url before downloading it
     */
    protected abstract fun cleanUrl(url: String): String
    protected abstract fun handleZip(file: File, pathMatchers: List<PathMatcher>)
    protected abstract fun postProcessing()
}

fun ZipInputStream.writeToFile(file: File) {
    file.outputStream().use { fos ->
        val bytes = this.readBytes()
        fos.write(bytes, 0, bytes.size)
    }
}