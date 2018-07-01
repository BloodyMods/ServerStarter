package atm.bloodworkxgaming.serverstarter

import atm.bloodworkxgaming.serverstarter.ServerStarter.Companion.LOGGER
import java.io.IOException
import java.net.InetAddress

object InternetChecker {
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
}
