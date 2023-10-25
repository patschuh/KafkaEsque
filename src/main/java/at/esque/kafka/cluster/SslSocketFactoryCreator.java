package at.esque.kafka.cluster;

import at.esque.kafka.handlers.ConfigHandler;
import org.apache.kafka.common.config.SslConfigs;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

public class SslSocketFactoryCreator {

    private SslSocketFactoryCreator() {
    }

    private static final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
    };

    public static SSLSocketFactory buildSSlSocketFactory(ClusterConfig clusterConfig, ConfigHandler configHandler) {
        try {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            Map<String, String> sslProperties = configHandler.getSslProperties(clusterConfig);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

            if (useKeyStore(configHandler.getSslProperties(clusterConfig))) {
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(new FileInputStream(sslProperties.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG)), sslProperties.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG).toCharArray());
                kmf.init(ks, sslProperties.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG).toCharArray());
            }else{
                kmf.init(null, null);
            }

            if (clusterConfig.isSchemaRegistrySuppressCertPathValidation()) {
                sc.init(kmf.getKeyManagers(), UNQUESTIONING_TRUST_MANAGER, null);
            } else {
                KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
                ts.load(new FileInputStream(sslProperties.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG)), sslProperties.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG).toCharArray());
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ts);

                sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            }

            return sc.getSocketFactory();
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException |
                 UnrecoverableKeyException | KeyManagementException e) {
            at.esque.kafka.alerts.ErrorAlert.show(e);
            return null;
        }
    }

    private static boolean useKeyStore(Map<String, String> sslProperties) {
        return sslProperties.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG) != null && sslProperties.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG) != null;
    }
}
