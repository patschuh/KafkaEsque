package at.esque.kafka.cluster;

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

public class SslSocketFactoryCreator {

    private SslSocketFactoryCreator() {
    }

    public static SSLSocketFactory buildSSlSocketFactory(ClusterConfig clusterConfig) {
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new FileInputStream(clusterConfig.getKeyStoreLocation()), clusterConfig.getKeyStorePassword().toCharArray());

            KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
            ts.load(new FileInputStream(clusterConfig.getTrustStoreLocation()), clusterConfig.getTrustStorePassword().toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, clusterConfig.getKeyStorePassword().toCharArray());

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
