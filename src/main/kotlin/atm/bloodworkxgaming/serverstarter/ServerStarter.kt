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


class ServerStarter(args: Array<String>) {
    companion object {
        private val rep: Representer = Representer()
        private val options: DumperOptions = DumperOptions()

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
                file = File("server-setup-config.yaml").inputStream().use { yaml.load<ConfigFile>(it) }
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

            info(ansi().fgRed().a("::::::::::::::::::::::::::::::::::::::::::::::::::::"))
            info(ansi().fgBrightBlue().a("   Minecraft-Forge Server install/launcher jar"))
            info(ansi().fgBrightBlue().a("   (Created by the ").fgGreen().a("\"Team RAM\"").fgBrightBlue().a(""))
            info(ansi().fgRed().a("::::::::::::::::::::::::::::::::::::::::::::::::::::"))
            info("")
            info("   This jar will launch a Minecraft Forge Modded server")
            info("")
            info(ansi().a("   Github:    ").fgBrightBlue().a("https://github.com/BloodyMods/ServerStarter"))
            info(ansi().a("   Discord:   ").fgBrightBlue().a("https://discord.gg/A3c5YfV"))
            info("")
            info(ansi().a("You are playing ").fgGreen().a(config.modpack.name))
            info("Starting to install/launch the server, lean back!")
            info(ansi().fgRed().a("::::::::::::::::::::::::::::::::::::::::::::::::::::"))
            info("")
        }
    }

    fun startLoading() {

        if (!InternetManager.checkConnection() && config.launch.checkOffline) {
            LOGGER.error("Problems with the Internet connection, shutting down.")
            System.exit(-1)
        }


        val forgeManager = ForgeManager(config)
        if (lockFile.checkShouldInstall(config) || installOnly) {
            val packtype = IPackType.createPackType(config.install.modpackFormat, config)
                    ?: throw InitException("Unknown pack format given in config, shutting down.")

            packtype.installPack()
            lockFile.packInstalled = true
            lockFile.packUrl = config.install.modpackUrl
            saveLockFile(lockFile)


            if (config.install.installForge) {
                val forgeVersion = packtype.getForgeVersion()
                val mcVersion = packtype.getMCVersion()
                forgeManager.installForge(config.install.baseInstallPath, forgeVersion, mcVersion)
            }

            if (config.launch.spongefix) {
                lockFile.spongeBootstrapper = forgeManager.installSpongeBootstrapper(config.install.baseInstallPath)
                saveLockFile(lockFile)
            }


            val filemanger = FileManager(config)
            filemanger.installAdditionalFiles()
            filemanger.installLocalFiles()


        } else {
            LOGGER.info("Server is already installed to correct version, to force install delete the serverstarter.lock File.")
        }

        if (installOnly) {
            LOGGER.info("Install only mod, exiting now.")
            System.exit(0)
        }

        forgeManager.handleServer()
    }
}

fun main(args: Array<String>) {
    AnsiConsole.systemInstall()

    try {
        val starter = ServerStarter(args)

        starter.greet()
        starter.startLoading()

    } catch (e: InitException) {
        ServerStarter.LOGGER.error(e.message)
    } catch (e: Throwable) {
        ServerStarter.LOGGER.error("Some uncaught error happened.", e)
    }
}

class InitException(s: String, e: Exception? = null) : RuntimeException(s, e)