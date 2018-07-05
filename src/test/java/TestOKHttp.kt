import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test

class TestOKHttp {
    @Test
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
}

