package it.petretiandrea.common.network;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class TrustManager implements X509TrustManager
{
    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
        return null;
    }


    public void checkClientTrusted(X509Certificate[] certs, String authType)
    {
    }


    public void checkServerTrusted(X509Certificate[] certs, String authType)
    {
    }
}
