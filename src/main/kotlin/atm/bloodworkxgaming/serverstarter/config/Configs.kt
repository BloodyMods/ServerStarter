package atm.bloodworkxgaming.serverstarter.config

import java.util.*

data class AdditionalFile(
        var url: String = "",
        var destination: String = ""
)

data class LocalFile(
        var from: String = "",
        var to: String = ""
)

data class ModpackConfig(
        var name: String = "",
        var description: String = ""
)

data class LaunchSettings(
        var spongefix: Boolean = false,
        var ramDisk: Boolean = false,
        var checkOffline: Boolean = false,
        var maxRam: String = "",

        var startFile: String = "",
        var javaArgs: List<String> = Collections.emptyList(),
        var autoRestart: Boolean = false,
        var crashLimit: Int = 0,
        var crashTimer: String = "",
        var preJavaArgs: String = "",

        var forcedJavaPath: String = ""
)

data class InstallConfig(
        var mcVersion: String = "",

        var forgeVersion: String = "",
        var forgeInstallerUrl: String = "",
        var installerArguments: List<String> = Collections.emptyList(),

        var modpackUrl: String = "",
        var modpackFormat: String = "",
        var formatSpecific: Map<String, Any> = Collections.emptyMap(),

        var baseInstallPath: String = "",
        var ignoreFiles: List<String> = Collections.emptyList(),
        var additionalFiles: List<AdditionalFile> = Collections.emptyList(),
        var localFiles: List<LocalFile> = Collections.emptyList(),

        var checkFolder: Boolean = false,
        var installForge: Boolean = false,

        var spongeBootstrapper: String = "") {


    @Suppress("UNCHECKED_CAST")
    fun <T> getFormatSpecificSettingOrDefault(name: String, fallback: T?): T? {
        return formatSpecific.getOrDefault(name, fallback) as T?
    }
}

data class ConfigFile(
        var _specver: Int = 0,
        var modpack: ModpackConfig = ModpackConfig(),
        var install: InstallConfig = InstallConfig(),
        var launch: LaunchSettings = LaunchSettings()
)
