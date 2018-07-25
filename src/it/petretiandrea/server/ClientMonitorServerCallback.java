package it.petretiandrea.server;

import it.petretiandrea.core.Message;
import it.petretiandrea.core.packet.Subscribe;

/**
 * Interface for communicate with the main MQTT Server.
 */
public interface ClientMonitorServerCallback {

    void onClientDisconnect(ClientMonitor clientMonitor);
    void onSubscriptionReceived(ClientMonitor client, Subscribe subscribe);
    void onPublishMessageReceived(Message message);


}
