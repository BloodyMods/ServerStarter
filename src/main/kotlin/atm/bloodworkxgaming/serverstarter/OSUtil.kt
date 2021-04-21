package atm.bloodworkxgaming.serverstarter

object OSUtil {
    val osName: String by lazy {
        try {
            System.getProperty("os.name")
        } catch (e: Exception) {
            ""
        }
    }

    val isLinux: Boolean = osName.toLowerCase().startsWith("linux")
    val isWindows: Boolean = osName.toLowerCase().startsWith("win")
}
