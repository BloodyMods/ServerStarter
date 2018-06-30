package atm.bloodworkxgaming.serverstarter.packtype

interface IPackType {
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
