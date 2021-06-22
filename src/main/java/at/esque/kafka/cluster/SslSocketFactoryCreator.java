package at.esque.kafka.cluster;

import at.esque.kafka.handlers.ConfigHandler;
import org.apache.kafka.common.config.SslConfigs;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;

public class SslSocketFactoryCreator {

    private SslSocketFactoryCreator() {
    }

    public static SSLSocketFactory buildSSlSocketFactory(ClusterConfig clusterConfig, ConfigHandler configHandler) {
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            Map<String, String> sslProperties = configHandler.getSslProperties(clusterConfig);
            ks.load(new FileInputStream(sslProperties.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG)), sslProperties.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG).toCharArray());

            KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
            ts.load(new FileInputStream(sslProperties.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG)), sslProperties.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG).toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, sslProperties.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG).toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return sc.getSocketFactory();

        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
            at.esque.kafka.alerts.ErrorAlert.show(e);
            return null;
        }
    }
}
