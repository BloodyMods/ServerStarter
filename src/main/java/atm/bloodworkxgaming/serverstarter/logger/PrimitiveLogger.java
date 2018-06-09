package atm.bloodworkxgaming.serverstarter.logger;

import org.apache.commons.io.FileUtils;
import org.fusesource.jansi.Ansi;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static org.fusesource.jansi.Ansi.ansi;

public class PrimitiveLogger {
    private File outputFile;
    private Pattern pattern = Pattern.compile("\\x1b\\[[0-9;]*m");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public PrimitiveLogger(File outputFile) {
        this.outputFile = outputFile;
        if (outputFile.exists()) {
            outputFile.delete();
        }
    }

    public void info(Object message) {
        info(message, false);
    }

    public void info(Object message, boolean logOnly) {
        String m = currentTimeAnsi().fgYellow().a("[INFO] ").fgDefault().a(message).reset().newline().toString();

        synchronized (this) {
            try {
                FileUtils.write(outputFile, stripColors(m), "utf-8", true);
            } catch (IOException e) {
                error("Error while logging!", e);
            }

            if (!logOnly) {
                System.out.print(m);
            }
        }
    }

    public void warn(Object message) {
        String m = currentTimeAnsi().fgMagenta().a("[WARNING] ").bgDefault().a(message).reset().newline().toString();

        synchronized (this) {
            try {
                FileUtils.write(outputFile, stripColors(m), "utf-8", true);
            } catch (IOException e) {
                error("Error while logging!", e);
            }

            System.out.print(m);
        }
    }

    public void error(String message, Throwable throwable) {
        String m = currentTimeAnsi().fgRed().a("[ERROR] ").bgDefault().a(message).reset().newline().toString();

        if (throwable != null) {
            throwable.printStackTrace();
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            m += "\n" + sw.toString();
        }

        synchronized (this) {
            try {
                FileUtils.write(outputFile, stripColors(m), "utf-8", true);
            } catch (IOException e) {
                System.err.println("Error while logging!");
                e.printStackTrace();
            }

            System.out.print(m);
        }
    }

    public void error(String message) {
        error(message, null);
    }

    private String stripColors(String message) {
        return pattern.matcher(message).replaceAll("");
    }

    private Ansi currentTimeAnsi() {
        return ansi().fgBrightBlack().a("[" + LocalTime.now().format(dateTimeFormatter) + "] ").fgDefault();
    }
}
