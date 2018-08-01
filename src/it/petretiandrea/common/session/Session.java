package it.petretiandrea.common.session;

import it.petretiandrea.common.QueueMQTT;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        this(clientID, cleanSession, Collections.emptyList(), Collections.emptyList());
    }

    public Session(String clientID, boolean cleanSession, List<MQTTPacket> sendedNotAck, List<MQTTPacket> receivedNotAck) {
        mClientID = clientID;
        mCleanSession = cleanSession;
        mSendedNotAck = new QueueMQTT<>();
        mSendedNotAck.addAll(sendedNotAck);
        mReceivedNotAck = new QueueMQTT<>();
        mReceivedNotAck.addAll(receivedNotAck);
    }

    public Session(Session session) {
        this(session.getClientID(), session.isCleanSession(),
                new ArrayList<>(session.mSendedNotAck),
                new ArrayList<>(session.mReceivedNotAck));
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
