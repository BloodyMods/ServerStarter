package atm.bloodworkxgaming.serverstarter;

import atm.bloodworkxgaming.serverstarter.config.ConfigFile;
import atm.bloodworkxgaming.serverstarter.packtype.IPackType;
import atm.bloodworkxgaming.serverstarter.packtype.TypeFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class ServerStarter {
    public static void main(String[] args) {
        Constructor constructor = new Constructor(ConfigFile.class);
        Representer rep = new Representer();
        DumperOptions options = new DumperOptions();
        rep.addClassTag(ConfigFile.class, Tag.MAP);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.AUTO);


        Yaml yaml = new Yaml(constructor);
        try {
            ConfigFile obj = yaml.load(new FileInputStream(new File("D:\\Users\\jonas\\Documents\\GitHub\\serverstarter\\server-setup-config.yaml")));
            System.out.println("obj = " + obj);

            IPackType packtype = TypeFactory.createPackType(obj.install.modpackFormat);
            if (packtype != null)
                packtype.handleConfig(obj);
            else
                System.out.println("Unknown pack format given in config");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
