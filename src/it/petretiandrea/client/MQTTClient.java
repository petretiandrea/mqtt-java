package it.petretiandrea.client;

import it.petretiandrea.common.Session;
import it.petretiandrea.common.network.Transport;
import it.petretiandrea.common.network.TransportTCP;
import it.petretiandrea.core.*;
import it.petretiandrea.core.exception.MQTTException;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.core.exception.MQTTParseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

// TODO: Add a read method inside network layer, for read with specific timeout.
// TODO: And use this method for wait a ConnAck, with timeout = connectionSettings.getKeepAliveSeconds() * 1000;
public class MQTTClient {

    private static int SOCKET_IO_TIMEOUT = (int) (0.5 * 1000);

    private Transport mTransport;
    private Session mSession;

    private ConnectionSettings mConnectionSettings;
    private boolean mConnected;

    private Thread mReadThread;
    // guard for mConnected;
    private ReentrantLock mLock;

    private long mLastPingRequest;
    private long mLastPingResponse;
    private long mPingRequestTimeout;
    private long mPingResponseTimeout;

    public MQTTClient(ConnectionSettings connectionSettings) {
        mConnected = false;
        mReadThread = new Thread(this::readTaskThread);
        mConnectionSettings = connectionSettings;
        mLastPingRequest = mLastPingResponse = System.currentTimeMillis();
        mLock = new ReentrantLock(true);

        // timeout for send a request ping is the real keep alive - 1/4 of real keep alive.
        mPingRequestTimeout = (connectionSettings.getKeepAliveSeconds() - (connectionSettings.getKeepAliveSeconds() / 4)) * 1000;
        // timeout for receive ping response is the real keep alive + 1/4 of real keep alive.
        mPingResponseTimeout = (connectionSettings.getKeepAliveSeconds() + (connectionSettings.getKeepAliveSeconds() / 4)) * 1000;
    }

    public Session getSession() {
        return mSession;
    }

    /**
     * Get the connected status
     * @return True if client is connect, false otherwise.
     */
    public boolean isConnected() {
        mLock.lock();
        try {
            return mConnected;
        } finally {
            mLock.unlock();
        }
    }

