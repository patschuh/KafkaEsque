package at.esque.kafka.handlers;

import at.esque.kafka.alerts.ConfirmationAlert;
import at.esque.kafka.alerts.ErrorAlert;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.application.Platform;
import org.gradle.util.VersionNumber;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Singleton
public class VersionInfoHandler {

    private static final String GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/patschuh/KafkaEsque/releases/latest";
    private static final String TAG_NAME = "tag_name";
    private static final String HTML_URL = "html_url";

    @Inject
    private ConfigHandler configHandler;

    private VersionInfo versionInfo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VersionInfoHandler() {
        objectMapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);
        try (InputStream resourceAsStream = getClass().getResourceAsStream("/version.json")) {
            versionInfo = objectMapper.readValue(resourceAsStream, VersionInfo.class);
        } catch (Exception e) {
            Platform.runLater(() -> ErrorAlert.show(e));
        }
    }

    public VersionInfo getVersionInfo() {
        return versionInfo;
    }

    public UpdateInfo availableUpdate() {
        final Map<String, Object> latestVersion = checkLatestVersion();
        final VersionNumber currentVersionNumber = versionInfo.releaseVersion();
        if (latestVersion != null && currentVersionNumber != null) {
            final VersionNumber latestVersionNumber = VersionNumber.parse(((String) latestVersion.get(TAG_NAME)).substring(1));
            final int i = currentVersionNumber.compareTo(latestVersionNumber);
            if (i < 0) {
                return new UpdateInfo((String) latestVersion.get(TAG_NAME), (String) latestVersion.get(HTML_URL));
            }
        }
        return null;
    }

    private Map<String, Object> checkLatestVersion() {
        if(Settings.isCheckForUpdatesEnabled(configHandler.getSettingsProperties())) {
            final Map<String, Object> versionCheckContent = configHandler.getVersionCheckContent();
            if (versionCheckContent == null || Duration.between(Instant.parse((String) versionCheckContent.get("checkTime")), Instant.now()).toHours() > Long.parseLong(configHandler.getSettingsProperties().get(Settings.CHECK_FOR_UPDATES_DURATION_BETWEEN_HOURS))) {
                try {
                    URL url = new URL(GITHUB_LATEST_RELEASE_URL);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("User-Agent", "patschuh/KafkaEsque");
                    con.setConnectTimeout(5000);
                    con.setReadTimeout(5000);
                    final int responseCode = con.getResponseCode();
                    if (responseCode != 200) {
                        return null;
                    }
                    final Map<String, Object> response = objectMapper.readValue(con.getInputStream(), Map.class);
                    Map<String, Object> checkContent = new HashMap<>();
                    checkContent.put("checkTime", Instant.now().toString());
                    checkContent.put("release", response);
                    configHandler.writeVersionCheckContent(checkContent);
                    return response;
                } catch (Exception e) {
                    Platform.runLater(() -> ErrorAlert.show(e));
                }
            } else {
                return (Map<String, Object>) versionCheckContent.get("release");
            }
        }
        return null;
    }

    public void showDialogIfUpdateIsAvailable() {
        final UpdateInfo updateInfo = availableUpdate();
        if(updateInfo != null){
            final boolean openInBrowser = ConfirmationAlert.show("Update Available", "Version " + updateInfo.getTag() + " is available", "Do you want to open the release page?");
            if(openInBrowser){
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI(updateInfo.getReleasePage()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
