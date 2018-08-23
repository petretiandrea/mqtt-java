package it.petretiandrea.server.security;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class TLSProvider implements SSLContextProvider {

    private static final String JKS = "JKS";

    private URL mServerKeyPath;
    private String mServerKeyPassword;
    private URL mCertPath;
    private String mCertPassword;

    public TLSProvider(URL serverKeyPath, URL certPath, String passwordKeys) {
        this(serverKeyPath, passwordKeys, certPath, passwordKeys);
    }

    public TLSProvider(URL serverKeyPath, String serverKeyPassword, URL certPath, String certPassword) {
        mServerKeyPath = serverKeyPath;
        mServerKeyPassword = serverKeyPassword;
        mCertPath = certPath;
        mCertPassword = certPassword;
    }

    @Override
    public TrustManager[] getTrustManagers() throws GeneralSecurityException, IOException {
        String algorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);

        KeyStore ks = KeyStore.getInstance(JKS);
        InputStream ksIs = mCertPath.openStream();
        try {
            ks.load(ksIs, mCertPassword.toCharArray());
        }
        finally {
            ksIs.close();
        }

        tmf.init(ks);

        return tmf.getTrustManagers();
    }

    @Override
    public KeyManager[] getKeyManagers() throws GeneralSecurityException, IOException {
        String serverKeyStorePasswd = mServerKeyPassword;
        String algorithm = KeyManagerFactory.getDefaultAlgorithm();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);

        KeyStore ks = KeyStore.getInstance(JKS);
        InputStream ksIs = mServerKeyPath.openStream();
        try {
            ks.load(ksIs, serverKeyStorePasswd.toCharArray());
        }
        finally {
            ksIs.close();
        }
        kmf.init(ks, serverKeyStorePasswd.toCharArray());

        return kmf.getKeyManagers();
    }

    @Override
    public String getProtocol() {
        return "TLSv1.2";
    }
}
