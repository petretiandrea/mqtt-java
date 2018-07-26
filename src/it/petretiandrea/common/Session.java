package it.petretiandrea.common;

import it.petretiandrea.core.packet.base.MQTTPacket;

public class Session {

    private String mClientID;
    /**
     * True if this session need to be save on persistant storage, False if at the end of connection can be deleted.
     */
    private final boolean mCleanSession;

    /**
     * QoS 1 and QoS 2 messages which have been sent to the Server, but have not been completely acknowledged.
     */
    private QueueMQTT<MQTTPacket> mSendedNotAck;
    /**
     * QoS 2 messages which have been received from the Server, but have not been completely acknowledged.
     */
    private QueueMQTT<MQTTPacket> mReceivedNotAck;

    public Session(String clientID, boolean cleanSession) {
        mClientID = clientID;
        mCleanSession = cleanSession;
        mSendedNotAck = new QueueMQTT<>();
        mReceivedNotAck = new QueueMQTT<>();
    }

    public String getClientID() {
        return mClientID;
    }

    public boolean isCleanSession() {
        return mCleanSession;
    }

    public QueueMQTT<MQTTPacket> getSendedNotAck() {
        return mSendedNotAck;
    }

    public QueueMQTT<MQTTPacket> getReceivedNotAck() {
        return mReceivedNotAck;
    }
}
