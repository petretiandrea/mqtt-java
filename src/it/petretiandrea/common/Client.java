package it.petretiandrea.common;

import it.petretiandrea.common.network.Transport;
import it.petretiandrea.common.network.TransportTCP;
import it.petretiandrea.common.session.BrokerSession;
import it.petretiandrea.common.session.ClientSession;
import it.petretiandrea.core.*;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
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

    private final Object mLock = new Object();

    private long mTimeLastMessageArrived;

    public Client(ConnectionSettings connectionSettings, ClientSession clientSession,
                  Transport transport, List<MQTTPacket> pendingQueue) {
        mTransport = transport;
        mClientSession = clientSession;
        mPendingQueue = new QueueMQTT<>();
        mPendingQueue.addAll(pendingQueue);
        mConnectionSettings = connectionSettings;
        mConnected = false;
        mPacketDispatcher = new PacketDispatcher(this);
        mTimeLastMessageArrived = System.currentTimeMillis();
    }

    public long getTimeLastMessageArrived() {
        return mTimeLastMessageArrived;
    }
/*public Client(ConnectionSettings connectionSettings, BrokerSession brokerSession, Transport transport) {
        mTransport = transport;

        mClientSession = new ClientSession(brokerSession.getClientID(), brokerSession.isCleanSession());
        mClientSession.getSendedNotAck().addAll(brokerSession.getSendedNotAck());
        mClientSession.getReceivedNotAck().addAll(brokerSession.getReceivedNotAck());

        mConnectionSettings = connectionSettings;
        mPendingQueue = new QueueMQTT<>();
        mPendingQueue.addAll(brokerSession.getPendingPublish());

        mConnected = false;
        mPacketDispatcher = new PacketDispatcher(this);
    }*/

    /**
     * Connect Client to Broker.
     * @return True if connected or already connected, False otherwise.
     */
    public boolean connect() throws MQTTProtocolException {
        synchronized (mLock) {
            if(!mConnected && !mTransport.isConnected()) {
                // need to be connected!
                try {
                    MQTTPacket incomePacket;
                    mTransport.connect(new InetSocketAddress(mConnectionSettings.getHostname(), mConnectionSettings.getPort()));
                    mTransport.writePacket(new Connect(MQTTVersion.MQTT_311, mConnectionSettings));
                    // TODO: Add timeout
                    if((incomePacket = mTransport.readPacket()) != null) {
                        if(incomePacket.getCommand().equals(MQTTPacket.Type.CONNACK)) {
                            if(((ConnAck)incomePacket).getConnectionStatus() == ConnectionStatus.ACCEPT) {
                                mLooper = new Thread(this::loop);
                                mConnected = true;
                                mLooper.start();
                                return true;
                            } else throw new MQTTProtocolException(((ConnAck)incomePacket).getConnectionStatus().toString());
                        }
                    }
                } catch (IOException | MQTTParseException e) {
                    e.printStackTrace();
                }
            } else return true; // already connected

            return false;
        }
    }

    /**
     * Return the connection status of this Client.
     * @return True if connected, False otherwise
     */
    public boolean isConnected() {
        return mConnected;
    }

    protected ConnectionSettings getConnectionSettings() {
        return mConnectionSettings;
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

    public void send(MQTTPacket packet) {
        synchronized (mPendingQueue) {
            mPendingQueue.add(packet);
        }
    }

    public void startLoop() {
        synchronized (mLock) {
            mLooper = new Thread(this::loop);
            mConnected = true;
            mLooper.start();
        }
    }
    /**
     * Disconnect this client sending a disconnect packet.
     * @return A Future for disconnect task.
     */
    public CompletableFuture<Boolean> disconnect() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (mLock) {
                if (isConnected()) {
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
                        mConnected = false;
                    }
                }
                return isConnected();
            }
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
                        mTimeLastMessageArrived = System.currentTimeMillis();
                        mPacketDispatcher.dispatch(incoming);
                    } else throw new IOException("Socket Closed!");
                } catch (SocketTimeoutException ignored) { } // ignored for because, is like to polling read from socket.


                long now = System.currentTimeMillis();
                if(now - mTimeLastMessageArrived > getKeepAliveTimeout()) {
                    // keep alive ends
                    onKeepAliveTimeout();
                }
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
    private void sendPendingQueue() throws IOException {
        for(MQTTPacket packet : mPendingQueue) {
            mTransport.writePacket(packet);
            // for packet send with qos > qos0, enqueue it because need to be acknowledged
            if(packet.getQos().ordinal() > Qos.QOS_0.ordinal())
                getClientSession().getSendedNotAck().add(packet);

            // after send remove from pending queue
            synchronized (mPendingQueue) {
                mPendingQueue.remove(packet);
            }
        }
    }

    /**
     * Publish a message
     * @param message Message to be published.
     */
    public void publish(Message message) {
        synchronized (mPendingQueue) {
            mPendingQueue.add(new Publish(message));
        }
    }

    /**
     * Subscribe to specific topic with Qos.
     * @param topic Topic to be subscribed
     * @param qos Qos of subscription.
     */
    public void subscribe(String topic, Qos qos) {
        synchronized (mPendingQueue) {
            mPendingQueue.add(new Subscribe(topic, qos));
        }
    }

    /**
     * Unsubscribe from specific Topic.
     * @param topic Topic
     */
    public void unsubscribe(String topic) {
        synchronized (mPendingQueue) {
            mPendingQueue.add(new Unsubscribe(topic));
        }
    }

    protected abstract void onKeepAliveTimeout() throws MQTTProtocolException;
    protected abstract long getKeepAliveTimeout();

    @Override
    public void onPublishReceive(Publish publish) {
        if(publish.getQos() == Qos.QOS_0) {
            mClientCallback.onMessageArrived(this, publish.getMessage());
        } else if(publish.getQos() == Qos.QOS_1) {
            mClientCallback.onMessageArrived(this, publish.getMessage());
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
            mClientCallback.onDeliveryComplete(this, pubAck.getMessageID());
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
                    mClientCallback.onMessageArrived(this, ((Publish) packet).getMessage());
                });
    }

    @Override
    public void onPubCompReceive(PubComp pubComp) {
        boolean removed = getClientSession().getReceivedNotAck()
                .removeIf(packet -> (packet instanceof PubRec) && ((PubRec)packet).getMessageID() == pubComp.getMessageID());
        if(removed)
            mClientCallback.onDeliveryComplete(this, pubComp.getMessageID());
    }
}
