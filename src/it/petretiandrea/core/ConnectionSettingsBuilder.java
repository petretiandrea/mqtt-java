package it.petretiandrea.core;

public class ConnectionSettingsBuilder {

    private boolean mUseTLS;

    private String mHostname;
    private int mPort;
    private String mClientId;
    private String mUsername;
    private String mPassword;
    private boolean mCleanSession;
    private Message mWillMessage;
    private int mKeepAliveSeconds;

    public ConnectionSettingsBuilder setHostname(String hostname) {
        mHostname = hostname;
        return this;
    }

    public ConnectionSettingsBuilder setPort(int port) {
        mPort = port;
        return this;
    }

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

    public ConnectionSettingsBuilder setUseTLS(boolean useTLS) {
        mUseTLS = useTLS;
        return this;
    }

    public ConnectionSettings build() {
        return new ConnectionSettings(mHostname, mPort, mClientId, mUsername, mPassword, mCleanSession, mWillMessage, mKeepAliveSeconds, mUseTLS);
    }
}