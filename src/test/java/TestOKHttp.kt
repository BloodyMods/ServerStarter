import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import okio.buffer
import okio.sink
import org.junit.Test
import java.io.File

class TestOKHttp {
    // @Test
    fun test() {
        val client = OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()

        val req = Request.Builder()
                .url("https://minecraft.curseforge.com/projects/ping/files/2546007/download")
                .get()
                .build()

        val res = client.newCall(req).execute()

        println(res.header("Location"))
        println(res.isRedirect)
    }

    // @Test
    fun testDownload() {
        val client = OkHttpClient.Builder()
                .build()

        val req = Request.Builder()
                .url("https://edge.forgecdn.net/files/2560/919/Pam's HarvestCraft 1.12.2u.jar")
                .get()
                .build()

        val res = client.newCall(req).execute()
        val file = File("D:\\Users\\jonas\\Documents\\GitHub\\serverstarter\\test/Pam's HarvestCraft 1.12.2u.jar")
        println("file = ${file.absolutePath}")
        val sink = file.sink().buffer()

        val source = res.body?.source() ?: return
        sink.writeAll(source)
        sink.close()

        println(res.header("Location"))
        println(res.isRedirect)
    }
}

