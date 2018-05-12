package atm.bloodworkxgaming.serverstarter.config;

import java.util.Objects;

public class LockFile {
    public boolean forgeInstalled = false;
    public boolean packInstalled = false;
    public String forgeVersion = "NONE";
    public String mcVersion = "NONE";
    public String packUrl = "NONE";


    public boolean checkShouldInstall(ConfigFile configFile) {
        return !forgeInstalled
                || !packInstalled
                || !Objects.equals(forgeVersion, configFile.install.forgeVersion)
                || !Objects.equals(packUrl, configFile.install.modpackUrl);
    }
}
