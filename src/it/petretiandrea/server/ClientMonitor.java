package it.petretiandrea.server;

import it.petretiandrea.common.Transport;
import it.petretiandrea.core.Message;
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

    private final Thread mThread;

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
                } catch (SocketTimeoutException ignored) {
                }
                checkClientAlive();
            }
        } catch (IOException | MQTTProtocolException | MQTTParseException ex) {
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
            case PUBACK: {
                break;
            }
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
                if(pub.getQos() == Qos.QOS_0 || pub.getQos() == Qos.QOS_1) {
                    if (pub.getQos() == Qos.QOS_1) mTransport.writePacket(new PubAck(pub.getMessage().getMessageID()));
                    mServerComm.onPublishMessageReceived(pub.getMessage());
                }
                break;
            }
            case SUBSCRIBE: {
                Subscribe sub = (Subscribe) packet;
                mTransport.writePacket(new SubAck(sub.getMessageID(), sub.getQosSub()));
                getSession().getSubscriptions().add(sub);
                System.out.println(getSession().getClientID() + "\tsubscribed to: " + sub.getTopic());
            }
        }
    }

    public void publish(Message message) {
        Publish publish = new Publish(message);
        try {
            if (message.getQos().ordinal() > Qos.QOS_0.ordinal()) {
                // need to add to queue for wait the puback, pubrel, ecc. for QOS 1 or 2.
                getSession().getSended().add(publish);
            }
            mTransport.writePacket(publish);
        } catch (IOException e) {
            e.printStackTrace();
            getSession().getSended().remove(publish);
        }
    }
}
