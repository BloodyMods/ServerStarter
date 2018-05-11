package atm.bloodworkxgaming.serverstarter.packtype;

import atm.bloodworkxgaming.serverstarter.config.ConfigFile;

public interface IPackTypeFactory {
    IPackType createPackType(ConfigFile configFile);
}
