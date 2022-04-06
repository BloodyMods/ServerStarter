package atm.bloodworkxgaming.serverstarter

import atm.bloodworkxgaming.serverstarter.config.ConfigFile
import atm.bloodworkxgaming.serverstarter.config.LockFile
import atm.bloodworkxgaming.serverstarter.logger.PrimitiveLogger
import atm.bloodworkxgaming.serverstarter.packtype.IPackType
import atm.bloodworkxgaming.serverstarter.yaml.CustomConstructor
import org.apache.commons.io.FileUtils
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.system.exitProcess


class ServerStarter(args: Array<String>) {
    companion object {
        private val rep: Representer = Representer()
        private val options: DumperOptions = DumperOptions()
        private const val CURRENT_SPEC = 2
        private const val VERSION = "2.3.0"

        val LOGGER = PrimitiveLogger(File("serverstarter.log"))
        var lockFile: LockFile
            private set
        val config: ConfigFile

        init {
            rep.addClassTag(ConfigFile::class.java, Tag.MAP)
            rep.addClassTag(LockFile::class.java, Tag.MAP)
            options.defaultFlowStyle = DumperOptions.FlowStyle.FLOW

            try {
                lockFile = readLockFile()
                config = readConfig()
            } catch (e: Exception) {
                LOGGER.error("Failed to load Yaml", e)
                throw InitException("Failed to load class", e)
            }

            if (config._specver < CURRENT_SPEC) {
                LOGGER.error(ansi().bgRed().fgBlack().a("You are loading with an older Version of the specification!!"))
                LOGGER.error(ansi().bgRed().fgBlack().a("Make sure you have updated your config.yaml file!"))
            }
        }

        /**
         * Reads the config and parses the config
         *
         * @return the configfile object
         */
        @Throws(RuntimeException::class)
        private fun readConfig(): ConfigFile {
            val yaml = Yaml(CustomConstructor(ConfigFile::class.java), rep, options)

            val file: ConfigFile

            try {
                file = File("server-setup-config.yaml").inputStream().use { yaml.load(it) }
            } catch (e: FileNotFoundException) {
                LOGGER.error("There is no config file given.", e)
                throw RuntimeException("No config file given.", e)
            } catch (e: IOException) {
                LOGGER.error("Could not read config file.", e)
                throw RuntimeException("Failed to read config file", e)
            }

            if (file == null)
                throw RuntimeException("Config file was null while reading.")

            return file
        }

        /**
         * Reads the lockfile if present, returns a new if not
         */
        private fun readLockFile(): LockFile {
            val yaml = Yaml(Constructor(LockFile::class.java), rep, options)
            val file = File("serverstarter.lock")

            if (file.exists()) {
                try {
                    FileInputStream(file).use { stream -> return yaml.load(stream) }
                } catch (e: FileNotFoundException) {
                    return LockFile()
                } catch (e: IOException) {
                    LOGGER.error("Error while reading Lock file", e)
                    throw RuntimeException("Error while reading Lock file", e)
                }

            } else {
                return LockFile()
            }
        }

        /**
         * Writes the lockfile to disk
         *
         * @param lockFile lockfile to write
         */
        fun saveLockFile(lockFile: LockFile) {
            val yaml = Yaml(Constructor(LockFile::class.java), rep, options)
            val file = File("serverstarter.lock")
            this.lockFile = lockFile

            val lock = "#Auto genereated file, DO NOT EDIT!\n" + yaml.dump(lockFile)
            try {
                FileUtils.write(file, lock, "utf-8", false)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private val installOnly: Boolean = args.getOrNull(0) == "install"

    fun greet() {
        LOGGER.run {
            info("ConfigFile: $config", true)
            info("LockFile: $lockFile", true)

            info(ansi().fgRed().a(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::"))
            info(ansi().fgBrightBlue().a("   Minecraft ServerStarter install/launcher jar"))
            info(ansi().fgBrightBlue().a("   (Created by ").fgGreen().a("BloodWorkXGaming").fgBrightBlue().a(" with the help of ").fgGreen().a("Contributors").fgBrightBlue().a(")"))
            info(ansi().fgBrightBlue().a("   Version $VERSION"))
            info(ansi().fgRed().a(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::"))
            info("")
            info("   This jar will launch a Minecraft Forge/Fabric Modded server")
            info("")
            info(ansi().a("   Github:    ").fgBrightBlue().a("https://github.com/BloodyMods/ServerStarter"))
            info(ansi().a("   Discord:   ").fgBrightBlue().a("https://discord.gg/A3c5YfV"))
            info("")
            info(ansi().a("You are playing ").fgGreen().a(config.modpack.name))
            info("Starting to install/launch the server, lean back!")
            info(ansi().fgRed().a(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::"))
            info("")
        }
    }

    fun startLoading() {

        val internetManager = InternetManager(config)

        if (!internetManager.checkConnection() && config.launch.checkOffline) {
            LOGGER.error("Problems with the Internet connection, please check your connection!\n" +
                    "This could however be a problem with the servers as well.\n" +
                    "Continuing to download, but this may fail!")
        }


        val forgeManager = LoaderManager(config, internetManager)
        if (lockFile.checkShouldInstall(config) || installOnly) {
            val packtype = IPackType.createPackType(config.install.modpackFormat, config, internetManager)
                    ?: throw InitException("Unknown pack format given in config, shutting down.")

            packtype.installPack()
            lockFile.packInstalled = true
            lockFile.packUrl = config.install.modpackUrl
            saveLockFile(lockFile)


            if (config.install.installLoader) {
                val forgeVersion = packtype.getForgeVersion()
                val mcVersion = packtype.getMCVersion()
                forgeManager.installLoader(config.install.baseInstallPath, forgeVersion, mcVersion)
            }

            if (config.launch.spongefix) {
                lockFile.spongeBootstrapper = forgeManager.installSpongeBootstrapper(config.install.baseInstallPath)
                saveLockFile(lockFile)
            }


            val fileManager = FileManager(config, internetManager)
            fileManager.installAdditionalFiles()
            fileManager.installLocalFiles()


        } else {
            LOGGER.info("Server is already installed to correct version, to force install delete the serverstarter.lock File.")
        }

        if (installOnly) {
            LOGGER.info("Install only mod, exiting now.")
            exitProcess(0)
        }

        forgeManager.handleServer()
    }
}

fun main(args: Array<String>) {
    // System.setProperty("jansi.passthrough", "true")
    try {
        AnsiConsole.systemInstall()
    } catch (e: Exception) {
        println("jansi couldn't be installed in this terminal (e.g. due to aarch64 not being supported)\n" +
                "Future terminal messages will have no color.")
    }

    try {
        val starter = ServerStarter(args)

        starter.greet()
        starter.startLoading()

    } catch (e: InitException) {
        ServerStarter.LOGGER.error(e.message)
    } catch (e: DownloadLoaderException) {
        ServerStarter.LOGGER.error("Stopping the process as downloading the ModLoader failed", e)
    } catch (e: Throwable) {
        ServerStarter.LOGGER.error("Some uncaught error happened.", e)
    }
}

class InitException(s: String, e: Exception? = null) : RuntimeException(s, e)
