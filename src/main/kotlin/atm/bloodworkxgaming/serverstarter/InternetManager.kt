package atm.bloodworkxgaming.serverstarter

import atm.bloodworkxgaming.serverstarter.ServerStarter.Companion.LOGGER
import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class InternetManager(private val configFile: ConfigFile) {
    val httpClient = OkHttpClient.Builder()
            .connectTimeout(configFile.install.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(configFile.install.readTimeout, TimeUnit.SECONDS)
            .build()


    // Checking for connections seems to be broken on linux without root priviliges
    // Therefore we use HTTP get requests on linux to check for a valid connection
    fun checkConnection(): Boolean {
        var reached = 0

        val urls = if (OSUtil.isWindows) {
            listOf("8.8.8.8", "1.0.0.1")
        }  else {
            listOf("example.com", "google.com")
        }


        for (url in urls) {
            try {
                LOGGER.info("Pinging $url.")
                val r = if (OSUtil.isWindows) {
                     InetAddress.getByName(url).isReachable(1000)
                } else {
                    val req = Request.Builder()
                        .url(url)
                        .get()
                        .build()

                    try {
                        val res = httpClient.newCall(req).execute()
                        res.isSuccessful
                    }  catch (ex: IOException) {
                        false
                    }
                }

                LOGGER.info("Reached $url: $r")
                if (r) reached++
            } catch (e: IOException) {
                LOGGER.error("Error while attempting to reach", e)
            }
        }

        LOGGER.info("Reached $reached out of ${urls.size} IPs.")
        if (reached != urls.size) {
            LOGGER.error("Not every host could be reached. There could be a problem with your internet connection!!!!")
            return false
        }

        return true
    }


    @Throws(IOException::class)
    fun downloadToFile(url: String, dest: File) {
        val req = Request.Builder()
                .url(url)
                .get()
                .build()

        val res = httpClient.newCall(req).execute()
        val source = res.body?.source()

        source ?: throw IOException("Message body or source from $url was null")

        source.use {
            dest.parentFile?.mkdirs()
            dest.sink().buffer().use {
                it.writeAll(source)
            }
        }
    }
}
