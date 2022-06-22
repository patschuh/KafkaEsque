package at.esque.kafka;

import ch.qos.logback.classic.util.ContextInitializer;

import java.io.File;

public class Launcher {

    static {
        File logbackFile = new File(String.format(System.getProperty("user.home") + "/.kafkaesque/%s", "/logback.xml"));
        if (logbackFile.exists()) {
            System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, logbackFile.getAbsolutePath());
        }
    }

    public static void main(String[] args) {
        Main.main(args);
    }

}
