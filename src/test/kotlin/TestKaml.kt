import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

class TestKaml {
    @Test
    fun testParse(){

        val yaml = File("server-setup-config.yaml").readText()
        val obj = Yaml.default.decodeFromString<ConfigFile>(yaml)


        println(obj)
    }
}

@Serializable
data class TestClass(
    val x: Int = 100,
    val s: String = "lalala",
    val n: String? = null
)

fun main() {
    val yaml =
        """
            x: 2
        """.trimIndent()

    val obj = Yaml.default.decodeFromString<TestClass>(yaml)


    println(obj)

}
