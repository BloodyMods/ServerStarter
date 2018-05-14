package atm.bloodworkxgaming.serverstarter;

import atm.bloodworkxgaming.serverstarter.config.ConfigFile;
import atm.bloodworkxgaming.serverstarter.config.LockFile;
import atm.bloodworkxgaming.serverstarter.logger.PrimitiveLogger;
import atm.bloodworkxgaming.serverstarter.packtype.IPackType;
import atm.bloodworkxgaming.serverstarter.packtype.TypeFactory;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class ServerStarter {
    public static final PrimitiveLogger LOGGER = new PrimitiveLogger(new File("serverstarter.log"));
    public static LockFile lockFile = null;
    private static Representer rep;
    private static DumperOptions options;

    static {
        rep = new Representer();
        options = new DumperOptions();
        rep.addClassTag(ConfigFile.class, Tag.MAP);
        rep.addClassTag(LockFile.class, Tag.MAP);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
    }

    public static void main(String[] args) {
        ConfigFile config = readConfig().normalize();
        lockFile = readLockFile();

        boolean installOnly = args.length > 0 && args[0].equals("install");

        LOGGER.info("ConfigFile: " + config, true);
        LOGGER.info("LockFile: " + lockFile, true);

        if (config == null || lockFile == null) {
            LOGGER.error("One file is null: config: " + config + " lock: " + lockFile);
            return;
        }

        ForgeManager forgeManager = new ForgeManager(config);


        if (lockFile.checkShouldInstall(config) || installOnly) {
            IPackType packtype = TypeFactory.createPackType(config.install.modpackFormat, config);
            if (packtype == null) {
                LOGGER.error("Unknown pack format given in config");
                return;
            }

            packtype.installPack();
            lockFile.packInstalled = true;
            lockFile.packUrl = config.install.modpackUrl;
            saveLockFile(lockFile);


            String forgeVersion = packtype.getForgeVersion();
            String mcVersion = packtype.getMCVersion();
            forgeManager.installForge(config.install.baseInstallPath, forgeVersion, mcVersion);

            FileManager filemanger = new FileManager(config);
            filemanger.installAdditionalFiles();


        } else {
            LOGGER.info("Server is already installed to correct version, to force install delete the serverstarter.lock File.");
        }

        if (installOnly) {
            LOGGER.info("Install only mod, exiting now.");
            return;
        }

        forgeManager.handleServer();
    }


    /**
     * Reads the config and parses the config
     *
     * @return the configfile object
     */
    public static ConfigFile readConfig() {
        Yaml yaml = new Yaml(new Constructor(ConfigFile.class), rep, options);
        try {
            return yaml.load(new FileInputStream(new File("server-setup-config.yaml")));
        } catch (FileNotFoundException e) {
            LOGGER.error("There is no config file given.", e);
            throw new RuntimeException("No config file given.");
        }
    }

    /**
     * Reads the lockfile if present, returns a new if not
     */
    public static LockFile readLockFile() {
        Yaml yaml = new Yaml(new Constructor(LockFile.class), rep, options);
        File file = new File("serverstarter.lock");

        if (file.exists()) {
            try {
                return yaml.load(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                return new LockFile();
            }
        } else {
            return new LockFile();
        }
    }

    /**
     * Writes the lockfile to disk
     *
     * @param lockFile lockfile to write
     */
    public static void saveLockFile(LockFile lockFile) {
        Yaml yaml = new Yaml(new Constructor(LockFile.class), rep, options);
        File file = new File("serverstarter.lock");
        ServerStarter.lockFile = lockFile;

        String lock = "#Auto genereated file, DO NOT EDIT!\n" + yaml.dump(lockFile);
        try {
            FileUtils.write(file, lock, "utf-8", false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
