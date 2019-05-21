package atm.bloodworkxgaming.serverstarter.packtype.curse

import atm.bloodworkxgaming.serverstarter.InternetManager
import atm.bloodworkxgaming.serverstarter.ServerStarter.Companion.LOGGER
import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import atm.bloodworkxgaming.serverstarter.packtype.AbstractZipbasedPackType
import atm.bloodworkxgaming.serverstarter.packtype.writeToFile
import com.google.gson.JsonParser
import okhttp3.Request
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URISyntaxException
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

open class CursePackType(private val configFile: ConfigFile) : AbstractZipbasedPackType(configFile) {
    private var forgeVersion: String = configFile.install.forgeVersion
    private var mcVersion: String = configFile.install.mcVersion
    private val oldFiles = File(basePath + "OLD_TO_DELETE/")

    override fun cleanUrl(url: String): String {
        if (url.contains("curseforge.com") && !url.endsWith("/download"))
            return "$url/download"

        return url
    }

    /**
     * Gets the forge version, can be based on the version from the downloaded pack
     *
     * @return String representation of the version
     */
    override fun getForgeVersion(): String {
        return forgeVersion
    }

    /**
     * Gets the forge version, can be based on the version from the downloaded pack
     *
     * @return String representation of the version
     */
    override fun getMCVersion(): String {
        return mcVersion
    }

