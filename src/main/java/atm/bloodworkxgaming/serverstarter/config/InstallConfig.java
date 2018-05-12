package atm.bloodworkxgaming.serverstarter.config;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
@EqualsAndHashCode
@ToString
public class InstallConfig {
    public String mcVersion;

    public String forgeVersion;
    public String forgeInstallerUrl;
    public String modpackUrl;
    public String modpackFormat;
    public HashMap<String, String> formatSpecific;

    public String baseInstallPath;
    public List<String> ignoreFiles;
    public List<AddionalFile> additionalFiles;
    public List<LocalFile> localFiles;

    public boolean checkFolder;

    public String getFormatSpecificSettingOrDefault(String name, String fallback) {
        return formatSpecific == null ? fallback : formatSpecific.getOrDefault(name, fallback);
    }
}
