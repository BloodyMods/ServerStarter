import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.junit.Test
import java.io.File

class TestKaml {
    @Test
    fun testParse(){

        val yaml = File("server-setup-config.yaml").readText()
        val obj = Yaml.default.decodeFromString<ConfigFile>(yaml)


        println(obj)
    }

    @Test
    fun testNullList(){
        val yaml =
            """
            n: 
        """.trimIndent()

        val obj = Yaml.default.decodeFromString<TestClass>(yaml)


        println(obj)
    }
}

@Serializable
data class TestClass(
    val x: Boolean = false,
    val s: String? = "lalala",
    val n: List<String>? = null
)
