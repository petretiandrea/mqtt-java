package it.petretiandrea.common;

import it.petretiandrea.core.Message;
import it.petretiandrea.core.packet.Subscribe;

public interface MQTTClientCallback {

    void onMessageArrived(Message message);
    void onDeliveryComplete(int messageId);
    void onConnectionLost(Client client, Throwable ex);
    void onDisconnect(Client client);
}
