package it.petretiandrea.common;

import it.petretiandrea.core.Message;

public interface MQTTClientCallback {

    void onConnectionLost(Throwable throwable);
    void onMessageArrived(Message message);
    void onDeliveryComplete(int messageId);
}
