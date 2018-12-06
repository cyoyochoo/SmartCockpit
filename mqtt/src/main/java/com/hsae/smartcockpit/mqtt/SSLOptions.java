package com.hsae.smartcockpit.mqtt;

import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSLOptions {
    private InputStream caInputStream;
    private InputStream clientInputStream;
    private String clientPassword;

    private SSLOptions(Builder builder) {
        caInputStream = builder.caInputStream;
        clientInputStream = builder.clientInputStream;
        clientPassword = builder.clientPassword;
    }

    public Pair<SSLSocketFactory, X509TrustManager> get() {
        SSLSocketFactory sslSocketFactory = null;
        X509TrustManager trustManager = null;
        try {
            if (caInputStream != null) {
                trustManager = trustManagerForCertificates();
            }

            KeyManager[] keyManagers = null;
            if (clientInputStream != null && clientPassword != null) {
                keyManagers = keyManagerForKeystore();
            }

            if (trustManager != null || keyManagers != null) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagers, new TrustManager[]{trustManager}, null);
                sslSocketFactory = sslContext.getSocketFactory();
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        return new Pair<>(sslSocketFactory, trustManager);
    }

    /**
     * https://github.com/square/okhttp/wiki/HTTPS
     * @return
     */
    private KeyManager[] keyManagerForKeystore() {
        KeyManager[] keyManagers = null;
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(clientInputStream, clientPassword.toCharArray());
            clientInputStream.close();
            clientInputStream = null;
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
            keyManagerFactory.init(keyStore, clientPassword.toCharArray());
            clientPassword = null;
            keyManagers = keyManagerFactory.getKeyManagers();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return keyManagers;
    }

    /**
     * Returns a trust manager that trusts {@code certificates} and none other. HTTPS services whose
     * certificates have not been signed by these certificates will fail with a {@code
     * SSLHandshakeException}.
     *
     * <p>This can be used to replace the host platform's built-in trusted certificates with a custom
     * set. This is useful in development where certificate authority-trusted certificates aren't
     * available. Or in production, to avoid reliance on third-party certificate authorities.
     *
     * <p>See also { CertificatePinner}, which can limit trusted certificates while still using
     * the host platform's built-in trust store.
     *
     * <h3>Warning: Customizing Trusted Certificates is Dangerous!</h3>
     *
     * <p>Relying on your own trusted certificates limits your server team's ability to update their
     * TLS certificates. By installing a specific set of trusted certificates, you take on additional
     * operational complexity and limit your ability to migrate between certificate authorities. Do
     * not use custom trusted certificates in production without the blessing of your server's TLS
     * administrator.
     */
    private X509TrustManager trustManagerForCertificates()
            throws GeneralSecurityException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(caInputStream);
        try {
            caInputStream.close();
            caInputStream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty set of trusted certificates");
        }

        // Put the certificates a key store.
        char[] password = "123456".toCharArray(); // Any password will work.
        KeyStore keyStore = newEmptyKeyStore(password);
        int index = 0;
        for (Certificate certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificate);
        }

        // Use it to build an X509 trust manager.
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    private KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = null; // By convention, 'null' creates an empty key store.
            keyStore.load(in, password);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static final class Builder {
        private InputStream caInputStream;
        private InputStream clientInputStream;
        private String clientPassword;

        public Builder withCaInputStream(InputStream caInputStream) {
            this.caInputStream = caInputStream;
            return this;
        }

        public Builder withClientInputStream(InputStream clientInputStream) {
            this.clientInputStream = clientInputStream;
            return this;
        }

        public Builder withClientPassword(String clientPassword) {
            this.clientPassword = clientPassword;
            return this;
        }

        public SSLOptions build() {
            return new SSLOptions(this);
        }
    }
}
