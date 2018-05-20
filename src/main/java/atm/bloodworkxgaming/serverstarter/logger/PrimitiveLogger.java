package atm.bloodworkxgaming.serverstarter.logger;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class PrimitiveLogger {
    private File outputFile;

    public PrimitiveLogger(File outputFile) {
        this.outputFile = outputFile;
        if (outputFile.exists()) {
            outputFile.delete();
        }
    }

    public void info(String message) {
        info(message, false);
    }

    public void info(String message, boolean logOnly) {
        message = "[INFO] " + message + "\n";

        try {
            FileUtils.write(outputFile, message, "utf-8", true);
        } catch (IOException e) {
            error("Error while logging!", e);
        }

        if (!logOnly) {
            System.out.print(message);
        }
    }

    public void warn(String message) {
        message = "[WARNING] " + message + "\n";

        try {
            FileUtils.write(outputFile, message, "utf-8", true);
        } catch (IOException e) {
            error("Error while logging!", e);
        }

        System.out.print(message);
    }


    public void error(String message, Throwable throwable) {

        message = "[ERROR] " + message + "\n";
        if (throwable != null) {
            throwable.printStackTrace();
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            message += "\n" + sw.toString();
        }

        try {
            FileUtils.write(outputFile, message, "utf-8", true);
        } catch (IOException e) {
            System.err.println("Error while logging!");
            e.printStackTrace();
        }

        System.out.print(message);

    }

    public void error(String message) {
        error(message, null);
    }
}
