package atm.bloodworkxgaming.serverstarter;

import atm.bloodworkxgaming.serverstarter.config.ConfigFile;
import org.apache.commons.io.FileUtils;
import org.fusesource.jansi.Ansi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static atm.bloodworkxgaming.serverstarter.ServerStarter.*;
import static org.fusesource.jansi.Ansi.ansi;

public class ForgeManager {
    private ConfigFile configFile;

    public ForgeManager(ConfigFile config) {
        this.configFile = config;
    }

    public void handleServer() {
        List<LocalDateTime> starttimes = new ArrayList<>();
        long _crashTimer = -1;
        String timerString = configFile.launch.crashTimer;

        try {
            if (timerString.endsWith("h"))
                _crashTimer = Long.parseLong(timerString.substring(0, timerString.length() - 1)) * 60 * 60;
            else if (timerString.endsWith("min"))
                _crashTimer = Long.parseLong(timerString.substring(0, timerString.length() - 3)) * 60;
            else if (timerString.endsWith("s"))
                _crashTimer = Long.parseLong(timerString.substring(0, timerString.length() - 1));
            else
                _crashTimer = Long.parseLong(timerString);
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid crash time format given", e);
        }

        final long crashTimer = _crashTimer;

        boolean shouldRestart;
        do {
            LocalDateTime now = LocalDateTime.now();
            starttimes.removeIf(start -> start.until(now, ChronoUnit.SECONDS) > crashTimer);

            startServer();
            starttimes.add(now);

            LOGGER.info("Server has been stopped, it has started " + starttimes.size() + " times in " + configFile.launch.crashTimer);


            shouldRestart = configFile.launch.autoRestart && starttimes.size() <= configFile.launch.crashLimit;
            if (shouldRestart) {
                LOGGER.info("Restarting server in 10 seconds, press ctrl+c to stop");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } while (shouldRestart);
    }

    private void checkEULA(String basepath) {
        try {
            File eulaFile = new File(basepath + "eula.txt");


            List<String> lines;
            if (eulaFile.exists()) {
                lines = FileUtils.readLines(eulaFile, "utf-8");
            } else {
                lines = new ArrayList<>();
                Collections.addAll(lines,
                        "#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).",
                        "#" + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("E MMM d HH:mm:ss O y", Locale.ENGLISH)),
                        "eula=false");
            }


            if (lines.size() > 2 && !lines.get(2).contains("true")) {
                try (Scanner scanner = new Scanner(System.in, "utf-8")) {

                    LOGGER.info(ansi().fgCyan().a("You have not accepted the eula yet."));
                    LOGGER.info(ansi().fgCyan().a("By typing TRUE you are indicating your agreement to the EULA of Mojang."));
                    LOGGER.info(ansi().fgCyan().a("Read it at https://account.mojang.com/documents/minecraft_eula before accepting it."));

                    String answer = scanner.nextLine();
                    if (answer.trim().equalsIgnoreCase("true")) {
                        LOGGER.info("You have accepted the EULA.");
                        lines.set(2, "eula=true\n");
                        FileUtils.writeLines(eulaFile, lines);
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.error("Error while checking EULA", e);
        }
    }

    public void installForge(String basePath, String forgeVersion, String mcVersion) {
        String temp = mcVersion + "-" + forgeVersion;
        String url = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/" + temp + "/forge-" + temp + "-installer.jar";
        // http://files.minecraftforge.net/maven/net/minecraftforge/forge/1.12.2-14.23.3.2682/forge-1.12.2-14.23.3.2682-installer.jar
        File installerPath = new File(basePath + "forge-" + temp + "-installer.jar");


        try {
            LOGGER.info("Attempting to download forge installer from " + url);
            FileUtils.copyURLToFile(new URL(url), installerPath);

            LOGGER.info("Starting installation of Forge, installer output incoming");
            LOGGER.info("Check log for installer for more information", true);
            Process installer = new ProcessBuilder("java", "-jar", installerPath.getAbsolutePath(), "--installServer")
                    .inheritIO()
                    .directory(new File(basePath + "."))
                    .start();

            installer.waitFor();

            LOGGER.info("Done installing forge, deleting installer!");

            lockFile.forgeInstalled = true;
            lockFile.forgeVersion = forgeVersion;
            lockFile.mcVersion = mcVersion;
            saveLockFile(lockFile);

            //noinspection ResultOfMethodCallIgnored
            installerPath.delete();


            checkEULA(basePath);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Problem while installing Forge", e);
        }
    }

    private void startServer() {

        try {
            String filename = "forge-" + lockFile.mcVersion + "-" + lockFile.forgeVersion + "-universal.jar";
            File forgeUniversal = new File(configFile.install.baseInstallPath + filename);

            List<String> arguments = new ArrayList<>();

            arguments.add(configFile.launch.preJavaArgs);
            arguments.add("java");
            arguments.addAll(configFile.launch.javaArgs);
            arguments.add("-Xmx" + configFile.launch.maxRam);

            if (configFile.launch.javaArgs.stream().noneMatch(s -> s.trim().startsWith("-Xms"))){
                try {
                    int xmx = Integer.parseInt(configFile.launch.maxRam.substring(0, configFile.launch.maxRam.length() - 1));
                    int xms = Math.max(1, xmx / 2);
                    arguments.add("-Xms" + xms + configFile.launch.maxRam.substring(configFile.launch.maxRam.length() - 1));

                } catch (NumberFormatException e) {
                    LOGGER.error("Problem while calculating XMS", e);
                }
            }


            Collections.addAll(arguments, "-jar", forgeUniversal.getAbsolutePath());
            arguments.add("nogui");

            LOGGER.info("Using arguments: " + arguments.toString(), true);
            LOGGER.info("Starting Forge, output incoming");
            LOGGER.info("For output of this check the server log", true);
            Process process = new ProcessBuilder(arguments)
                    .inheritIO()
                    .directory(new File(configFile.install.baseInstallPath + "."))
                    .start();


            process.waitFor();

            process.getOutputStream().close();
            process.getErrorStream().close();
            process.getInputStream().close();

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error while starting the server", e);
        }
    }
}
