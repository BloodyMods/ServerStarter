package atm.bloodworkxgaming.serverstarter.config;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
@EqualsAndHashCode
@ToString
public class InstallConfig {
    public String mcVersion;

    public String forgeVersion;
    public String forgeInstallerUrl;
    public String modpackUrl;
    public String modpackFormat;

    public List<String> ignoreFiles;
    public List<AddionalFile> additionalFiles;
    public List<LocalFile> localFiles;

    public boolean checkFolder;

}
