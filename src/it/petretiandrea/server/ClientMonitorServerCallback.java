package it.petretiandrea.server;

/**
 * Interface for communicate with the main MQTT Server.
 */
public interface ClientMonitorServerCallback {

    void onClientDisconnect(ClientMonitor clientMonitor);

}
