package it.petretiandrea.server;

import it.petretiandrea.common.Transport;
import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;

public class ClientMonitor {

    /**
     * Transport layer, for communicate with MQTT Client.
     */
    private Transport mTransport;
    /**
     * Connect packet sended by MQTT Client, contains all connection configuration.
     */
    private Connect mConnectSettings;
    /**
     * Current session for MQTT Client.
     */
    private Session mSession;
    /**
     * Time of last packet received from MQTT Client. Used for keepalive timeout.
     */
    private long mLastPacketReceived;


    private ClientMonitorServerCallback mServerComm;

    private Thread mThread;

    public ClientMonitor(Transport transport, Session session, Connect settings, ClientMonitorServerCallback serverComm) {
        mTransport = transport;
        mSession = session;
        mConnectSettings = settings;
        mThread = new Thread(this::run);
        mServerComm = serverComm;
    }

    public void start() {
        mThread.start();
    }

    public Session getSession() {
        return mSession;
    }

    public CompletableFuture<Void> disconnect() {
        try {
            mTransport.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mThread.interrupt();

        return CompletableFuture.runAsync(() -> {
            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void run() {
        try {
            // initialize the timeout for keepalive.
            mLastPacketReceived = System.currentTimeMillis();
            while (!Thread.interrupted()) {
                try {
                    // read packet
                    MQTTPacket packet = mTransport.readPacket();
                    if(packet != null) {
                        // reset the keep alive, if a packet income
                        resetTimeoutKeepAlive();
                        handlePacket(packet);
                        System.out.println(mConnectSettings.getClientID() + "\t" +packet);
                    }
                } catch (SocketTimeoutException | MQTTParseException ex) {
                    if(ex instanceof  MQTTParseException)
                        ex.printStackTrace();
                }
                checkClientAlive();
            }
        } catch (IOException | MQTTProtocolException ex) {
            System.err.println(mConnectSettings.getClientID() + " " + ex.getMessage());
            ex.printStackTrace();
        }

        try {
            mTransport.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mServerComm.onClientDisconnect(this);
    }

    /**
     * Check if the client is alive, using the time of last packet received.
     * @throws MQTTProtocolException Throw if the client timeout expire.
     */
    private void checkClientAlive() throws MQTTProtocolException {
        long now = System.currentTimeMillis();
        if(now - mLastPacketReceived >= mConnectSettings.getKeepAliveSeconds() * 1000) {
            // client is not alive.
            throw new MQTTProtocolException("Client timeout expired!");
        }
    }

    /**
     * Reset the timeout for keep alive
     */
    private void resetTimeoutKeepAlive() {
        mLastPacketReceived = System.currentTimeMillis();
    }

    private void handlePacket(MQTTPacket packet) throws IOException, MQTTProtocolException {
        switch (packet.getCommand()) {
            case DISCONNECT:
                disconnect();
                break;
            case CONNECT:
                // duplicate connect packet
                throw new MQTTProtocolException("Duplicate CONNECT Packet!");
            case PINGREQ:
                mTransport.writePacket(new PingResp());
                break;
            case PUBLISH: {
                Publish pub = (Publish) packet;
                if(pub.getQos() == Qos.QOS_1) {
                    // send to other client
                    // send a puback
                    mTransport.writePacket(new PubAck(pub.getMessage().getMessageID()));
                } else if(pub.getQos() == Qos.QOS_2) {

                }
                break;
            }
        }
    }

}
