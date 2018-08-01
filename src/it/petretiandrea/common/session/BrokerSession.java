package it.petretiandrea.common.session;

import it.petretiandrea.common.QueueMQTT;
import it.petretiandrea.core.packet.Publish;
import it.petretiandrea.core.packet.Subscribe;
import it.petretiandrea.server.Broker;

import java.util.List;
import java.util.function.Consumer;

public class BrokerSession extends Session {

    /**
     * List of Topic subscriptions.
     */
    private QueueMQTT<Subscribe> mSubscriptions;

    /* QoS 1 and QoS 2 messages pending transmission to the MQTTClient. */
    /* Optionally, QoS 0 messages pending transmission to the MQTTClient. */
    private QueueMQTT<Publish> mPendingPublish;

    public BrokerSession(String clientID, boolean cleanSession) {
        super(clientID, cleanSession);
        mSubscriptions = new QueueMQTT<>();
        mPendingPublish = new QueueMQTT<>();
    }

    public BrokerSession(ClientSession clientSession, List<Subscribe> subscription) {
        this(clientSession.getClientID(), clientSession.isCleanSession(), subscription);
    }

    public BrokerSession(String clientID, boolean cleanSession, List<Subscribe> subscription) {
        super(clientID, cleanSession);
        mSubscriptions = new QueueMQTT<>();
        mSubscriptions.addAll(subscription);
        mPendingPublish = new QueueMQTT<>();
    }

    public QueueMQTT<Subscribe> getSubscriptions() {
        return mSubscriptions;
    }

    public QueueMQTT<Publish> getPendingPublish() {
        return mPendingPublish;
    }
}
