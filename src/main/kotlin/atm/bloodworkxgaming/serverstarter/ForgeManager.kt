package atm.bloodworkxgaming.serverstarter


import atm.bloodworkxgaming.serverstarter.ServerStarter.Companion.LOGGER
import atm.bloodworkxgaming.serverstarter.ServerStarter.Companion.lockFile
import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.fusesource.jansi.Ansi.ansi
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class ForgeManager(private val configFile: ConfigFile) {

    fun handleServer() {
        val startTimes = ArrayList<LocalDateTime>()
        val timerString = configFile.launch.crashTimer
        val crashTimer =
                try {
                    when {
                        timerString.endsWith("h") -> java.lang.Long.parseLong(timerString.substring(0, timerString.length - 1)) * 60 * 60
                        timerString.endsWith("min") -> java.lang.Long.parseLong(timerString.substring(0, timerString.length - 3)) * 60
                        timerString.endsWith("s") -> java.lang.Long.parseLong(timerString.substring(0, timerString.length - 1))
                        else -> java.lang.Long.parseLong(timerString)
                    }
                } catch (e: NumberFormatException) {
                    LOGGER.error("Invalid crash time format given", e)
                    -1L
                }

        var shouldRestart: Boolean
        do {
            val now = LocalDateTime.now()
            startTimes.removeIf { start -> start.until(now, ChronoUnit.SECONDS) > crashTimer }

            startServer()
            startTimes.add(now)

            LOGGER.info("Server has been stopped, it has started " + startTimes.size + " times in " + configFile.launch.crashTimer)


            shouldRestart = configFile.launch.autoRestart && startTimes.size <= configFile.launch.crashLimit
            if (shouldRestart) {
                LOGGER.info("Restarting server in 10 seconds, press ctrl+c to stop")
                try {
                    Thread.sleep(10_000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }

        } while (shouldRestart)
    }

    private fun checkEULA(basepath: String) {
        try {
            val eulaFile = File(basepath + "eula.txt")


            val lines: MutableList<String>
            if (eulaFile.exists()) {
                lines = FileUtils.readLines(eulaFile, "utf-8")
            } else {
                lines = ArrayList()
                Collections.addAll(lines,
                        "#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).",
                        "#" + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("E MMM d HH:mm:ss O y", Locale.ENGLISH)),
                        "eula=false")
            }


            if (lines.size > 2 && !lines[2].contains("true")) {
                Scanner(System.`in`, "utf-8").use { scanner ->

                    LOGGER.info(ansi().fgCyan().a("You have not accepted the eula yet."))
                    LOGGER.info(ansi().fgCyan().a("By typing TRUE you are indicating your agreement to the EULA of Mojang."))
                    LOGGER.info(ansi().fgCyan().a("Read it at https://account.mojang.com/documents/minecraft_eula before accepting it."))

                    val answer = scanner.nextLine()
                    if (answer.trim().equals("true", ignoreCase = true)) {
                        LOGGER.info("You have accepted the EULA.")
                        lines[2] = "eula=true\n"
                        FileUtils.writeLines(eulaFile, lines)
                    }
                }
            }

        } catch (e: IOException) {
            LOGGER.error("Error while checking EULA", e)
        }

    }

    fun installForge(basePath: String, forgeVersion: String, mcVersion: String) {
        val versionString = "$mcVersion-$forgeVersion"
        val url = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/$versionString/forge-$versionString-installer.jar"
        // http://files.minecraftforge.net/maven/net/minecraftforge/forge/1.12.2-14.23.3.2682/forge-1.12.2-14.23.3.2682-installer.jar
        val installerPath = File(basePath + "forge-" + versionString + "-installer.jar")


        try {
            LOGGER.info("Attempting to download forge installer from $url")
            InternetManager.downloadToFile(url, installerPath)

            LOGGER.info("Starting installation of Forge, installer output incoming")
            LOGGER.info("Check log for installer for more information", true)
            val installer = ProcessBuilder("java", "-jar", installerPath.absolutePath, "--installServer")
                    .inheritIO()
                    .directory(File("$basePath."))
                    .start()

            installer.waitFor()

            LOGGER.info("Done installing forge, deleting installer!")

            lockFile.forgeInstalled = true
            lockFile.forgeVersion = forgeVersion
            lockFile.mcVersion = mcVersion
            ServerStarter.saveLockFile(lockFile)


            installerPath.delete()


            checkEULA(basePath)
        } catch (e: IOException) {
            LOGGER.error("Problem while installing Forge", e)
        } catch (e: InterruptedException) {
            LOGGER.error("Problem while installing Forge", e)
        }

    }

    fun installSpongeBootstrapper(basePath: String): String {
        val filename = FilenameUtils.getName(configFile.install.spongeBootstrapper)
        val downloadFile = File(basePath + filename)

        try {
            InternetManager.downloadToFile(configFile.install.spongeBootstrapper, downloadFile)
        } catch (e: IOException) {
            LOGGER.error("Error while downloading bootstrapper", e)
        }

        return filename
    }

    private fun startServer() {

        try {
            val levelName = try {
                val props = Properties()
                File("server.properties").inputStream().use {
                    props.load(it)
                }

                props["level-name"] as String
            } catch (e: FileNotFoundException) {
                "world"
            }

            var filename =
                    if (configFile.launch.spongefix) {
                        lockFile.spongeBootstrapper
                    } else {
                        "forge-${lockFile.mcVersion}-${lockFile.forgeVersion}-universal.jar"
                    }
            if (!File(filename).exists()) {
                filename = "forge-${lockFile.mcVersion}-${lockFile.forgeVersion}.jar"
            }

            val launchJar = File(configFile.install.baseInstallPath + filename)
            val arguments = mutableListOf<String>()
            val ramPreArguments = mutableListOf<String>()
            val ramPostArguments = mutableListOf<String>()

            if (configFile.launch.ramDisk)
                if (OSUtil.isLinux) {
                    ramPreArguments.addAll(arrayOf("rsync", "-aAXv", "${levelName}_backup/", levelName))
                } else {
                    LOGGER.warn("Windows does not support RAMDisk yet!")
                }

            if (!configFile.launch.preJavaArgs.isEmpty()) {
                arguments.addAll(configFile.launch.preJavaArgs.trim().split(' ').dropWhile { it.isEmpty() })
            }

            arguments.add("java")
            arguments.addAll(configFile.launch.javaArgs)
            arguments.add("-Xmx${configFile.launch.maxRam}")

            if (configFile.launch.javaArgs.none { it.trim().startsWith("-Xms") }) {
                try {
                    val xmx = Integer.parseInt(configFile.launch.maxRam.substring(0, configFile.launch.maxRam.length - 1))
                    val xms = Math.max(1, xmx / 2)
                    val ending = configFile.launch.maxRam.substring(configFile.launch.maxRam.length - 1)
                    arguments.add("-Xms$xms$ending")

                } catch (e: NumberFormatException) {
                    LOGGER.error("Problem while calculating XMS", e)
                }

            }


            arguments.addAll(arrayOf("-jar", launchJar.absolutePath, "nogui"))

            if (configFile.launch.ramDisk)
                when (OSUtil.isLinux) {
                    true -> {
                        ramPostArguments.addAll(arrayOf("rsync", "-aAXv", "$levelName/", "${levelName}_backup"))
                    }
                    false -> {
                        LOGGER.warn("Windows does not support RAMDisk yet!")
                    }
                }

            LOGGER.info("Using arguments: $arguments", true)
            LOGGER.info("Starting Forge, output incoming")
            LOGGER.info("For output of this check the server log", true)
            if (configFile.launch.ramDisk)
                ProcessBuilder(ramPreArguments).apply {
                    inheritIO()
                    directory(File(configFile.install.baseInstallPath + "."))
                    start().apply {
                        waitFor()
                        outputStream.close()
                        errorStream.close()
                        inputStream.close()
                    }

                }

            ProcessBuilder(arguments).apply {
                inheritIO()
                directory(File(configFile.install.baseInstallPath + "."))
                start().apply {
                    waitFor()
                    outputStream.close()
                    errorStream.close()
                    inputStream.close()
                }

            }

            if (configFile.launch.ramDisk)
                ProcessBuilder(ramPostArguments).apply {
                    inheritIO()
                    directory(File(configFile.install.baseInstallPath + "."))
                    start().apply {
                        waitFor()
                        outputStream.close()
                        errorStream.close()
                        inputStream.close()
                    }

                }

        } catch (e: IOException) {
            LOGGER.error("Error while starting the server", e)
        } catch (e: InterruptedException) {
            LOGGER.error("Error while starting the server", e)
        }

    }
}
