package it.petretiandrea.common;

import it.petretiandrea.core.Message;
import it.petretiandrea.core.packet.Subscribe;

public interface MQTTClientCallback {

    void onConnectionLost(Throwable throwable);
    void onMessageArrived(Message message);
    void onDeliveryComplete(int messageId);
    void onSubscribe(Subscribe subscribe);
}
