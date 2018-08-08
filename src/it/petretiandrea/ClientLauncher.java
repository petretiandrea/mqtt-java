package it.petretiandrea;

import it.petretiandrea.client.MQTTClient;
import it.petretiandrea.core.ConnectionSettingsBuilder;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.utils.CustomLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;

public class ClientLauncher {

    public static void main(String[] args) throws IOException, MQTTProtocolException, NoSuchAlgorithmException {

        // Creazione client che si sotto iscrive soltanto, con le relative impostazioni di connessione
        MQTTClient subscribeClient = new MQTTClient(new ConnectionSettingsBuilder()
                .setClientId(InetAddress.getLocalHost().getHostName() + "_sub") // client id impostato ad hostname
                .setHostname("localhost")
                .setPort(1883)
                .setCleanSession(false) // sessione mantenuta al momento della disconnessione
                .setKeepAliveSeconds(20) // keep alive di 20 secondi
                .build());

        // Creazione client che pubblica soltanto, con le relative impostazioni di connessione
        MQTTClient publishClient = new MQTTClient(new ConnectionSettingsBuilder()
                .setClientId(InetAddress.getLocalHost().getHostName() + "_pub") // client id impostato ad hostname
                .setHostname("localhost")
                .setPort(1883)
                .setCleanSession(true) // sessione eliminata al momento della disconnessione
                .setKeepAliveSeconds(20) // keep alive di 20 secondi
                .build());

        // connessione al broker
        boolean subscribeConnected = subscribeClient.connect();
        boolean publishConnected = publishClient.connect();

        if(subscribeConnected) {
            // sotto iscrizione al topic meteo, con qos 2
            subscribeClient.subscribe("/meteo/urbino",  Qos.QOS_2);
            // sotto iscrizione al meteo di tutte le frazioni di urbino con qos = 1
            subscribeClient.subscribe("/meteo/urbino/#", Qos.QOS_1);
            // sotto iscrizione a ricevere la temperatura da tutti i sensori (+) con qos 0
            subscribeClient.subscribe("/sensor/+/temperature", Qos.QOS_0);
        } else {
            CustomLogger.LOGGER.info("Client Subscribe connection error");
        }

        if(publishConnected) {
            // il client publisher invia messaggi al broker
            // invia il meteo di urbino, con qos = 0
            publishClient.publish(new Message("/meteo/urbino", "soleggiato - 25.0", Qos.QOS_0, false));
            // invia il meteo ad una frazione di urbino
            publishClient.publish(new Message("/meteo/urbino/canavaccio", "soleggiato - 21.0", Qos.QOS_0, false));
            // invia la temperatura letta da arduino, con qos = 1, ovvero il broker dovrà inviare un ack di conferma
            publishClient.publish(new Message("/sensor/arduino/temperature", "35.0", Qos.QOS_1, false));
            // invia la temperatura letta da arduino, con qos = 2, ovvero il broker e il cliente scambierrano una serie di ack.
            publishClient.publish(new Message("/sensor/raspberry/temperature", "35.0", Qos.QOS_2, false));

        } else {
            CustomLogger.LOGGER.info("Client Publish connection error");
        }
    }
}
