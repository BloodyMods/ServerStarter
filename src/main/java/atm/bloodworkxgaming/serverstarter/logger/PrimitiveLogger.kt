package atm.bloodworkxgaming.serverstarter.logger

import okio.Okio
import org.fusesource.jansi.Ansi
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PrimitiveLogger(outputFile: File) {
    private val pattern = "\\x1b\\[[0-9;]*m".toRegex()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val bufferedSink = Okio.buffer(Okio.sink(outputFile))

    init {
        if (outputFile.exists()) {
            outputFile.delete()
        }
    }

    @JvmOverloads
    fun info(message: Any?, logOnly: Boolean = false) {
        val m = currentTimeAnsi().fgYellow().a("[INFO] ").fgDefault().a(message).reset().newline().toString()

        synchronized(this) {
            try {
                bufferedSink.writeUtf8(stripColors(m))
            } catch (e: IOException) {
                error("Error while logging!", e)
            }

            if (!logOnly) {
                print(m)
            }
        }
    }

    fun warn(message: Any?) {
        val m = currentTimeAnsi().fgMagenta().a("[WARNING] ").bgDefault().a(message).reset().newline().toString()

        synchronized(this) {
            try {
                bufferedSink.writeUtf8(stripColors(m))
            } catch (e: IOException) {
                error("Error while logging!", e)
            }

            print(m)
        }
    }

    fun error(message: String?, throwable: Throwable? = null) {
        var m = currentTimeAnsi().fgRed().a("[ERROR] ").bgDefault().a(message).reset().newline().toString()

        if (throwable != null) {
            throwable.printStackTrace()
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            m += "\n" + sw.toString()
        }

        synchronized(this) {
            try {
                bufferedSink.writeUtf8(stripColors(m))
            } catch (e: IOException) {
                System.err.println("Error while logging!")
                e.printStackTrace()
            }

            print(m)
        }
    }

    private fun stripColors(message: String): String {
        return pattern.replace(message, "")
    }

    private fun currentTimeAnsi(): Ansi {
        return Ansi.ansi().fgBrightBlack().a("[" + LocalTime.now().format(dateTimeFormatter) + "] ").fgDefault()
    }
}