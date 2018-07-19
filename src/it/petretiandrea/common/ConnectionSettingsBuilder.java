package it.petretiandrea.common;

public class ConnectionSettingsBuilder {
    private String mClientId;
    private String mUsername;
    private String mPassword;
    private boolean mCleanSession;
    private Message mWillMessage;
    private int mKeepAliveSeconds;

    public ConnectionSettingsBuilder setClientId(String clientId) {
        mClientId = clientId;
        return this;
    }

    public ConnectionSettingsBuilder setUsername(String username) {
        mUsername = username;
        return this;
    }

    public ConnectionSettingsBuilder setPassword(String password) {
        mPassword = password;
        return this;
    }

    public ConnectionSettingsBuilder setCleanSession(boolean cleanSession) {
        mCleanSession = cleanSession;
        return this;
    }

    public ConnectionSettingsBuilder setWillMessage(Message willMessage) {
        mWillMessage = willMessage;
        return this;
    }

    public ConnectionSettingsBuilder setKeepAliveSeconds(int keepAliveSeconds) {
        mKeepAliveSeconds = keepAliveSeconds;
        return this;
    }

    public ConnectionSettings build() {
        return new ConnectionSettings(mClientId, mUsername, mPassword, mCleanSession, mWillMessage, mKeepAliveSeconds);
    }
}