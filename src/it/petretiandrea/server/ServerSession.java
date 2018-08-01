package it.petretiandrea.server;

import it.petretiandrea.common.QueueMQTT;
import it.petretiandrea.common.session.Session;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.packet.Publish;
import it.petretiandrea.core.packet.Subscribe;
import it.petretiandrea.core.packet.base.MQTTPacket;

public class ServerSession extends Session {

    /**
     * List of Topic subscriptions.
     */
    private QueueMQTT<Subscribe> mSubscriptions;

    /* QoS 1 and QoS 2 messages pending transmission to the MQTTClient. */
    /* Optionally, QoS 0 messages pending transmission to the MQTTClient. */
    private QueueMQTT<MQTTPacket> mPending;

    public ServerSession(String clientID, boolean cleanSession) {
        super(clientID, cleanSession);
        mSubscriptions = new QueueMQTT<>();
        mPending = new QueueMQTT<>();
    }

    public QueueMQTT<Subscribe> getSubscriptions() {
        return mSubscriptions;
    }

    public QueueMQTT<MQTTPacket> getPending() {
        return mPending;
    }


    public void addPendingPublish(Message message) {
        getPending().add(new Publish(message));
    }
}
