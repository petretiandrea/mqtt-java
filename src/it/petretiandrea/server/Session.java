package it.petretiandrea.server;

import it.petretiandrea.common.QueueMQTT;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.packet.Publish;
import it.petretiandrea.core.packet.Subscribe;
import it.petretiandrea.core.packet.base.MQTTPacket;

public class Session {

    private String mClientID;
    /**
     * True if this session need to be save on persistant storage, False if at the end of connection can be deleted.
     */
    private final boolean mCleanSession;
    /**
     * List of Topic subscriptions.
     */
    private QueueMQTT<Subscribe> mSubscriptions;

    /* QoS 1 and QoS 2 messages which have been sent to the Client, but have not been completely acknowledged. */
    private QueueMQTT<MQTTPacket> mSendedNotAck;

    /* QoS 1 and QoS 2 messages pending transmission to the Client. */
    /* Optionally, QoS 0 messages pending transmission to the Client. */
    private QueueMQTT<MQTTPacket> mPending;

    /* QoS 2 messages which have been received from the Client, but have not been completely acknowledged. */
    private QueueMQTT<MQTTPacket> mReceivedNotAck;

    public Session(String clientID, boolean cleanSession) {
        mClientID = clientID;
        mCleanSession = cleanSession;
        mSubscriptions = new QueueMQTT<>();
        mSendedNotAck = new QueueMQTT<>();
        mPending = new QueueMQTT<>();
        mReceivedNotAck = new QueueMQTT<>();
    }

    public boolean isCleanSession() {
        return mCleanSession;
    }

    public String getClientID() {
        return mClientID;
    }

    public QueueMQTT<Subscribe> getSubscriptions() {
        return mSubscriptions;
    }

    public QueueMQTT<MQTTPacket> getSendedNotAck() {
        return mSendedNotAck;
    }

    public QueueMQTT<MQTTPacket> getPending() {
        return mPending;
    }

    public QueueMQTT<MQTTPacket> getReceivedNotAck() {
        return mReceivedNotAck;
    }

    public void addPendingPublish(Message message) {
        getPending().add(new Publish(message));
    }
}
