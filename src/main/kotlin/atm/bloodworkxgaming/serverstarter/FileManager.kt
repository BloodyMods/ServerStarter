package atm.bloodworkxgaming.serverstarter

import atm.bloodworkxgaming.serverstarter.ServerStarter.Companion.LOGGER
import atm.bloodworkxgaming.serverstarter.config.AdditionalFile
import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

class FileManager(private val configFile: ConfigFile, private val internetManager: InternetManager) {

    fun installAdditionalFiles() {
        LOGGER.info("Starting to installing Additional Files")
        val fallbackList = mutableListOf<AdditionalFile>()
        configFile.install.additionalFiles
            ?.parallelStream()
            ?.forEach { file -> handleAdditionalFile(file, fallbackList) }

        val failList = mutableListOf<AdditionalFile>()
        fallbackList.parallelStream().forEach { file -> handleAdditionalFile(file, failList) }
    }

    private fun handleAdditionalFile(file: AdditionalFile, fallbackList: MutableList<AdditionalFile>) {
        LOGGER.info("Starting to download $file")
        try {
            internetManager.downloadToFile(file.url, File(configFile.install.baseInstallPath + file.destination))
        } catch (e: IOException) {
            LOGGER.error("Failed to download additional file", e)
            fallbackList.add(file)
        } catch (e: URISyntaxException) {
            LOGGER.error("Invalid url for $file", e)
        }

    }


    fun installLocalFiles() {
        LOGGER.info("Starting to copy local files.")

        for (localFile in configFile.install.localFiles ?: return) {
            LOGGER.info("Copying local file: $localFile")
            try {
                if (File(localFile.from).isDirectory) {
                    FileUtils.copyDirectory(File(localFile.from), File(localFile.to))
                } else {
                    FileUtils.copyFile(File(localFile.from), File(localFile.to))
                }
            } catch (e: IOException) {
                LOGGER.error("Error while copying local file", e)
            }
        }
    }
}

@Throws(URISyntaxException::class, MalformedURLException::class)
fun String.toCleanUrl(): URL {
    val sOrNotIndex = this.indexOf("/")
    val sOrNot = this.substring(0, sOrNotIndex - 1)
    val hostIndex = this.indexOf("/", sOrNotIndex + 2)
    val host = this.substring(sOrNotIndex + 2, hostIndex)
    val path = this.substring(this.indexOf("/", hostIndex))

    return URI(sOrNot, host, path, null).toURL()
}

