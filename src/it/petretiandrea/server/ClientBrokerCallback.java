package it.petretiandrea.server;

import it.petretiandrea.core.Message;
import it.petretiandrea.core.packet.Subscribe;
import it.petretiandrea.core.packet.Unsubscribe;

/**
 * Interface for communicate with the main MQTT Server.
 */
public interface ClientBrokerCallback {

    void onClientDisconnect(ClientBroker clientMonitor);
    void onSubscriptionReceived(ClientBroker client, Subscribe subscribe);
    void onPublishMessageReceived(Message message);
    void onUnsubscribeReceived(ClientBroker clientBroker, Unsubscribe unsubscribe);

}
