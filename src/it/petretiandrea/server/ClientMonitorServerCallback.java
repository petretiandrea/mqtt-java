package it.petretiandrea.server;

import it.petretiandrea.core.Message;

/**
 * Interface for communicate with the main MQTT Server.
 */
public interface ClientMonitorServerCallback {

    void onClientDisconnect(ClientMonitor clientMonitor);
    void onPublishMessageReceived(Message message);


}
