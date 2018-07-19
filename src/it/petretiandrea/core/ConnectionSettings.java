package it.petretiandrea.core;

import it.petretiandrea.core.Message;

public class ConnectionSettings {

    private String mClientId;
    private String mUsername;
    private String mPassword;
    private boolean mCleanSession;
    private Message willMessage;
    private int mKeepAliveSeconds;

    ConnectionSettings(String clientId, String username, String password, boolean cleanSession, Message willMessage, int keepAliveSeconds) {
        mClientId = clientId;
        mUsername = username;
        mPassword = password;
        mCleanSession = cleanSession;
        this.willMessage = willMessage;
        mKeepAliveSeconds = keepAliveSeconds;
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
