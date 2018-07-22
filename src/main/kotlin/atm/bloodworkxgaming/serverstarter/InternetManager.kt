package atm.bloodworkxgaming.serverstarter

import atm.bloodworkxgaming.serverstarter.ServerStarter.Companion.LOGGER
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object InternetManager {
    val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()!!

    private val urls = listOf("8.8.8.8", "1.1.1.1")

    fun checkConnection(): Boolean {
        var reached = 0

        for (url in urls) {
            try {
                LOGGER.info("Pinging $url.")
                val r = InetAddress.getByName(url).isReachable(1000)
                LOGGER.info("Reached $url: $r")
                if (r) reached++
            } catch (e: IOException) {
                LOGGER.error("Error while pinging", e)
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
        val source = res?.body()?.source()

        source ?: throw IOException("Message body or source from $url was null")

        Okio.buffer(Okio.sink(dest)).use {
            it.writeAll(source)
        }
    }
}
