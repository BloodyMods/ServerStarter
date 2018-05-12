package atm.bloodworkxgaming.serverstarter.packtype;

public interface IPackType {
    /**
     * Downloads and installs the pack
     */
    void installPack();

    /**
     * Gets the forge version, can be based on the version from the downloaded pack
     * @return String representation of the version
     */
    String getForgeVersion();

    /**
     * Gets the forge version, can be based on the version from the downloaded pack
     * @return String representation of the version
     */
    String getMCVersion();
}
