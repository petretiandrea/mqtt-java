package it.petretiandrea.core;

import it.petretiandrea.core.Message;

public class ConnectionSettings {

    private String mHostname;
    private int mPort;
    private String mClientId;
    private String mUsername;
    private String mPassword;
    private boolean mCleanSession;
    private Message willMessage;
    private int mKeepAliveSeconds;

    public ConnectionSettings(String hostname, int port, String clientId, String username, String password, boolean cleanSession, Message willMessage, int keepAliveSeconds) {
        mHostname = hostname;
        mPort = port;
        mClientId = clientId;
        mUsername = username;
        mPassword = password;
        mCleanSession = cleanSession;
        this.willMessage = willMessage;
        mKeepAliveSeconds = keepAliveSeconds;
    }

    public String getHostname() {
        return mHostname;
    }

    public int getPort() {
        return mPort;
    }

    public String getClientId() {
        return mClientId;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getPassword() {
        return mPassword;
    }

    public boolean isCleanSession() {
        return mCleanSession;
    }

    public Message getWillMessage() {
        return willMessage;
    }

    public int getKeepAliveSeconds() {
        return mKeepAliveSeconds;
    }
}
