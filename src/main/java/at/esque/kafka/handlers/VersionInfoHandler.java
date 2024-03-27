package at.esque.kafka.handlers;

import at.esque.kafka.alerts.*;
import at.esque.kafka.alerts.model.UpdateDialogResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.application.HostServices;
import javafx.application.Platform;
import okhttp3.Call;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;
import org.gradle.util.VersionNumber;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
        if (Settings.isCheckForUpdatesEnabled(configHandler.getSettingsProperties())) {
            final Map<String, Object> versionCheckContent = configHandler.getVersionCheckContent();
            if (versionCheckContent == null || Duration.between(Instant.parse((String) versionCheckContent.get("checkTime")), Instant.now()).toHours() > Long.parseLong(configHandler.getSettingsProperties().get(Settings.CHECK_FOR_UPDATES_DURATION_BETWEEN_HOURS))) {
                final ConnectionSpec connectionSpec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .cipherSuites(
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384,
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                        CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384,
                        CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA256,
                        CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA256,
                        CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
                        CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA
                    ).build();

                OkHttpClient client = new OkHttpClient.Builder()
                    .connectionSpecs(List.of(connectionSpec))
                    .build();

                Request request = new Request.Builder()
                    .url(GITHUB_LATEST_RELEASE_URL)
                    .addHeader("User-Agent", "patschuh/KafkaEsque")
                    .method("GET", null)
                    .build();


                Call call = client.newCall(request);
                try (Response response = call.execute()) {

                    if (!response.isSuccessful()) {
                        return null;
                    }
                    final Map<String, Object> responseRelease = objectMapper.readValue(response.body().byteStream(), Map.class);
                    Map<String, Object> checkContent = new HashMap<>();
                    checkContent.put("checkTime", Instant.now().toString());
                    checkContent.put("release", responseRelease);
                    configHandler.writeVersionCheckContent(checkContent);
                    return responseRelease;


                } catch (Exception e) {
                    Platform.runLater(() -> ErrorAlert.show("Update Check failed", "Failed to check for availabe Updates", e.getMessage(), e, null, false));
                }
            } else {
                return (Map<String, Object>) versionCheckContent.get("release");
            }
        }
        return null;
    }

    public void showDialogIfUpdateIsAvailable(HostServices hostServices) {
        final String askLaterFieldName = "showUpdateDialogAgainTimestamp";
        int showUpdateDialogTimestamp = Optional.ofNullable(configHandler.getVersionCheckContent())
            .map(el -> (Integer) el.get(askLaterFieldName))
            .orElse(0);
        if (showUpdateDialogTimestamp > Instant.now().getEpochSecond()) {
            return;
        }

        final UpdateInfo updateInfo = availableUpdate();
        if (updateInfo != null) {
            final UpdateDialogResult action = UpdateAlert.show("Update Available", "Version " + updateInfo.getTag() + " is available", "Do you want to open the release page?");
            if (UpdateDialogResult.OPEN.equals(action)) {
                try {
                    hostServices.showDocument(updateInfo.getReleasePage());
                } catch (Exception e) {
                    ErrorAlert.show(e);
                }
            } else if (UpdateDialogResult.REMIND_LATER.equals(action)) {
                Map<String, Object> versionCheckContent = Optional.ofNullable(configHandler.getVersionCheckContent()).orElse(new HashMap<>());
                versionCheckContent.put(askLaterFieldName, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
                configHandler.writeVersionCheckContent(versionCheckContent);
            }
        }
    }
}
