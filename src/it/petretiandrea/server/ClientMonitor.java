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
import java.util.function.Predicate;

public class ClientMonitor {

    private static int SOCKET_IO_TIMEOUT = (int) (0.5 * 1000);

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

    private final long mPingTimeout;

    public ClientMonitor(Transport transport, Session session, Connect settings, ClientMonitorServerCallback serverComm) {
        mTransport = transport;
        mSession = session;
        mConnectSettings = settings;
        mThread = new Thread(this::run);
        mServerComm = serverComm;
        mPingTimeout = (mConnectSettings.getKeepAliveSeconds() + (mConnectSettings.getKeepAliveSeconds() / 4)) * 1000;
    }

    public void start() {
        mThread.start();
    }

    /**
     * Publish a message.
     * @param message Message need to be published.
     */
    public void publish(Message message) {
        Publish publish = new Publish(message);
        getSession().getPending().add(publish);
    }

    /**
     * Get the session associate to this client.
     * @return The session of this client.
     */
    public Session getSession() {
        return mSession;
    }

    /**
     * Close the connection of this client.
     * @return A Task that disconnect the client.
     */
    public CompletableFuture<Void> closeConnection() {
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

    /* Behaviour of this Client */
    private void run() {
        try {
            // initialize the timeout for keepalive.
            mLastPacketReceived = System.currentTimeMillis();
            while (!Thread.interrupted()) {
                try {
                    // process and send the packet on pending queue.
                    handlePendingPacket();
                    // read packet
                    MQTTPacket packet = mTransport.readPacket(SOCKET_IO_TIMEOUT);
                    if(packet != null) {
                        // reset the keep alive, if a packet income
                        resetTimeoutKeepAlive();
                        handleReceivePacket(packet);
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
        if(now - mLastPacketReceived > mPingTimeout) {
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

    /**
     * Handle and send all packet pending of session.
     */
    private void handlePendingPacket() {
        // check for pending message to be sended
        // send it and add to sended not ack.
        if(getSession().getPending().size() > 0) {
            getSession().getPending().forEach(mqttPacket -> {
                try {
                    mTransport.writePacket(mqttPacket);
                    // here if send is correct, remove from queue
                    getSession().getPending().remove(mqttPacket);
                    if(mqttPacket.getQos().ordinal() >= Qos.QOS_1.ordinal())
                        getSession().getSendedNotAck().add(mqttPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Handle and manage a MQTT Packet Received!
     * @param packet Packet received!
     * @throws IOException Throw if there is and error during send a response.
     * @throws MQTTProtocolException Throw if there is a protocol violation.
     */
    private void handleReceivePacket(MQTTPacket packet) throws IOException, MQTTProtocolException {
        switch (packet.getCommand()) {
            case DISCONNECT:
                closeConnection();
                break;
            case CONNECT:
                // duplicate connect packet
                throw new MQTTProtocolException("Duplicate CONNECT Packet!");
            case PINGREQ:
                mTransport.writePacket(new PingResp());
                break;
            case SUBSCRIBE: {
                Subscribe sub = (Subscribe) packet;
                mTransport.writePacket(new SubAck(sub.getMessageID(), sub.getQosSub()));
                // here only if write packet have success, and add it to subscribe topics
                getSession().getSubscriptions().add(sub);
                System.out.println(getSession().getClientID() + "\tsubscribed to: " + sub.getTopic());
                mServerComm.onSubscriptionReceived(this, sub);
                break;
            }
            case PUBLISH: {
                Publish pub = (Publish) packet;
                if(pub.getQos() == Qos.QOS_0) {
                    mServerComm.onPublishMessageReceived(pub.getMessage());
                } else if(pub.getQos() == Qos.QOS_1) {
                    mServerComm.onPublishMessageReceived(pub.getMessage());
                    mTransport.writePacket(new PubAck(pub.getMessage().getMessageID()));
                } else if(pub.getQos() == Qos.QOS_2) {
                    // store message into QOS 1, QOS 2 sent to client but not completly acked.
                    getSession().getReceivedNotAck().add(pub);
                    mTransport.writePacket(new PubRec(pub.getMessage().getMessageID()));
                }
                break;
            }
            case PUBACK: {
                PubAck pubAck = (PubAck) packet;
                getSession().getSendedNotAck().stream()
                        .filter(packet1 -> (packet1 instanceof Publish) &&((Publish) packet1).getMessage().getMessageID() == pubAck.getMessageID())
                        .findFirst()
                        // now i can remove the published message, beacouse is acked.
                        .ifPresent(publish -> getSession().getSendedNotAck().remove(publish));
                break;
            }
            case PUBREL: { // step for receive from client publish QOS_2
                PubRel rel = (PubRel) packet;
                // remove the message received, beacouse now is completly acked.
                getSession().getReceivedNotAck().stream()
                        .filter(packet12 -> (packet12 instanceof Publish) && ((Publish) packet12).getMessage().getMessageID() == rel.getMessageID())
                        .findFirst()
                        .ifPresent(packet15 -> {
                            if(packet15 instanceof Publish) {
                                // publish to other clients subscribed
                                mServerComm.onPublishMessageReceived(((Publish) packet15).getMessage());
                                // send a pubcomp
                                try {
                                    mTransport.writePacket(new PubComp(rel.getMessageID()));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                break;
            }
            case PUBREC: { // 1st step for QOS_2 message sended to Client.
                PubRec rec = (PubRec) packet;
                // discard the publish message from sended not acked.
                getSession().getSendedNotAck()
                        .removeIf(packet13 -> (packet13 instanceof Publish) && ((Publish) packet13).getMessage().getMessageID() == rec.getMessageID());
                // store pub rec received.
                getSession().getReceivedNotAck().add(rec);
                // send a pub rel.
                mTransport.writePacket(new PubRel(rec.getMessageID()));
                break;
            }
            case PUBCOMP: { // 2rd step for QOS_2 message sended to Client
                PubComp pubComp = (PubComp) packet;
                // removed pub rec received but not completly acked.
                getSession().getReceivedNotAck()
                        .removeIf(packet14 -> (packet14 instanceof PubRec) && ((PubRec) packet14).getMessageID() == pubComp.getMessageID());
                System.out.println("QOS_2 Publish completed!");
                break;
            }
            case UNSUBSCRIBE: {
                Unsubscribe unsubscribe = (Unsubscribe) packet;
                mTransport.writePacket(new UnsubAck(unsubscribe.getMessageID()));
                getSession().getSubscriptions()
                        .removeIf(subscribe -> subscribe.getTopic().equals(unsubscribe.getTopic()));
                System.out.println(getSession().getClientID() + "\tunsubscribed from: " + unsubscribe.getTopic());
                break;
            }
        }
    }
}
