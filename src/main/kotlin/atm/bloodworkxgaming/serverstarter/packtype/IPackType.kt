package atm.bloodworkxgaming.serverstarter.packtype

import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import atm.bloodworkxgaming.serverstarter.packtype.curse.CursePackType

interface IPackType {
    companion object {
        private val packtype = mutableMapOf<String, (ConfigFile) -> IPackType>(
                Pair("curse", ::CursePackType),
                Pair("curseforge", ::CursePackType)
        )

        fun createPackType(packTypeName: String, configFile: ConfigFile): IPackType? {
            return packtype[packTypeName]?.invoke(configFile)
        }
    }

    /**
     * Downloads and installs the pack
     */
    fun installPack()

    /**
     * Gets the forge version, can be based on the version from the downloaded pack
     *
     * @return String representation of the version
     */
    fun getForgeVersion(): String

    /**
     * Gets the forge version, can be based on the version from the downloaded pack
     *
     * @return String representation of the version
     */
    fun getMCVersion(): String
}
