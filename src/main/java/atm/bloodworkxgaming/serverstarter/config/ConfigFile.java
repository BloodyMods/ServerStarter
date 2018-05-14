package atm.bloodworkxgaming.serverstarter.config;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class ConfigFile {
    public int _specver;
    public ModpackConfig modpack;
    public InstallConfig install;

    public LaunchSettings launch;

    public ConfigFile normalize(){
        if (install.baseInstallPath == null) install.baseInstallPath = "";

        return this;
    }
}
