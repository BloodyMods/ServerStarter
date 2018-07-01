package atm.bloodworkxgaming.serverstarter.config

data class LockFile(
        var forgeInstalled: Boolean = false,
        var packInstalled: Boolean = false,
        var forgeVersion: String = "NONE",
        var mcVersion: String = "NONE",
        var packUrl: String = "NONE",
        var spongeBootstrapper: String = "NONE") {


    fun checkShouldInstall(configFile: ConfigFile): Boolean {
        return (!forgeInstalled || !packInstalled || !configFile.install.forgeVersion.isEmpty() && forgeVersion != configFile.install.forgeVersion || packUrl != configFile.install.modpackUrl)
    }
}
