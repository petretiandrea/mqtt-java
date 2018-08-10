package it.petretiandrea.server;

import it.petretiandrea.common.network.TransportTLS;
import it.petretiandrea.utils.CustomLogger;
import it.petretiandrea.common.*;
import it.petretiandrea.common.network.Transport;
import it.petretiandrea.common.network.TransportTCP;
import it.petretiandrea.common.session.BrokerSession;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.ConnectionStatus;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.server.security.AccountManager;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Broker implements MQTTClientCallback {

    private static final Executor EXECUTOR = Executors.newCachedThreadPool();

    private static final int TIMEOUT_CONNECT = (int) (0.5 * 1000);
    /**
     * Clients connected
     */
    private final Map<String, ClientBroker> mClients;

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

    private final Object mLock = new Object();

    private ServerSocket mServerSocket;
    private volatile boolean mRunning;

    private Thread mBrokerThread;

    private boolean mUseTLS;

    public Broker() {
        this(new AccountManager());
    }

    public Broker(AccountManager accountManager) {
        mAccountManager = accountManager;
        mSessionManager = new SessionManager();
        mSubscribeManager = new SubscribeManager();
        mClients = new HashMap<>();
        mRetainMessages = new ArrayList<>();
        mServerSocket = null;
    }

    public void listenTLS(int port) throws IOException {
        if(!mRunning) {
            synchronized (mLock)
            {
                try {
                    SSLContext context = SSLContext.getInstance("TLSv1.2");
                    context.init(null, new TrustManager[] { new it.petretiandrea.common.network.TrustManager() }, null);
                    mUseTLS = true;
                    mServerSocket = context.getServerSocketFactory().createServerSocket(port);
                    mBrokerThread = new Thread(this::connectionLoop);
                    mBrokerThread.start();
                    CustomLogger.LOGGER.info("Server running on: " + port);
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    CustomLogger.LOGGER.severe("Broker: " + e.getMessage());
                }
            }
        }
    }

    public void listen(int port) throws IOException {
        if(!mRunning) {
            synchronized (mLock)
            {
                mUseTLS = false;
                mServerSocket = new ServerSocket(port);
                mBrokerThread = new Thread(this::connectionLoop);
                mBrokerThread.start();
                CustomLogger.LOGGER.info("Server running on: " + port);
            }
        }
    }

    /**
     * Wait for end of Broker Thread Life.
     * @throws InterruptedException
     */
    public void waitEnd() throws InterruptedException {
        mBrokerThread.join();
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
                Transport transport = (mUseTLS) ? new TransportTLS(mServerSocket.accept()) : new TransportTCP(mServerSocket.accept());
                try {
                    MQTTPacket packet = transport.readPacket(TIMEOUT_CONNECT);
                    if(packet.getCommand() == MQTTPacket.Type.CONNECT) {
                        Connect connect = (Connect) packet;
                        CustomLogger.LOGGER.info("Broker: Client connected request: " + connect);
                        // 3. Perform authentication.
                        if(tryValidateUsernamePassword(connect.getUsername(), connect.getPassword())) {
                            disconnectClient(connect.getClientID());

                            // recover an old session
                            BrokerSession session = restoreSession(connect);
                            // restore the subscribed topics
                            session.getSubscriptions().forEach(subscribe -> mSubscribeManager.subscribe(session.getClientID(), subscribe));

                            ClientBroker clientBroker = new ClientBroker(ConnectionSettings.from(connect, mUseTLS), session,
                                    transport, new ArrayList<>(session.getPendingPublish()));

                            addConnectedClient(clientBroker);

                            // send connack
                            transport.writePacket(new ConnAck(false, ConnectionStatus.ACCEPT));

                            clientBroker.setClientCallback(this);
                            clientBroker.startLoop();
                            CustomLogger.LOGGER.info("Broker: Client connected: " + connect.getClientID());
                        } else {
                            CustomLogger.LOGGER.info("Broker: Client " + connect.getClientID() + " BAD LOGIN!");
                            transport.writePacket(new ConnAck(false, ConnectionStatus.REFUSED_BAD_LOGIN));
                        }
                    }
                } catch (IOException | MQTTParseException e) {
                    e.printStackTrace();
                    if(e instanceof MQTTParseException) {
                        switch (((MQTTParseException) e).getReason()) {
                            case INVALID_MQTT_NAME_LEVEL: // versione del protocollo non valida
                                transport.writePacket(new ConnAck(false, ConnectionStatus.REFUSED_UNACCEPTABLE_PROTOCOL_VERSION));
                                break;
                            case INVALID_CLIENT_ID: // client id non valid, es length > 23.
                                transport.writePacket(new ConnAck(false, ConnectionStatus.REFUSED_IDENTIFIER_REJECTED));
                                break;
                        }
                    }
                    // close connection
                    transport.close();
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

    private void addConnectedClient(ClientBroker clientBroker) {
        synchronized (mClients) {
            mClients.put(clientBroker.getClientSession().getClientID(), clientBroker);
        }
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

    /**
     * Restore and remove a Session.
     * @param connect The connect packet
     * @return Session restored or new Session
     */
    private BrokerSession restoreSession(Connect connect) {
        BrokerSession restoredSession;
        if(!connect.isCleanSession()) {
            restoredSession = mSessionManager.getSession(connect.getClientID());
            if(restoredSession == null)
                restoredSession = new BrokerSession(connect.getClientID(), false);
            else
                mSessionManager.removeSession(restoredSession.getClientID());
        } else {
            restoredSession = new BrokerSession(connect.getClientID(), true);
        }
        return restoredSession;
    }

    private void closeClientConnection(Client client) {
        if(mClients.containsKey(client.getClientSession().getClientID())) {
            /* Remove the client from the list */
            mClients.remove(client.getClientSession().getClientID());

            /* Clean session if the flag clean session is true */
            if(client.getClientSession().isCleanSession())
                mSessionManager.cleanSession(client.getClientSession().getClientID());
            else /* Save Sessions */
                mSessionManager.addSession(client.getClientSession(), mSubscribeManager.getSubscriptions(client.getClientSession().getClientID()));

            // remove all subscriptions
            mSubscribeManager.unsubscribeAll(client.getClientSession().getClientID());

            // send if present the will message to other clients
            // uso on messageArrived che gestisce automanticamente gi il rendirizzamento nella sessione e il retain
            if(client.getWillMessage() != null)
                onMessageArrived(client, client.getWillMessage());
        }
    }

    /* Single client Callbacks */

    @Override
    public void onMessageArrived(Client client, Message message) {
        CustomLogger.LOGGER.info("Broker: Message Received from " + client.getClientSession().getClientID()
                + " " + message);

        Qos receivedQos = message.getQos();

        // 1. publish message for all active connections
        mClients.values().stream()
                .filter(Client::isConnected)
                .forEach(clientBroker -> mSubscribeManager.getSubscriptions(clientBroker.getClientSession().getClientID()).stream()
                        .filter(subscribe -> TopicMatcher.matchTopic(subscribe.getTopic(), message.getTopic()))
                        .forEach(subscribe -> {
                            message.setQos(Qos.min(subscribe.getQosSub(), receivedQos));
                            clientBroker.publish(message);
                        }));


        // 2. recover session from session manager, and put here the publish message
        mSessionManager.getSessions().forEach(brokerSession ->
                brokerSession.getSubscriptions().stream()
                    .filter(subscribe -> TopicMatcher.matchTopic(subscribe.getTopic(), message.getTopic()))
                    .forEach(subscribe -> {
                        message.setQos(Qos.min(subscribe.getQosSub(), receivedQos));
                        brokerSession.getPendingPublish().add(new Publish(message));
                }));

        // 3. save retain message
        if(message.isRetain()) {
            synchronized (mLock) {
                if (message.getMessage().trim().isEmpty()) // is empty remove it from retained message
                    mRetainMessages.removeIf(message1 -> message1.getTopic().equals(message.getTopic()));
                else
                    mRetainMessages.add(message); // add it from retained message.
            }
        }
    }

    @Override
    public void onDeliveryComplete(Client client, int messageId) {
        CustomLogger.LOGGER.info("Broker: Message delivered with message id " + messageId);
    }

    @Override
    public void onConnectionLost(Client client, Throwable ex) {
        CustomLogger.LOGGER.info("Broker: Connection Lost with " + client.getClientSession().getClientID());
        // same action of onDisconnect
        closeClientConnection(client);
    }

    @Override
    public void onDisconnect(Client client) {
        CustomLogger.LOGGER.info("Broker: Disconnect " + client.getClientSession().getClientID());
        closeClientConnection(client);
    }

    @Override
    public void onSubscribeComplete(Client client, Subscribe subscribe) {
        mSubscribeManager.subscribe(client.getClientSession().getClientID(), subscribe);
        mRetainMessages.stream()
                .filter(message -> TopicMatcher.matchTopic(subscribe.getTopic(), message.getTopic()))
                .forEach(client::publish);
    }

    @Override
    public void onUnsubscribeComplete(Client client, Unsubscribe unsubscribe) {
        CustomLogger.LOGGER.info("Broker: Unsubscribe from " + client.getClientSession().getClientID() + " " + unsubscribe);
        mSubscribeManager.unsubscribe(client.getClientSession().getClientID(), unsubscribe);
    }

}
