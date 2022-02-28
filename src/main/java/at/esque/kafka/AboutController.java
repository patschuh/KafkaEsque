package at.esque.kafka;

import at.esque.kafka.handlers.VersionInfo;
import at.esque.kafka.handlers.VersionInfoHandler;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AboutController {
    public ImageView imageView;
    public HBox infoContainer;
    public Text infoText;
    public Label releaseVersion;
    public Label runtimeVersion;
    public Label buildJvm;
    public Label buildTime;

    public void setup(VersionInfoHandler versionInfoHandler) {
        final VersionInfo versionInfo = versionInfoHandler.getVersionInfo();
        final String tag = versionInfo.getTag();
        infoContainer.setVisible(false);
        if(tag != null){
            releaseVersion.setText(tag);
        } else {
            releaseVersion.setText(versionInfo.getVersion() +" (" + versionInfo.getRevision() + ")");
            infoContainer.setVisible(true);
            infoText.setText("This is not a release version!\nNo update checks will be performed");
        }
        buildJvm.setText(versionInfo.getBuildJvm());
        buildTime.setText(versionInfo.getBuildTime());
        runtimeVersion.setText(System.getProperty("java.version"));
    }

    public void clickGithubLink(MouseEvent mouseEvent) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/patschuh/KafkaEsque"));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}
