package atm.bloodworkxgaming.serverstarter.packtype;

import atm.bloodworkxgaming.serverstarter.packtype.curse.CursePackType;

import java.util.HashMap;

public class TypeFactory {
    public static final HashMap<String, IPackTypeFactory> packtype = new HashMap<>();

    static {
        registerPackType("curse", CursePackType::new);
        registerPackType("curseforge", CursePackType::new);
    }

    public static IPackType createPackType(String packTypeName) {
        IPackTypeFactory fact = packtype.get(packTypeName);
        return fact == null ? null : fact.createPackType();
    }

    public static void registerPackType(String name, IPackTypeFactory type) {
        packtype.put(name, type);
    }
}
