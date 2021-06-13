package atm.bloodworkxgaming.serverstarter.config

data class LockFile(
        var loaderInstalled: Boolean = false,
        var packInstalled: Boolean = false,
        var loaderVersion: String = "NONE",
        var mcVersion: String = "NONE",
        var packUrl: String = "NONE",
        var spongeBootstrapper: String = "NONE") {


    fun checkShouldInstall(configFile: ConfigFile): Boolean {
        return (!loaderInstalled
                || !packInstalled
                || !configFile.install.loaderVersion.isNullOrEmpty() && loaderVersion != configFile.install.loaderVersion
                || packUrl != configFile.install.modpackUrl)
    }
}
