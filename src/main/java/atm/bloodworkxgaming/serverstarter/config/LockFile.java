package atm.bloodworkxgaming.serverstarter.config;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString
public class LockFile {
    public boolean forgeInstalled = false;
    public boolean packInstalled = false;
    public String forgeVersion = "NONE";
    public String mcVersion = "NONE";
    public String packUrl = "NONE";
    public String spongeBootstrapper = "NONE";


    public boolean checkShouldInstall(ConfigFile configFile) {
        return !forgeInstalled
                || !packInstalled
                || (configFile.install.forgeVersion != null && !configFile.install.forgeVersion.isEmpty() && !Objects.equals(forgeVersion, configFile.install.forgeVersion))
                || !Objects.equals(packUrl, configFile.install.modpackUrl);
    }
}