    public void setConnected(boolean connected) {
        mLock.lock();
        try {
            mConnected = connected;
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Connect to MQTT broker using the connection settings.
     * @return True if the connection is successful, false otherwise.
     * @throws MQTTParseException If the server response is invalid.
     * @throws IOException If there is and error on socket.
     * @throws MQTTException If the server not accept the connection.
     */
    public boolean connect() throws MQTTParseException, IOException, MQTTException {
        setConnected(connectMQTT());
        return isConnected();
    }

    /**
     * Disconnect the client.
     * @return True if disconnection is successful, False otherwise.
     */
    public CompletableFuture<Boolean> disconnect() {
        return CompletableFuture.supplyAsync(() -> {
            if(isConnected()) {
                mLock.lock();
                try {
                    mTransport.writePacket(new Disconnect());
                    mTransport.close();
                    mReadThread.interrupt();
                    setConnected(false);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mTransport = null;
                    mReadThread = null;
                    mLock.unlock();
                }
            }
            return !isConnected();
        });
    }

    /**
     * Publish a message.
     * @param message Message need to be published.
     */
    public void publish(Message message) {
        if(isConnected()) {
            Publish publish = new Publish(message);
            if(message.getQos().ordinal() > Qos.QOS_0.ordinal())
                getSession().getSendedNotAck().add(publish);

            try {
                mTransport.writePacket(publish);
            } catch (IOException e) {
                e.printStackTrace();
                getSession().getSendedNotAck().remove(publish);
            }
        }
    }

    /**
     * Subscribe to specific Topic with specific Qos.
     * @param topic Topic to subscribe
     * @param qos Qos of message received from with this topic.
     */
    public void subscribe(String topic, Qos qos) {
        if(isConnected()) {
            Subscribe subscribe = new Subscribe(topic, qos);
            try {
                getSession().getSendedNotAck().add(subscribe);
                mTransport.writePacket(subscribe);
            } catch (IOException ex) {
                ex.printStackTrace();
                getSession().getSendedNotAck().remove(subscribe);
            }
        }
    }

    // same of connect()
    private boolean connectMQTT() throws IOException, MQTTException, MQTTParseException {
        if(!mConnected && mTransport == null) {
            mTransport = new TransportTCP();
        }

        if(!mConnected) {
             // milliseconds
            mTransport.connect(new InetSocketAddress(mConnectionSettings.getHostname(), mConnectionSettings.getPort()));
            MQTTPacket connack = null;
            mTransport.writePacket(new Connect(MQTTVersion.MQTT_311, mConnectionSettings));
            if((connack = mTransport.readPacket()) != null) {
                if(connack.getCommand() == MQTTPacket.Type.CONNACK) {
                    if (((ConnAck) connack).getConnectionStatus() == ConnectionStatus.ACCEPT) {
                        mSession = new Session(mConnectionSettings.getClientId(), ((ConnAck) connack).isSessionPresent());
                        mReadThread.start();
                        return true;
                    } else throw new MQTTException(((ConnAck) connack).getConnectionStatus().toString());
                }
            }
        }
        return false;
    }

    // behaviour of Read Thread.
    private void readTaskThread() {
        try {
            while (!Thread.interrupted() && isConnected()) {
                try {
                    MQTTPacket packet = mTransport.readPacket(SOCKET_IO_TIMEOUT);
                    if(packet != null) {
                        // a packet is arrived, reset keep alive
                        resetKeepAliveTimeout();
                        handleMQTTPacket(packet);
                        System.out.println(packet);
                    }
                } catch (SocketTimeoutException ignored) {
                } finally {
                    // Keep alive algorithm
                    checkKeepAlive();
                }
            }
        } catch (IOException | MQTTProtocolException | MQTTParseException ex) {
            ex.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private void resetKeepAliveTimeout() {
        mLastPingRequest = mPingResponseTimeout = System.currentTimeMillis();
    }

    /**
     * Method for check the KeepAlive status.
     * @throws IOException If there is an error during Ping Request
     * @throws MQTTProtocolException If the timeout for receive a Ping Response from server expire.
     */
    private void checkKeepAlive() throws IOException, MQTTProtocolException {
        long now = System.currentTimeMillis();
        if(now - mLastPingRequest >= mPingRequestTimeout) {
            // timeout reached need to send ping req.
            mTransport.writePacket(new PingReq());
            resetKeepAliveTimeout();
            System.out.println("Sended PING REQUEST");
        } else if(now - mLastPingResponse > mPingResponseTimeout) {
            // here no response from server from my ping request, throw and exception for no ping response received
            throw new MQTTProtocolException(String.format("No Ping Response received %d ms", mPingResponseTimeout));
        }
    }

    /**
     * Method for handle the incoming packet.
     * @param packet The incoming packet
     * @throws IOException If there is an error during response on socket.
     */
    private void handleMQTTPacket(MQTTPacket packet) throws IOException {
        switch (packet.getCommand()) {
            case SUBACK: {
                SubAck subAck = (SubAck) packet;
                getSession().getSendedNotAck().stream()
                        .filter(packet1 -> (packet1 instanceof Subscribe) &&((Subscribe) packet1).getMessageID() == subAck.getMessageID())
                        .findFirst()
                        .ifPresent(packet16 -> {
                            getSession().getSendedNotAck().remove(packet16);
                            System.out.println("Subscribed to: " + ((Subscribe) packet16).getTopic());
                        });
                break;
            }
            case PUBLISH: { // receive a message from broker
                Publish publish = (Publish) packet;
                if(publish.getQos() == Qos.QOS_0) {
                    // call interface for received message
                    System.out.println("Received message: " + publish.getMessage());
                } else if(publish.getQos() == Qos.QOS_1) {
                    System.out.println("Received message: " + publish.getMessage());
                    mTransport.writePacket(new PubAck(publish.getMessage().getMessageID()));
                } else if(publish.getQos() == Qos.QOS_2) {
                    getSession().getReceivedNotAck().add(publish);
                    mTransport.writePacket(new PubRec(publish.getMessage().getMessageID()));
                }
                break;
            }
            case PUBACK: { // for published message with QOS_1;
                PubAck pubAck = (PubAck) packet;
                getSession().getSendedNotAck().stream()
                        .filter(packet1 -> (packet1 instanceof Publish) &&((Publish) packet1).getMessage().getMessageID() == pubAck.getMessageID())
                        .findFirst()
                        // now i can remove the published message, beacouse is acked.
                        .ifPresent(publish -> {
                            getSession().getSendedNotAck().remove(publish);
                            System.out.println("Message " + pubAck.getMessageID() + " is published with QOS_1");
                        });
                break;
            }
            case PUBREC: { // 1st step for publish a QOS_2 message
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
            case PUBCOMP: { // 2st step for publish a QOS_2 message
                PubComp pubComp = (PubComp) packet;
                // removed pub rec received but not completly acked.
                getSession().getReceivedNotAck()
                        .removeIf(packet14 -> (packet14 instanceof PubRec) && ((PubRec) packet14).getMessageID() == pubComp.getMessageID());
                System.out.println("QOS_2 Publish completed!");
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
                                System.out.println("Received a publish message! " + rel.getMessageID());
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
            case PINGREQ: // server request for a ping
                System.out.println("Received PING REQUEST");
                mTransport.writePacket(new PingResp()); // send a ping response to server.
                break;
            case PINGRESP:
                System.out.println("Received PING RESPONSE");
                mLastPingResponse = System.currentTimeMillis(); // save the last response from server.
                break;
        }
    }
}