    @Throws(IOException::class)
    override fun handleZip(file: File, pathMatchers: List<PathMatcher>) {
        // delete old installer folder
        FileUtils.deleteDirectory(oldFiles)

        // start with deleting the mods folder as it is not guaranteed to have override mods
        val modsFolder = File(basePath + "mods/")

        if (modsFolder.exists())
            FileUtils.moveDirectory(modsFolder, File(oldFiles, "mods"))
        LOGGER.info("Moved the mods folder")

        LOGGER.info("Starting to unzip files.")
        // unzip start
        try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry

                loop@ while (entry != null) {
                    LOGGER.info("Entry in zip: $entry", true)
                    val name = entry.name

                    // special manifest treatment
                    if (name == "manifest.json")
                        zis.writeToFile(File(basePath + "manifest.json"))


                    // overrides
                    if (name.startsWith("overrides/")) {
                        val path = entry.name.substring(10)

                        when {
                            pathMatchers.any { it.matches(Paths.get(path)) } ->
                                LOGGER.info("Skipping $path as it is on the ignore List.", true)


                            !name.endsWith("/") -> {
                                val outfile = File(basePath + path)
                                LOGGER.info("Copying zip entry to = $outfile", true)


                                outfile.parentFile?.mkdirs()

                                zis.writeToFile(outfile)
                            }

                            name != "overrides/" -> {
                                val newFolder = File(basePath + path)
                                if (newFolder.exists())
                                    FileUtils.moveDirectory(newFolder, File(oldFiles, path))

                                LOGGER.info("Folder moved: " + newFolder.absolutePath, true)
                            }
                        }
                    }

                    entry = zis.nextEntry
                }


                zis.closeEntry()
            }
        } catch (e: IOException) {
            LOGGER.error("Could not unzip files", e)
        }

        LOGGER.info("Done unzipping the files.")
    }

    @Throws(IOException::class)
    override fun postProcessing() {
        val mods = ArrayList<ModEntryRaw>()

        InputStreamReader(FileInputStream(File(basePath + "manifest.json")), "utf-8").use { reader ->
            val json = JsonParser().parse(reader).asJsonObject
            LOGGER.info("manifest JSON Object: $json", true)
            val mcObj = json.getAsJsonObject("minecraft")

            if (mcVersion.isEmpty()) {
                mcVersion = mcObj.getAsJsonPrimitive("version").asString
            }

            // gets the forge version
            if (forgeVersion.isEmpty()) {
                val loaders = mcObj.getAsJsonArray("modLoaders")
                if (loaders.size() > 0) {
                    forgeVersion = loaders[0].asJsonObject.getAsJsonPrimitive("id").asString.substring(6)
                }
            }

            // gets all the mods
            for (jsonElement in json.getAsJsonArray("files")) {
                val obj = jsonElement.asJsonObject
                mods.add(ModEntryRaw(
                        obj.getAsJsonPrimitive("projectID").asString,
                        obj.getAsJsonPrimitive("fileID").asString))
            }
        }

        downloadMods(mods)
    }

    /**
     * Downloads the mods specified in the manifest
     * Gets the data from cursemeta
     *
     * @param mods List of the mods from the manifest
     */
    private fun downloadMods(mods: List<ModEntryRaw>) {
        val ignoreSet = HashSet<String>()
        val ignoreListTemp = configFile.install.getFormatSpecificSettingOrDefault<List<Any>>("ignoreProject", null)

        if (ignoreListTemp != null)
            for (o in ignoreListTemp) {
                if (o is String)
                    ignoreSet.add(o)

                if (o is Int)
                    ignoreSet.add(o.toString())
            }


        val urls = ConcurrentLinkedQueue<String>()

        LOGGER.info("Requesting Download links from cursemeta.")

        mods.parallelStream().forEach { mod ->
            if (!ignoreSet.isEmpty() && ignoreSet.contains(mod.projectID)) {
                LOGGER.info("Skipping mod with projectID: " + mod.projectID)
                return@forEach
            }

            val url = (configFile.install.getFormatSpecificSettingOrDefault("cursemeta", "https://staging_cursemeta.dries007.net")
                    + "/" + mod.projectID + "/" + mod.fileID + ".json")
            LOGGER.info("Download url is: $url", true)

            try {
                val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "All the mods server installer.")
                        .header("Content-Type", "application/json")
                        .build()

                val res = InternetManager.httpClient.newCall(request).execute()

                if (!res.isSuccessful)
                    throw IOException("Request to $url was not successful.")
                val body = res.body() ?: throw IOException("Request to $url returned a null body.")

                val jsonRes = JsonParser().parse(body.string()).asJsonObject
                LOGGER.info("Response from manifest query: $jsonRes", true)

                urls.add(jsonRes
                        .asJsonObject
                        .getAsJsonPrimitive("DownloadURL").asString)
            } catch (e: IOException) {
                LOGGER.error("Error while trying to get URL from cursemeta for mod $mod", e)
            }
        }

        LOGGER.info("Mods to download: $urls", true)

        processMods(urls)

    }

    //region >>>>>>> Stuff for when cursemeta works again:
    /*
    private void downloadMods(List<ModEntryRaw> mods) {
        Set<String> ignoreSet = new HashSet<>();
        List<Object> ignoreListTemp = configFile.install.getFormatSpecificSettingOrDefault("ignoreProject", null);

        if (ignoreListTemp != null)
            for (Object o : ignoreListTemp) {
                if (o instanceof String)
                    ignoreSet.add((String) o);

                if (o instanceof Integer)
                    ignoreSet.add(String.valueOf(o));
            }

        // constructs the body
        JsonObject request = new JsonObject();
        JsonArray array = new JsonArray();
        for (ModEntryRaw mod : mods) {
            if (!ignoreSet.isEmpty() && ignoreSet.contains(mod.projectID)) {
                LOGGER.info("Skipping mod with projectID: " + mod.projectID);
                continue;
            }

            JsonObject objMod = new JsonObject();
            objMod.addProperty("AddOnID", mod.projectID);
            objMod.addProperty("FileID", mod.fileID);
            array.add(objMod);
        }
        request.add("addOnFileKeys", array);

        LOGGER.info("Requesting Download links from cursemeta.");
        LOGGER.info("About to make a request to cursemeta with body: " + request.toString(), true);

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

            LOGGER.info("Response from manifest query: " + jsonRes, true);
            LOGGER.info("Mods to download: " + modsToDownload, true);
            processMods(modsToDownload);

        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }*/
    //endregion

    /**
     * Downloads all mods, with a second fallback if failed
     * This is done in parallel for better performance
     *
     * @param mods List of urls
     */
    private fun processMods(mods: Collection<String>) {
        // constructs the ignore list
        val ignorePatterns = ArrayList<Pattern>()
        for (ignoreFile in configFile.install.ignoreFiles) {
            if (ignoreFile.startsWith("mods/")) {
                ignorePatterns.add(Pattern.compile(ignoreFile.substring(ignoreFile.lastIndexOf('/'))))
            }
        }

        // downloads the mods
        val count = AtomicInteger(0)
        val totalCount = mods.size
        val fallbackList = ArrayList<String>()

        mods.stream().parallel().forEach { s -> processSingleMod(s, count, totalCount, fallbackList, ignorePatterns) }

        val secondFail = ArrayList<String>()
        fallbackList.forEach { s -> processSingleMod(s, count, totalCount, secondFail, ignorePatterns) }

        if (!secondFail.isEmpty()) {
            LOGGER.warn("Failed to download (a) mod(s):")
            for (s in secondFail) {
                LOGGER.warn("\t" + s)
            }
        }
    }

    /**
     * Downloads a single mod and saves to the /mods directory
     *
     * @param mod            URL of the mod
     * @param counter        current counter of how many mods have already been downloaded
     * @param totalCount     total count of mods that have to be downloaded
     * @param fallbackList   List to write to when it failed
     * @param ignorePatterns Patterns of mods which should be ignored
     */
    private fun processSingleMod(mod: String, counter: AtomicInteger, totalCount: Int, fallbackList: MutableList<String>, ignorePatterns: List<Pattern>) {
        try {
            val modName = FilenameUtils.getName(mod)
            for (ignorePattern in ignorePatterns) {
                if (ignorePattern.matcher(modName).matches()) {
                    LOGGER.info("[" + counter.incrementAndGet() + "/" + totalCount + "] Skipped ignored mod: " + modName)
                }
            }

            InternetManager.downloadToFile(mod, File(basePath + "mods/" + modName))
            LOGGER.info("[" + String.format("% 3d", counter.incrementAndGet()) + "/" + totalCount + "] Downloaded mod: " + modName)

        } catch (e: IOException) {
            LOGGER.error("Failed to download mod", e)
            fallbackList.add(mod)

        } catch (e: URISyntaxException) {
            LOGGER.error("Invalid url for $mod", e)
        }
    }
}

/**
 * Data class to keep projectID and fileID together
 */
data class ModEntryRaw(val projectID: String, val fileID: String)
