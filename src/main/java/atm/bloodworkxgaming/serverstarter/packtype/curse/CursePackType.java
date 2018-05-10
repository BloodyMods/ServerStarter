package atm.bloodworkxgaming.serverstarter.packtype.curse;

import atm.bloodworkxgaming.serverstarter.config.ConfigFile;
import atm.bloodworkxgaming.serverstarter.packtype.IPackType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CursePackType implements IPackType {
    @Override
    public void handleConfig(ConfigFile configFile) {
        File manifest = new File("manifest.json");
        File overrides = new File("overrides");
        if (manifest.exists() && overrides.exists()) {
            System.out.println("Valid folder which already has stuff, can go on with ");
        } else if (configFile.install.modpackUrl != null && !configFile.install.modpackUrl.isEmpty()) {
            String url = configFile.install.modpackUrl;
            if (!url.endsWith("/download"))
                url += "/download";

            try {
                File to = new File("setup/modpack-download.zip");
                System.out.println("to = " + to.getAbsolutePath());

                // Download file TODO
                // FileUtils.copyURLToFile(new URL(url), to);
                System.out.println("Downloaded file!");


                // unzip start
                ZipInputStream zis = new ZipInputStream(new FileInputStream("setup/modpack-download.zip"));
                ZipEntry entry = zis.getNextEntry();

                byte[] buffer = new byte[1024];

                while (entry != null) {
                    System.out.println("entry = " + entry);

                    // special manifest treatment
                    if (entry.getName().equals("manifest.json")) {

                        File manifestFile = new File("setup/manifest.json");
                        FileOutputStream fos = new FileOutputStream(manifestFile);

                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }

                        JsonElement json = new JsonParser().parse(new FileReader(manifestFile));
                        System.out.println("json = " + json.getAsJsonObject());
                        for (JsonElement jsonElement : json.getAsJsonObject().getAsJsonArray("files")) {
                            System.out.println("jsonElement = " + jsonElement);
                        }
                    }


                    // overrides
                    if (entry.getName().startsWith("overrides/") && !entry.getName().endsWith("/")) {
                        File outfile = new File("setup/" + entry.getName().substring(10));
                        System.out.println("outfile = " + outfile);
                        new File(outfile.getParent()).mkdirs();

                        FileOutputStream fos = new FileOutputStream(outfile);

                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }





                    entry = zis.getNextEntry();
                }

                zis.closeEntry();
                zis.close();




            } catch (IOException  e) {
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


    public void getBasePack() {

    }
}
