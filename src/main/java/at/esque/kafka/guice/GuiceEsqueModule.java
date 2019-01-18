package at.esque.kafka.guice;

import at.esque.kafka.guice.provider.FXMLLoaderProvider;
import com.google.inject.AbstractModule;
import javafx.fxml.FXMLLoader;

public class GuiceEsqueModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(FXMLLoader.class).toProvider(FXMLLoaderProvider.class);
    }
}
