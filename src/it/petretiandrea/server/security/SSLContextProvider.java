package it.petretiandrea.server.security;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;

public interface SSLContextProvider {

    TrustManager[] getTrustManagers() throws GeneralSecurityException, IOException;

    KeyManager[] getKeyManagers() throws GeneralSecurityException, IOException;

    String getProtocol();

}
