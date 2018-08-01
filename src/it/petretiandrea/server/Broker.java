package it.petretiandrea.server;

import it.petretiandrea.client.MQTTClient;
import it.petretiandrea.common.Client;
import it.petretiandrea.common.MQTTClientCallback;
import it.petretiandrea.common.SessionManager;
import it.petretiandrea.common.SubscribeManager;
import it.petretiandrea.common.network.Transport;
import it.petretiandrea.common.network.TransportTCP;
import it.petretiandrea.common.session.BrokerSession;
import it.petretiandrea.common.session.ClientSession;
import it.petretiandrea.common.session.Session;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.ConnectionStatus;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.server.security.AccountManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

                            // recover an old session
                            BrokerSession session = restoreSession(connect);
                            // restore the subscribed topics
                            session.getSubscriptions().forEach(subscribe -> mSubscribeManager.subscribe(session.getClientID(), subscribe));

                            ClientBroker clientBroker = new ClientBroker(ConnectionSettings.from(connect), session,
                                    transport, new ArrayList<>(session.getPendingPublish()));

                            addConnectedClient(clientBroker);

                            // send connack
                            transport.writePacket(new ConnAck(false, ConnectionStatus.ACCEPT));

                            clientBroker.setClientCallback(this);
                            clientBroker.startLoop();
                        } else {
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

    /* Single client Callbacks */

    @Override
    public void onMessageArrived(Client client, Message message) {
        System.out.println("Broker.onMessageArrived");

        Qos receivedQos = message.getQos();

        // 1. publish message for all active connections
        mClients.values().stream()
                .filter(Client::isConnected)
                .forEach(clientBroker -> mSubscribeManager.getSubscriptions(clientBroker.getClientSession().getClientID()).stream()
                        .filter(subscribe -> subscribe.getTopic().equals(message.getTopic()))
                        .forEach(subscribe -> {
                            message.setQos(Qos.min(subscribe.getQosSub(), receivedQos));
                            clientBroker.publish(message);
                        }));


        // 2. recover session from session manager, and put here the publish message
        mSessionManager.getSessions().forEach(brokerSession ->
                brokerSession.getSubscriptions().stream()
                    .filter(subscribe -> subscribe.getTopic().equals(message.getTopic()))
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

    }

    @Override
    public void onConnectionLost(Client client, Throwable ex) {
        System.out.println("Broker.onConnectionLost");
        // same action of onDisconnect
        onDisconnect(client);
    }

    @Override
    public void onDisconnect(Client client) {
        System.out.println("Broker.onDisconnect " + client.getClientSession().getClientID());
        if(mClients.containsKey(client.getClientSession().getClientID())) {
            /* Remove the client from the list */
            mClients.remove(client.getClientSession().getClientID());

            /* Clean session if the flag clean session is true */
            if(client.getClientSession().isCleanSession())
                mSessionManager.cleanSession(client.getClientSession().getClientID());
            else /* Save Sessions */
                mSessionManager.addSession(client.getClientSession(), mSubscribeManager.getSubscriptions(client.getClientSession().getClientID()));

        }
    }

    @Override
    public void onSubscribeComplete(Client client, Subscribe subscribe) {
        System.out.println("Broker.onSubscribeComplete");
        mSubscribeManager.subscribe(client.getClientSession().getClientID(), subscribe);
        mRetainMessages.stream()
                .filter(message -> message.getTopic().equals(subscribe.getTopic()))
                .forEach(client::publish);
    }

    @Override
    public void onUnsubscribeComplete(Client client, Unsubscribe unsubscribe) {
        System.out.println("Broker.onUnsubscribeComplete");
        mSubscribeManager.unsubscribe(client.getClientSession().getClientID(), unsubscribe);
    }

}
