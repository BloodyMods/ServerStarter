package atm.bloodworkxgaming.serverstarter;

import atm.bloodworkxgaming.serverstarter.config.AddionalFile;
import atm.bloodworkxgaming.serverstarter.config.ConfigFile;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static atm.bloodworkxgaming.serverstarter.ServerStarter.*;

public class FileManager {
    public ConfigFile configFile;
    public List<AddionalFile> addionalFiles;

    public FileManager(ConfigFile configFile) {
        this.configFile = configFile;
        addionalFiles = configFile.install.additionalFiles != null ? configFile.install.additionalFiles : Collections.emptyList();
    }

    public void installAdditionalFiles() {
        LOGGER.info("Starting to installing Additional Files");
        List<AddionalFile> fallbackList = new ArrayList<>();
        addionalFiles.parallelStream().forEach(file -> handleAdditionalFile(file, fallbackList));

        List<AddionalFile> failList = new ArrayList<>();

        fallbackList.parallelStream().forEach(file -> handleAdditionalFile(file, failList));
    }

    private void handleAdditionalFile(AddionalFile file, List<AddionalFile> fallbackList) {
        LOGGER.info("Starting to download " + file);
        try {
            FileUtils.copyURLToFile(cleanUrl(file.url), new File(configFile.install.baseInstallPath + file.destination));
        } catch (IOException e) {
            LOGGER.error("Failed to download additional file", e);
            fallbackList.add(file);
        } catch (URISyntaxException e) {
            LOGGER.error("Invalid url for " + file, e);
        }
    }


    public static URL cleanUrl(String url) throws URISyntaxException, MalformedURLException {
        int sOrNotIndex = url.indexOf("/");
        String sOrNot = url.substring(0, sOrNotIndex - 1);
        int hostIndex = url.indexOf("/", sOrNotIndex + 2);
        String host = url.substring(sOrNotIndex + 2, hostIndex);
        String path = url.substring(url.indexOf("/", hostIndex));

        return new URI(sOrNot, host, path, null).toURL();
    }
}
