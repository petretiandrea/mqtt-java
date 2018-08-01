package it.petretiandrea.server;

import it.petretiandrea.client.MQTTClient;
import it.petretiandrea.common.Client;
import it.petretiandrea.common.MQTTClientCallback;
import it.petretiandrea.common.SessionManager;
import it.petretiandrea.common.SubscribeManager;
import it.petretiandrea.common.network.Transport;
import it.petretiandrea.common.network.TransportTCP;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.Connect;
import it.petretiandrea.core.packet.Subscribe;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.server.security.AccountManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Broker implements MQTTClientCallback {

    private static final int TIMEOUT_CONNECT = (int) (0.5 * 1000);
    /**
     * Clients connected
     */
    private final Map<String, MQTTClient> mClients;

    /**
     * Manager for subscribe and unsubscribe the clients to topic.
     */
    private SubscribeManager mSubscribeManager;

    /**
     * Session manager
     */
    private SessionManager mSessionManager;

    /**
     * Retain Messages
     */
    private List<Message> mRetainMessages;

    /**
     * Manager for auth clients.
     */
    private AccountManager mAccountManager;


    private ServerSocket mServerSocket;
    private volatile boolean mRunning;
    private final Object mLock = new Object();

    public Broker() {
        mAccountManager = AccountManager.getInstance();
        mSessionManager = new SessionManager();
        mSubscribeManager = new SubscribeManager();
        mClients = new HashMap<>();
        mRetainMessages = new ArrayList<>();
        mServerSocket = null;
    }

    public void listen(int port) throws IOException {
        if(!mRunning) {
            synchronized (mLock)
            {
                mServerSocket = new ServerSocket(port);
                new Thread(this::connectionLoop).start();
            }
        }
    }

    public boolean isRunning() {
        return mRunning;
    }

    /**
     * Connection Loop for accept the clients Connections
     */
    private void connectionLoop() {
        try {
            while (!mServerSocket.isClosed()) {
                Transport transport = new TransportTCP(mServerSocket.accept());
                try {
                    MQTTPacket packet = transport.readPacket(TIMEOUT_CONNECT);
                    if(packet.getCommand() == MQTTPacket.Type.CONNECT) {
                        Connect connect = (Connect) packet;
                        // 3. Perform authentication.
                        if(tryValidateUsernamePassword(connect.getUsername(), connect.getPassword())) {
                            disconnectClient(connect.getClientID());

                            // restore subscription by broker session
                            // restore pending publish packet, passing list of packet to new Client()

                        }
                    }
                } catch (MQTTParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if need to authentication and try to authenticate the client from it Connect request
     * @return True if authorized or there is no authorization request, False otherwise.
     */
    private boolean tryValidateUsernamePassword(String username, String password) {
        // only username check
        if(username != null) {
            if(mAccountManager.existUsername(username)) {
                // username and password check
                return password == null || mAccountManager.grantAccess(username, password);
            }
            return false;
        }
        return true;
    }

    /**
     * Method for closeConnection a MQTTClient, auto clean the session if needed, and remove the client from list of connected.
     * @param clientID The ID of MQTTClient
     */
    private void disconnectClient(String clientID) {
        synchronized (mClients) {
            if(mClients.containsKey(clientID)) {
                // another client with same client id
                // wait for disconnection.
                mClients.get(clientID).disconnect().join();
            }
        }
    }


    /* Single client Callbacks */
    @Override
    public void onConnectionLost(Throwable throwable) {

    }

    @Override
    public void onMessageArrived(Message message) {

    }

    @Override
    public void onDeliveryComplete(int messageId) {

    }

    @Override
    public void onSubscribe(Subscribe subscribe) {

    }
}
