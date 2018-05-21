package atm.bloodworkxgaming.serverstarter.config;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@EqualsAndHashCode
@ToString
public class LaunchSettings {
    public boolean spongefix;
    public boolean checkOffline;
    public String maxRam;

    public boolean autoRestart;
    public int crashLimit;
    public String crashTimer;
    public List<String> javaArgs;

    public LaunchSettings normalize(){
        if (javaArgs == null) javaArgs = Collections.emptyList();

        return this;
    }
}
