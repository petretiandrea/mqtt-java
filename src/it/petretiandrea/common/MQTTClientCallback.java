package it.petretiandrea.common;

import it.petretiandrea.core.Message;
import it.petretiandrea.core.packet.Subscribe;
import it.petretiandrea.core.packet.Unsubscribe;

public interface MQTTClientCallback {

    void onMessageArrived(Client client, Message message);
    void onDeliveryComplete(Client client, int messageId);
    void onConnectionLost(Client client, Throwable ex);
    void onDisconnect(Client client);
    void onSubscribeComplete(Client client, Subscribe subscribe);
    void onUnsubscribeComplete(Client client, Unsubscribe unsubscribe);
}
