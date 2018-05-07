package config;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString()
public class ConfigFile {
    public int _specver;
    public ModpackConfig modpack;
    public InstallConfig install;

    public LaunchSettings launch;
}
