package it.petretiandrea.client;

import it.petretiandrea.common.MQTTClientCallback;
import it.petretiandrea.common.PacketDispatcher;
import it.petretiandrea.common.QueueMQTT;
import it.petretiandrea.common.session.Session;
import it.petretiandrea.common.network.Transport;
import it.petretiandrea.common.network.TransportTCP;
import it.petretiandrea.core.*;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class MQTTClient {

    private static int SOCKET_IO_TIMEOUT = (int) (0.5 * 1000);

    private Thread mLoop;
    private boolean mRunning;
    private ReentrantLock mLock;
    private Transport mTransport;

    private Session mSession;
    private PacketDispatcher mPacketDispatcher;

    private final ConnectionSettings mConnectionSettings;
    private QueueMQTT<MQTTPacket> mOutgoing;
    private long mLastMsgReceivedTime;
    private long mPingRespTime;

    private long mAliveTimeout;
    private long mPingRespTimeout;

    private MQTTClientCallback mClientCallback;



    public MQTTClient(ConnectionSettings connectionSettings, MQTTClientCallback clientCallback) {
        mConnectionSettings = connectionSettings;
        mRunning = false;
        mOutgoing = new QueueMQTT<>();
        mLock = new ReentrantLock(true);
        mAliveTimeout = (connectionSettings.getKeepAliveSeconds() - (connectionSettings.getKeepAliveSeconds() / 4)) * 1000;
        mPingRespTimeout = (connectionSettings.getKeepAliveSeconds() + (connectionSettings.getKeepAliveSeconds() / 4)) * 1000;
        mPacketDispatcher = new PacketDispatcher(new ClientReceiver());
        mClientCallback = clientCallback;
    }

    private void setRunning(boolean running) {
        mLock.lock();
        try {
            mRunning = running;
        } finally {
            mLock.unlock();
        }
    }

    private boolean isRunning() {
        mLock.lock();
        try {
            return mRunning;
        } finally {
            mLock.unlock();
        }
    }

    public boolean connect() throws IOException, MQTTParseException, MQTTProtocolException {
        if(!isRunning() && mTransport == null)
            mTransport = new TransportTCP();

        if(!isRunning()) {
            mLock.lock();
            try {
                mTransport.connect(new InetSocketAddress(mConnectionSettings.getHostname(), mConnectionSettings.getPort()));
                MQTTPacket connack;
                mTransport.writePacket(new Connect(MQTTVersion.MQTT_311, mConnectionSettings));
                if ((connack = mTransport.readPacket()) != null) {
                    if (connack.getCommand() == MQTTPacket.Type.CONNACK) {
                        if (((ConnAck) connack).getConnectionStatus() == ConnectionStatus.ACCEPT) {
                            mSession = new Session(mConnectionSettings.getClientId(), ((ConnAck) connack).isSessionPresent());
                            mLoop = new Thread(this::loop);
                            setRunning(true);
                            mLoop.start();
                            return true;
                        } else throw new MQTTProtocolException(((ConnAck) connack).getConnectionStatus().toString());
                    }
                }
            } finally {
                mLock.unlock();
            }
        }
        return false;
    }

    private boolean isConnect() {
        return isRunning();
    }

    public CompletableFuture<Boolean> disconnect() {
        return CompletableFuture.supplyAsync(() -> {
            if(isConnect()) {
                mLock.lock();
                try {
                    mTransport.writePacket(new Disconnect());
                    mTransport.close();
                    mLoop.interrupt();
                    mLoop.join();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mTransport = null;
                    mLoop = null;
                    mTransport = null;
                    mLock.unlock();
                }
            }
            return !isConnect();
        });
    }

    public void publish(Message message) {
        if(isConnect()) {
            mOutgoing.add(new Publish(message));
        }
    }

    public void subscribe(String topic, Qos qos) {
        if(isConnect()) {
            mOutgoing.add(new Subscribe(topic, qos));
        }
    }

    public Session getSession() {
        return mSession;
    }

    private void loop() {
        try {
            while(!Thread.interrupted() && isRunning()) {

                try {
                    // send pending message
                    mOutgoing.forEach(packet -> {
                            try {
                                mTransport.writePacket(packet);
                                if(packet.getQos().ordinal() > Qos.QOS_0.ordinal())
                                    getSession().getSendedNotAck().add(packet);
                                // if sended remove from queue
                                mOutgoing.remove(packet);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    // read incoming message and dispatch it.
                    mPacketDispatcher.dispatch(mTransport.readPacket(SOCKET_IO_TIMEOUT));
                    mLastMsgReceivedTime = mPingRespTime = System.currentTimeMillis();
                } catch (SocketTimeoutException ignored) { }

                // check keepalive
                long now = System.currentTimeMillis();
                if(now - mLastMsgReceivedTime > mAliveTimeout) {
                    mOutgoing.add(new PingReq()); // send a ping request.
                    mPingRespTime = now;
                } else if(now - mPingRespTime > mPingRespTimeout)
                    throw new MQTTProtocolException("No ping response received");
            }
        } catch (IOException | MQTTProtocolException | MQTTParseException ex) {
            ex.printStackTrace();
          //  mClientCallback.onConnectionLost(ex);
        } finally {
            disconnect();
        }
    }

    public void unsubscribe(String topic) {

    }

    private class ClientReceiver implements PacketDispatcher.IPacketReceiver {

        @Override
        public void onPingRespReceive(PingResp pingResp) {
            // reset the time of pingresp
            mPingRespTime = System.currentTimeMillis();
        }

        @Override
        public void onPublishReceive(Publish publish) {
            if(publish.getQos() == Qos.QOS_0) {
                mClientCallback.onMessageArrived(publish.getMessage());
            } else if(publish.getQos() == Qos.QOS_1) {
                mClientCallback.onMessageArrived(publish.getMessage());
                mOutgoing.add(new PubAck(publish.getMessage().getMessageID()));
            } else if(publish.getQos() == Qos.QOS_2) {
                getSession().getReceivedNotAck().add(publish);
                mOutgoing.add(new PubRec(publish.getMessage().getMessageID()));
            }
        }

        @Override
        public void onConnectReceive(Connect connect) {

        }

        @Override
        public void onConnAckReceive(ConnAck connAck) {

        }

        @Override
        public void onPubAckReceive(PubAck pubAck) {
            boolean removed = getSession().getSendedNotAck()
                    .removeIf(packet -> (packet instanceof Publish) && ((Publish)packet).getMessage().getMessageID() == pubAck.getMessageID());
            if(removed)
                mClientCallback.onDeliveryComplete(pubAck.getMessageID());
        }

        @Override
        public void onPubRecReceive(PubRec pubRec) {
            getSession().getSendedNotAck()
                    .removeIf(packet -> (packet instanceof Publish) && ((Publish)packet).getMessage().getMessageID() == pubRec.getMessageID());
            // store pubrec
            getSession().getReceivedNotAck().add(pubRec);
            mOutgoing.add(new PubRel(pubRec.getMessageID()));
        }

        @Override
        public void onPubRelReceive(PubRel pubRel) {
            getSession().getReceivedNotAck().stream()
                    .filter(packet -> (packet instanceof  Publish) && ((Publish)packet).getMessage().getMessageID() == pubRel.getMessageID())
                    .findFirst()
                    .ifPresent(packet -> {
                        mOutgoing.add(new PubComp(pubRel.getMessageID()));
                        mClientCallback.onMessageArrived(((Publish) packet).getMessage());
                    });
        }

        @Override
        public void onPubCompReceive(PubComp pubComp) {
            boolean removed = getSession().getReceivedNotAck()
                    .removeIf(packet -> (packet instanceof PubRec) && ((PubRec)packet).getMessageID() == pubComp.getMessageID());
            if(removed)
                mClientCallback.onDeliveryComplete(pubComp.getMessageID());
        }

        @Override
        public void onUnsubAckReceive(UnsubAck unsubAck) {
            getSession().getSendedNotAck().stream()
                    .filter(packet -> (packet instanceof Unsubscribe) && ((Unsubscribe)packet).getMessageID() == unsubAck.getMessageID())
                    .findFirst()
                    .ifPresent(packet -> {
                        getSession().getSendedNotAck().remove(packet);
                        System.out.println("Unsubscribe complete from "  + packet);
                    });
        }

        @Override
        public void onSubscribeReceive(Subscribe subscribe) {

        }

        @Override
        public void onSubAckReceive(SubAck subAck) {

        }

        @Override
        public void onUnsubscribeReceive(Unsubscribe unsubscribe) {

        }

        @Override
        public void onPingReqReceive(PingReq pingReq) {

        }

        @Override
        public void onDisconnect(Disconnect disconnect) {

        }
    }
}
