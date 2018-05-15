package atm.bloodworkxgaming.serverstarter;

import java.io.IOException;
import java.net.InetAddress;

import static atm.bloodworkxgaming.serverstarter.ServerStarter.LOGGER;

public class InternetChecker {
    private static String[] urls = {"8.8.8.8", "1.1.1.1"};

    public static boolean checkConnection() {
        int reached = 0;

        for (String url : urls) {
            try {
                LOGGER.info("Pinging " + url + ".");
                boolean r = InetAddress.getByName(url).isReachable(1000);
                LOGGER.info("Reached " + url + ": " + r);
                if (r) reached++;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        LOGGER.info("Reached " + reached + " out of " + urls.length + " IPs.");
        if (reached != urls.length) {
            LOGGER.error("Not every host could be reached. The could be a problem with your internet connection!!!!");
            return false;
        }

        return true;
    }
}
