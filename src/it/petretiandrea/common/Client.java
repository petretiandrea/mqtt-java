package it.petretiandrea.common;

import it.petretiandrea.common.network.Transport;
import it.petretiandrea.common.session.BrokerSession;
import it.petretiandrea.common.session.ClientSession;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class Client implements PacketDispatcher.IPacketReceiver {

    private static final int SOCKET_IO_TIMEOUT = (int) (0.5 * 1000);
    /**
     * Transport Layer for write and read.
     */
    private Transport mTransport;

    /**
     * The session of client.
     */
    private final ClientSession mClientSession;

    /**
     * Queue for message pending to write.
     */
    private final QueueMQTT<MQTTPacket> mPendingQueue;

    /**
     * Connection Setting for connect to broker.
     */
    private ConnectionSettings mConnectionSettings;

    /**
     * Status of connection
     */
    private volatile boolean mConnected;

    /**
     * Thread Looper for catch incoming packet, and for write pending packet.
     */
    private Thread mLooper;

    /**
     * Dispatcher for all MQTT packet, call specific method with packet casted
     */
    private PacketDispatcher mPacketDispatcher;

    private MQTTClientCallback mClientCallback;

    public Client(ConnectionSettings connectionSettings, ClientSession clientSession) {
        this(connectionSettings, clientSession, Collections.emptyList());
    }

    /**
     * Create a new Client with specific connection settings, client session and a list of old pending packet. Is
     * used by the broker for restore the pending packet.
     * @param connectionSettings Settings of connection.
     * @param clientSession Client session
     * @param pendingPacket Packet not send, because Client is Offline. Used by broker for restore pending packet.
     */
    public Client(ConnectionSettings connectionSettings, ClientSession clientSession, List<MQTTPacket> pendingPacket) {
        mConnectionSettings = connectionSettings;
        mClientSession = clientSession;
        mConnected = false;
        mPendingQueue = new QueueMQTT<>();
        mPendingQueue.addAll(pendingPacket);
        mPacketDispatcher = new PacketDispatcher(this);
    }

    public Client(ConnectionSettings connectionSettings, BrokerSession brokerSession, Transport transport) {
        mTransport = transport;

        mClientSession = new ClientSession(brokerSession.getClientID(), brokerSession.isCleanSession());
        mClientSession.getSendedNotAck().addAll(brokerSession.getSendedNotAck());
        mClientSession.getReceivedNotAck().addAll(brokerSession.getReceivedNotAck());

        mConnectionSettings = connectionSettings;
        mPendingQueue = new QueueMQTT<>();
        mPendingQueue.addAll(brokerSession.getPendingPublish());

        mConnected = false;
        mPacketDispatcher = new PacketDispatcher(this);
    }

    /**
     * Connect Client to Broker.
     * @return True if connected or already connected, False otherwise.
     */
    public boolean connect() {
        return false;
    }

    /**
     * Return the connection status of this Client.
     * @return True if connected, False otherwise
     */
    public boolean isConnected() {
        return mConnected;
    }

    public ClientSession getClientSession() {
        synchronized (mClientSession) {
            return mClientSession;
        }
    }

    public MQTTClientCallback getClientCallback() {
        return mClientCallback;
    }

    public void setClientCallback(MQTTClientCallback clientCallback) {
        mClientCallback = clientCallback;
    }

    /**
     * Disconnect this client sending a disconnect packet.
     * @return A Future for disconnect task.
     */
    public CompletableFuture<Boolean> disconnect() {
        return CompletableFuture.supplyAsync(() -> {
            if(isConnected()) {
                try {
                    mTransport.writePacket(new Disconnect());
                    mTransport.close();
                    mConnected = false;
                    mLooper.interrupt();
                    mLooper.join();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mTransport = null;
                    mLooper = null;
                }
            }
            return isConnected();
        });
    }


    private void loop() {
        try {
            while (isConnected()) {
                try {
                    // 1. send packet from mPendingQueue
                    sendPendingQueue();
                    // 2. read packet
                    MQTTPacket incoming = mTransport.readPacket(SOCKET_IO_TIMEOUT);
                    if(incoming != null) {
                        // 3. packet received, dispatch it.
                        mPacketDispatcher.dispatch(incoming);
                    }
                } catch (SocketTimeoutException ignored) { } // ignored for because, is like to polling read from socket.
            }
        } catch (IOException | MQTTProtocolException | MQTTParseException ex) {
            ex.printStackTrace();
            if(mClientCallback != null) mClientCallback.onConnectionLost(this, ex);
        }
        if(mClientCallback != null) mClientCallback.onDisconnect(this);
    }

    /**
     * Send all packet inside the pending queue. Auto move the packet, if send, to
     * the queue of session for "packet send but not acknowledged" if the qos is > qos 0
     */
    private void sendPendingQueue() {
        mPendingQueue.forEach(mqttPacket -> {
            try {
                mTransport.writePacket(mqttPacket);
                // for packet send with qos > qos0, enqueue it because need to be acknowledged
                if(mqttPacket.getQos().ordinal() > Qos.QOS_0.ordinal())
                    getClientSession().getSendedNotAck().add(mqttPacket);

                // after send remove from pending queue
                synchronized (mPendingQueue) {
                    mPendingQueue.remove(mqttPacket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void publish(Message message) {
        synchronized (mPendingQueue) {
            mPendingQueue.add(new Publish(message));
        }
    }

    public void subscribe(String topic, Qos qos) {
        synchronized (mPendingQueue) {
            mPendingQueue.add(new Subscribe(topic, qos));
        }
    }

    @Override
    public void onConnectReceive(Connect connect) {

    }

    @Override
    public void onConnAckReceive(ConnAck connAck) {

    }

    @Override
    public void onPublishReceive(Publish publish) {
        if(publish.getQos() == Qos.QOS_0) {
            mClientCallback.onMessageArrived(publish.getMessage());
        } else if(publish.getQos() == Qos.QOS_1) {
            mClientCallback.onMessageArrived(publish.getMessage());
            mPendingQueue.add(new PubAck(publish.getMessage().getMessageID()));
        } else if(publish.getQos() == Qos.QOS_2) {
            getClientSession().getReceivedNotAck().add(publish);
            mPendingQueue.add(new PubRec(publish.getMessage().getMessageID()));
        }
    }

    @Override
    public void onPubAckReceive(PubAck pubAck) {
        boolean removed = getClientSession().getSendedNotAck()
                .removeIf(packet -> (packet instanceof Publish) && ((Publish)packet).getMessage().getMessageID() == pubAck.getMessageID());
        if(removed)
            mClientCallback.onDeliveryComplete(pubAck.getMessageID());
    }

    @Override
    public void onPubRecReceive(PubRec pubRec) {
        getClientSession().getSendedNotAck()
                .removeIf(packet -> (packet instanceof Publish) && ((Publish)packet).getMessage().getMessageID() == pubRec.getMessageID());
        // store pubrec
        getClientSession().getReceivedNotAck().add(pubRec);
        mPendingQueue.add(new PubRel(pubRec.getMessageID()));
    }

    @Override
    public void onPubRelReceive(PubRel pubRel) {
        getClientSession().getReceivedNotAck().stream()
                .filter(packet -> (packet instanceof  Publish) && ((Publish)packet).getMessage().getMessageID() == pubRel.getMessageID())
                .findFirst()
                .ifPresent(packet -> {
                    mPendingQueue.add(new PubComp(pubRel.getMessageID()));
                    mClientCallback.onMessageArrived(((Publish) packet).getMessage());
                });
    }

    @Override
    public void onPubCompReceive(PubComp pubComp) {
        boolean removed = getClientSession().getReceivedNotAck()
                .removeIf(packet -> (packet instanceof PubRec) && ((PubRec)packet).getMessageID() == pubComp.getMessageID());
        if(removed)
            mClientCallback.onDeliveryComplete(pubComp.getMessageID());
    }
}
