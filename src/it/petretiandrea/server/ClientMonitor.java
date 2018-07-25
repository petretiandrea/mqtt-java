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

    private void checkQueue() {
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

    private void handleReceivePacket(MQTTPacket packet) throws IOException, MQTTProtocolException {
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
            case SUBSCRIBE: {
                Subscribe sub = (Subscribe) packet;
                getSession().getPending().add(new SubAck(sub.getMessageID(), sub.getQosSub()));
                getSession().getSubscriptions().add(sub);
                System.out.println(getSession().getClientID() + "\tsubscribed to: " + sub.getTopic());
                break;
            }
            case PUBLISH: {
                Publish pub = (Publish) packet;
                if(pub.getQos() == Qos.QOS_0) {
                    mServerComm.onPublishMessageReceived(pub.getMessage());
                } else if(pub.getQos() == Qos.QOS_1) {
                    mServerComm.onPublishMessageReceived(pub.getMessage());
                    getSession().getPending().add(new PubAck(pub.getMessage().getMessageID()));
                } else if(pub.getQos() == Qos.QOS_2) {
                    // store message into QOS 1, QOS 2 sent to client but not ack
                    getSession().getReceivedNotAck().add(pub);
                    getSession().getPending().add(new PubRec(pub.getMessage().getMessageID()));
                }
                break;
            }
            case PUBACK: {

                break;
            }
            /*case PUBREL: {
                PubRel rel = (PubRel) packet;
                Publish publish = (Publish) getSession().getPending().stream().filter(packet12 -> {
                    if(packet12 instanceof PubRec)
                        return ((PubRec) packet12).getMessageID() == rel.getMessageID();
                    return false;
                }).findFirst().orElse(null);
                if(publish != null) {
                    getSession().getPending().remove(publish);
                    // send to other
                    mServerComm.onPublishMessageReceived(publish.getMessage());
                    // write pubcomp
                    mTransport.writePacket(new PubComp(rel.getMessageID()));
                    System.out.println("Message QOS 2 received!");
                }
                break;
            }
            case PUBACK: {// for published message with QOS_1;
                PubAck pubAck = (PubAck) packet;
                // find publish message with same messageid.
                Publish publish = (Publish) getSession().getSended().stream().filter(packet1 -> {
                    if(packet1 instanceof Publish)
                        return ((Publish) packet1).getMessage().getMessageID() == pubAck.getMessageID();
                    return false;
                }).findFirst().orElse(null);
                if(publish != null) {
                    getSession().getSended().remove(publish);
                    // call interface for signal a QOS_1 publish message
                    System.out.println("Message " + publish.getMessage().getMessageID() + " is published with QOS_1");
                }
                break;
            }
            case PUBREC: {
                // 1st step for publish a QOS_2 message
                PubRec pubRec = (PubRec) packet;
                Publish publish = (Publish) getSession().getSended().stream().filter(packet1 -> {
                    if(packet1 instanceof Publish)
                        return ((Publish) packet1).getMessage().getMessageID() == pubRec.getMessageID();
                    return false;
                }).findFirst().orElse(null);
                if(publish != null) {
                    // found remove it
                    getSession().getSended().remove(publish);
                    // add pub rec to incoming message
                    getSession().getPending().add(pubRec);
                    // send a pub rel
                    mTransport.writePacket(new PubRel(pubRec.getMessageID()));
                }
                break;
            }
            case PUBCOMP: { // 2st step for publish a QOS_2 message
                PubComp comp = (PubComp) packet;
                // find a stored pubRec
                PubRec pubRec = (PubRec) getSession().getPending().stream().filter(packet12 -> {
                    if(packet12 instanceof PubRec)
                        return ((PubRec) packet12).getMessageID() == comp.getMessageID();
                    return false;
                }).findFirst().orElse(null);
                if(pubRec != null) {
                    // found remove it!
                    getSession().getPending().remove(pubRec);
                    // call interface for signal a QOS_2 publish message
                    System.out.println("Message " + comp.getMessageID() + " is published with QOS_2");
                }
                break;
            }
            case UNSUBSCRIBE: {
                Unsubscribe unsubscribe = (Unsubscribe) packet;
                getSession().getSubscriptions().stream()
                        .filter(subscribe -> subscribe.getTopic().equals(unsubscribe.getTopic()))
                        .findAny()
                        .ifPresent(subscribe -> getSession().getSubscriptions().remove(subscribe));
                mTransport.writePacket(new UnsubAck(unsubscribe.getMessageID()));
                System.out.println("Unsubscribe from: " + unsubscribe.getTopic());
                break;
            }*/
        }
    }

    public void publish(Message message) {
        Publish publish = new Publish(message);
        getSession().getPending().add(publish);
        /*Publish publish = new Publish(message);
        try {
            if (message.getQos().ordinal() > Qos.QOS_0.ordinal()) {
                // need to add to queue for wait the puback, pubrel, ecc. for QOS 1 or 2.
                getSession().getSended().add(publish);
            }
            mTransport.writePacket(publish);
        } catch (IOException e) {
            e.printStackTrace();
            getSession().getSended().remove(publish);
        }*/
    }
}
