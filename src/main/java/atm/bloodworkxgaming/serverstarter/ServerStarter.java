package atm.bloodworkxgaming.serverstarter;

import atm.bloodworkxgaming.serverstarter.config.ConfigFile;
import atm.bloodworkxgaming.serverstarter.config.LockFile;
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
    private static Representer rep;
    private static DumperOptions options;
    public static LockFile lockFile = null;

    static {
        rep = new Representer();
        options = new DumperOptions();
        rep.addClassTag(ConfigFile.class, Tag.MAP);
        rep.addClassTag(LockFile.class, Tag.MAP);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.AUTO);
    }

    public static void main(String[] args) {
        ConfigFile config = readConfig();
        lockFile = readLockFile();

        if (config == null || lockFile == null) {
            System.err.println("[Error] One file is null: config: " + config + " lock: " + lockFile);
            return;
        }

        // normalize the file so it can be used
        if (config.install.baseInstallPath == null) config.install.baseInstallPath = "";


        if (checkShouldInstall(config)) {
            IPackType packtype = TypeFactory.createPackType(config.install.modpackFormat, config);
            if (packtype != null)
                packtype.installPack();
            else
                System.out.println("Unknown pack format given in config");
        }
    }

    public static ConfigFile readConfig() {
        Yaml yaml = new Yaml(new Constructor(ConfigFile.class), rep, options);
        try {
            return yaml.load(new FileInputStream(new File("server-setup-config.yaml")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static LockFile readLockFile() {
        Yaml yaml = new Yaml(new Constructor(LockFile.class), rep, options);
        File file = new File("serverstarter.lock");

        if (file.exists()) {
            try {
                return  yaml.load(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                return new LockFile();
            }
        } else {
            return new LockFile();
        }
    }

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

    private static boolean checkShouldInstall(ConfigFile configFile) {
        return !lockFile.forgeInstalled
                || !lockFile.packInstalled
                || !lockFile.forgeVersion.equals(configFile.install.forgeVersion)
                || !lockFile.packUrl.equals(configFile.install.modpackUrl);
    }
}
