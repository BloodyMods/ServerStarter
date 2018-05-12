package atm.bloodworkxgaming.serverstarter.packtype.curse;

import atm.bloodworkxgaming.serverstarter.config.ConfigFile;
import atm.bloodworkxgaming.serverstarter.packtype.IPackType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CursePackType implements IPackType {
    private ConfigFile configFile;
    private String basePath;

    public CursePackType(ConfigFile configFile) {
        this.configFile = configFile;
        basePath = configFile.install.baseInstallPath;
    }

    @Override
    public void installPack() {
        if (configFile.install.modpackUrl != null && !configFile.install.modpackUrl.isEmpty()) {
            String url = configFile.install.modpackUrl;
            if (!url.endsWith("/download"))
                url += "/download";

            try {
                // unzipFile(downloadPack(url));
                unzipFile(new File(basePath + "modpack-download.zip"));
                handleManifest();

            } catch (IOException e) {
                e.printStackTrace();
            }


        } else if (configFile.install.formatSpecific.containsKey("packid") && configFile.install.formatSpecific.containsKey("fileid")) {
            try {
                HttpResponse<JsonNode> res = Unirest.get("/api/v2/direct/GetAddOnFile/" + configFile.install.formatSpecific.get("packid") + "/" + configFile.install.formatSpecific.get("fileid")).asJson();
                System.out.println("res = " + res);
            } catch (UnirestException e) {
                e.printStackTrace();
            }
        }
    }


    private File downloadPack(String url) throws IOException {
        try {
            File to = new File(basePath + "modpack-download.zip");
            System.out.println("to = " + to.getAbsolutePath());

            FileUtils.copyURLToFile(new URL(url), to);
            System.out.println("Downloaded file!");

            return to;

        } catch (IOException e) {
            System.err.println("Pack could not be downloaded");
            throw e;
        }
    }

    private void unzipFile(File downloadedPack) throws IOException {
        // start with deleting the mods folder as it is not garanteed to have override mods
        FileUtils.deleteDirectory(new File(basePath + "mods/"));
        System.out.println("Deleted the mods folder");

        // unzip start
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(downloadedPack));) {
            ZipEntry entry = zis.getNextEntry();

            byte[] buffer = new byte[1024];

            while (entry != null) {
                System.out.println("entry = " + entry);
                String name = entry.getName();

                // special manifest treatment
                if (name.equals("manifest.json")) {

                    File manifestFile = new File(basePath + "manifest.json");
                    try (FileOutputStream fos = new FileOutputStream(manifestFile)) {

                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }


                // overrides
                if (name.startsWith("overrides/")) {
                    if (!name.endsWith("/")) {
                        File outfile = new File(basePath + entry.getName().substring(10));
                        System.out.println("outfile = " + outfile);
                        //noinspection ResultOfMethodCallIgnored
                        new File(outfile.getParent()).mkdirs();

                        try (FileOutputStream fos = new FileOutputStream(outfile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }

                    } else if (!name.equals("overrides/")) {
                        File newFolder = new File(basePath + entry.getName().substring(10));
                        FileUtils.deleteDirectory(newFolder);

                        System.out.println("FOLDER Deleted: " + newFolder.getAbsolutePath());
                    }
                }


                entry = zis.getNextEntry();
            }


            zis.closeEntry();
        } catch (IOException e) {
            System.err.println("Could not unzip files");
            throw e;
        }
    }

    private void handleManifest() throws IOException {
        String forgeVersion = null;
        String mcVersion = null;
        List<ModEntryRaw> mods = new ArrayList<>();

        try (FileReader reader = new FileReader(new File(basePath + "manifest.json"))) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            System.out.println("json = " + json);
            JsonObject mcObj = json.getAsJsonObject("minecraft");
            mcVersion = mcObj.getAsJsonPrimitive("version").getAsString();
            JsonArray loaders = mcObj.getAsJsonArray("modLoaders");

            if (loaders.size() > 0) {
                forgeVersion = loaders.get(0).getAsJsonObject().getAsJsonPrimitive("id").getAsString();
            }

            for (JsonElement jsonElement : json.getAsJsonArray("files")) {
                JsonObject obj = jsonElement.getAsJsonObject();
                mods.add(new ModEntryRaw(
                        obj.getAsJsonPrimitive("projectID").getAsString(),
                        obj.getAsJsonPrimitive("fileID").getAsString()));
            }
        }

        forgeVersion = configFile.install.forgeVersion == null ? forgeVersion : configFile.install.forgeVersion;
        mcVersion = configFile.install.mcVersion == null ? mcVersion : configFile.install.mcVersion;

        downloadMods(mods);
    }

    private void downloadMods(List<ModEntryRaw> mods) {
        JsonObject request = new JsonObject();
        // constructs the body
        JsonArray array = new JsonArray();
        for (ModEntryRaw mod : mods) {
            JsonObject objMod = new JsonObject();
            objMod.addProperty("AddOnID", mod.projectID);
            objMod.addProperty("FileID", mod.fileID);
            array.add(objMod);
        }
        request.add("addOnFileKeys", array);
        System.out.println("request = " + request.toString());

        try {
            HttpResponse<JsonNode> res = Unirest
                    .post(configFile.install.getFormatSpecificSettingOrDefault("cursemeta", "https://cursemeta.dries007.net")
                            + "/api/v2/direct/GetAddOnFiles")
                    .header("User-Agent", "All the mods server installer.")
                    .header("Content-Type", "application/json")
                    .body(request.toString())
                    .asJson();

            if (res.getStatus() != 200)
                throw new UnirestException("Response was not OK");

            // Gets the download links for the mods
            List<String> modsToDownload = new ArrayList<>();
            JsonArray jsonRes = new JsonParser().parse(res.getBody().toString()).getAsJsonArray();
            for (JsonElement modEntry : jsonRes) {
                modsToDownload.add(modEntry
                        .getAsJsonObject()
                        .getAsJsonArray("Value").get(0)
                        .getAsJsonObject()
                        .getAsJsonPrimitive("DownloadURL").getAsString());
            }

            processMods(modsToDownload);

            System.out.println("res: " + jsonRes);
            System.out.println("modsToDownload = " + modsToDownload);


        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }

    private void processMods(List<String> mods) {
        AtomicInteger count = new AtomicInteger(0);
        int totalCount = mods.size();
        List<String> fallbackList = new ArrayList<>();

        mods.stream().parallel().forEach(s -> processSingleMod(s, count, totalCount, fallbackList));
        // mods.forEach(s -> processSingleMod(s, count, totalCount, fallbackList));

        List<String> secondFail = new ArrayList<>();
        fallbackList.forEach(s -> processSingleMod(s, count, totalCount, secondFail));

        if (!secondFail.isEmpty()) {
            System.out.println("Failed to download (a) mod(s):");
            for (String s : secondFail) {
                System.out.println("\t" + s);
            }

        }

    }

    private void processSingleMod(String mod, AtomicInteger counter, int totalCount, List<String> fallbackList) {
        try {
            String modName = FilenameUtils.getName(mod);
            URI uri = new URI("https", "files.forgecdn.net", mod.substring(26), null);

            FileUtils.copyURLToFile(
                    //new URL(uri.toASCIIString()),
                    uri.toURL(),
                    new File(basePath + "mods/" + modName));
            System.out.println("[" + counter.incrementAndGet() + "/" + totalCount + "] Downloaded mod: " + modName);
        } catch (IOException e) {
            fallbackList.add(mod);
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @AllArgsConstructor
    @ToString
    private static class ModEntryRaw {
        @Getter
        private String projectID;

        @Getter
        private String fileID;
    }
}
