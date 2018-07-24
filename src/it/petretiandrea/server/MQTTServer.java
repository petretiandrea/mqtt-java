package it.petretiandrea.server;

import it.petretiandrea.common.Transport;
import it.petretiandrea.common.TransportTCP;
import it.petretiandrea.core.ConnectionStatus;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.ConnAck;
import it.petretiandrea.core.packet.Connect;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.server.security.AccountManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MQTTServer {

    public static final int TIMEOUT_CONNECT = 10000;

    private static final Executor CLIENTS_POOL = Executors.newCachedThreadPool();

    /**
     * Server socket for accept incoming TCP connection.
     */
    private ServerSocket mServerSocket;

    /**
     * Map of connected Clients.
     */
    private ConcurrentMap<String, ClientMonitor> mClientsConnected;

    /**
     * Session manager, for manage the sessions active, and the persistent sessions.
     */
    private SessionManager mSessionManager;

    /**
     * Callback for receive communication from ClientMonitor
     */
    private ClientMonitorServerCallback mClientMonitorServerCallback;

    public MQTTServer() {
        mSessionManager = new SessionManager();
        mClientsConnected = new ConcurrentHashMap<>();
        mServerSocket = null;
        mClientMonitorServerCallback = new ClientMonitorServerCallback() {
            @Override
            public void onClientDisconnect(ClientMonitor clientMonitor) {
                System.out.println("Client Disconnected!");
                if(mClientsConnected.containsKey(clientMonitor.getSession().getClientID())) {
                    /* Remove the client from the list */
                    mClientsConnected.remove(clientMonitor.getSession().getClientID());
                    /* Clean session if the flag clean session is true */
                    mSessionManager.cleanSession(clientMonitor.getSession().getClientID());
                }
            }
        };
    }

    /**
     * Start server on default MQTT port 1883.
     * @throws IOException If there is a socket server binding error.
     */
    public void listen() throws IOException {
        listen(1883);
    }

    /**
     * Start server on specific port.
     * @throws IOException If there is a socket server binding error.
     */
    public void listen(int port) throws IOException {
        mServerSocket = new ServerSocket(port);
        new Thread(this::connectionThread).start();
    }

    /**
     * Behaviour of thread, that manage the connection pahse, and start the ClientMonitor thread on Pool.
     */
    private void connectionThread() {
        try {
            while (!Thread.interrupted() && mServerSocket.isBound()) {
                Transport transport = new TransportTCP(mServerSocket.accept());
                // try read connect packet
                try {
                    // 1. Read with specific timeout
                    // 2. During read the message is validated.
                    MQTTPacket packet = transport.readPacket(TIMEOUT_CONNECT);
                    if(packet.getCommand() == MQTTPacket.Type.CONNECT) {
                        Connect connect = (Connect) packet;

                        // 3. Perform authentication.
                        if(tryValidateUsernamePassword(connect.getUsername(), connect.getPassword())) {

                            // 4. Disconnect an existing Client with same ClientID.
                            disconnectClient(connect.getClientID());

                            // 5. Process the clean session flag, and search or create a new session.
                            Session session = null;
                            boolean isPresent = false;
                            if(connect.isCleanSession()) {
                                // remove any old permanent session
                                mSessionManager.cleanSession(connect.getClientID());
                                session = mSessionManager.createNewSession(connect.getClientID(), true);
                            } else {
                                session = mSessionManager.searchSession(connect.getClientID());
                                isPresent = (session != null);
                                if(!isPresent)
                                    session = mSessionManager.createNewSession(connect.getClientID(), false);
                            }
                            // 6. Send ack with zero code (all is ok), to Client.
                            transport.writePacket(new ConnAck(isPresent, ConnectionStatus.ACCEPT));

                            // 7. Start the message and keep alive monitoring.
                            ClientMonitor clientMonitor = new ClientMonitor(transport, session, connect, mClientMonitorServerCallback);
                            mClientsConnected.put(clientMonitor.getSession().getClientID(), clientMonitor);
                            clientMonitor.start();

                        } else {
                            // send bad_login
                            transport.writePacket(new ConnAck(false, ConnectionStatus.REFUSED_BAD_LOGIN));
                        }
                    }
                } catch (IOException | MQTTParseException ex) {
                    ex.printStackTrace();
                    if(ex instanceof MQTTParseException) {
                        switch (((MQTTParseException) ex).getReason()) {
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
            if(AccountManager.getInstance().existUsername(username)) {
                // username and password check
                return password == null || AccountManager.getInstance().grantAccess(username, password);
            }
            return false;
        }
        return true;
    }

    /**
     * Method for disconnect a Client, auto clean the session if needed, and remove the client from list of connected.
     * @param clientID The ID of Client
     */
    private void disconnectClient(String clientID) {
        if(mClientsConnected.containsKey(clientID)) {
            // another client with same client id
            try {
                // wait for disconnection.
                mClientsConnected.get(clientID).disconnect().wait(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}