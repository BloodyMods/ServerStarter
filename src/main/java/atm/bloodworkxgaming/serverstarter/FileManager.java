package atm.bloodworkxgaming.serverstarter;

import atm.bloodworkxgaming.serverstarter.config.AdditionalFile;
import atm.bloodworkxgaming.serverstarter.config.ConfigFile;
import atm.bloodworkxgaming.serverstarter.config.LocalFile;
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
    public List<AdditionalFile> additionalFiles;

    public FileManager(ConfigFile configFile) {
        this.configFile = configFile;
        additionalFiles = configFile.install.additionalFiles != null ? configFile.install.additionalFiles : Collections.emptyList();
    }

    public void installAdditionalFiles() {
        LOGGER.info("Starting to installing Additional Files");
        List<AdditionalFile> fallbackList = new ArrayList<>();
        additionalFiles.parallelStream().forEach(file -> handleAdditionalFile(file, fallbackList));

        List<AdditionalFile> failList = new ArrayList<>();

        fallbackList.parallelStream().forEach(file -> handleAdditionalFile(file, failList));
    }

    private void handleAdditionalFile(AdditionalFile file, List<AdditionalFile> fallbackList) {
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


    public void installLocalFiles() {
        LOGGER.info("Starting to copy local files.");
        for (LocalFile localFile : configFile.install.localFiles) {
            LOGGER.info("Copying local file: " + localFile);
            try {
                FileUtils.copyFile(new File(localFile.from), new File(localFile.to));
            } catch (IOException e) {
                LOGGER.error("Error while copying local file", e);
            }
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
