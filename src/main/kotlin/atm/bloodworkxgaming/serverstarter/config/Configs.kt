package atm.bloodworkxgaming.serverstarter.config

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class AdditionalFile(
    var url: String = "",
    var destination: String = ""
)

@Serializable
data class LocalFile(
    var from: String = "",
    var to: String = ""
)

@Serializable
data class ModpackConfig(
    var name: String = "",
    var description: String = ""
)

@Serializable
data class LaunchSettings(
    var spongefix: Boolean = false,
    var ramDisk: Boolean = false,
    var checkOffline: Boolean = false,
    var maxRam: String = "",
    var minRam: String = "",

    var startFile: String = "",
    var javaArgs: List<String> = Collections.emptyList(),
    var autoRestart: Boolean = false,
    var crashLimit: Int = 0,
    var crashTimer: String = "",
    var preJavaArgs: String = "",

    var forcedJavaPath: String = "",

    ) {
    val processedForcedJavaPath: String
        get() {
            var str = forcedJavaPath
            val regex = Regex("\\\$\\{(.+)}")
            for (matchResult in regex.findAll(forcedJavaPath)) {
                val res = matchResult.groupValues.getOrNull(0) ?: continue
                val inner = matchResult.groupValues.getOrNull(1) ?: continue
                str = str.replace(res, System.getenv(inner))
            }

            return str
        }
}

@Serializable
data class InstallConfig(
    var mcVersion: String = "",

    var loaderVersion: String = "",
    var installerUrl: String = "",
    var installerArguments: List<String> = Collections.emptyList(),

    var modpackUrl: String = "",
    var modpackFormat: String = "",

    var formatSpecific: FormatSpecific = FormatSpecific(),

    var baseInstallPath: String = "",
    var ignoreFiles: List<String>? = null,
    var additionalFiles: List<AdditionalFile>? = null,
    var localFiles: List<LocalFile>? = null,

    var checkFolder: Boolean = false,
    var installLoader: Boolean = false,

    var spongeBootstrapper: String = "",
    var connectTimeout: Long = 30,
    var readTimeout: Long = 30,
) {


    @Suppress("UNCHECKED_CAST")
    fun <T> getFormatSpecificSettingOrDefault(name: String, fallback: T?): T? {
        TODO()
        // return formatSpecific.getOrDefault(name, fallback) as T?
    }
}

@Serializable
data class FormatSpecific(
    val ignoreProject: List<Int> = emptyList()
)

@Serializable
data class ConfigFile(
    var _specver: Int = 0,
    var modpack: ModpackConfig = ModpackConfig(),
    var install: InstallConfig = InstallConfig(),
    var launch: LaunchSettings = LaunchSettings()
)

