import atm.bloodworkxgaming.serverstarter.ServerStarter
import okhttp3.internal.closeQuietly
import org.junit.Test
import java.io.BufferedInputStream
import java.io.File
import java.util.*


class StarterTests {
    // @Test
    fun testCurseID() {
        val starter = ServerStarter(emptyArray())

        starter.startLoading()
    }

    fun testIO() {
        val t = readLine()

        ProcessBuilder("cmd").apply {
            inheritIO()
            start().apply {
                waitFor()
                outputStream.close()
                errorStream.close()
                inputStream.close()
            }
        }



    }
}

fun main() {
    StarterTests().testIO()
}
