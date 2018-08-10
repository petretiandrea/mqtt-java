package it.petretiandrea.core;

import it.petretiandrea.core.Message;
import it.petretiandrea.core.packet.Connect;

public class ConnectionSettings {

    private String mHostname;
    private int mPort;
    private String mClientId;
    private String mUsername;
    private String mPassword;
    private boolean mCleanSession;
    private Message willMessage;
    private int mKeepAliveSeconds;

    private boolean mUseTLS;

    public static ConnectionSettings from(Connect connect, boolean useTLS) {
        return new ConnectionSettings(
                "",
                0,
                connect.getClientID(),
                connect.getUsername(),
                connect.getPassword(),
                connect.isCleanSession(),
                connect.getWillMessage(),
                connect.getKeepAliveSeconds(),
                useTLS
        );
    }

    public ConnectionSettings(String hostname, int port, String clientId, String username, String password, boolean cleanSession, Message willMessage, int keepAliveSeconds, boolean useTLS) {
        mHostname = hostname;
        mPort = port;
        mClientId = clientId;
        mUsername = username;
        mPassword = password;
        mCleanSession = cleanSession;
        this.willMessage = willMessage;
        mKeepAliveSeconds = keepAliveSeconds;
        mUseTLS = useTLS;
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

    public boolean isUseTLS() {
        return mUseTLS;
    }

    @Override
    public String toString() {
        return "ConnectionSettings{" +
                "mHostname='" + mHostname + '\'' +
                ", mPort=" + mPort +
                ", mClientId='" + mClientId + '\'' +
                ", mUsername='" + mUsername + '\'' +
                ", mPassword='" + mPassword + '\'' +
                ", mCleanSession=" + mCleanSession +
                ", willMessage=" + willMessage +
                ", mKeepAliveSeconds=" + mKeepAliveSeconds +
                '}';
    }
}
