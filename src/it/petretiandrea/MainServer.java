package it.petretiandrea;

import it.petretiandrea.client.MQTTClient;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.ConnectionSettingsBuilder;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.Qos;
import it.petretiandrea.server.MQTTServer;

import java.io.IOException;

public class MainServer {
    public static void main(String[] args) throws IOException {

        MQTTServer server = new MQTTServer();

        server.listen();

        System.in.read();

        ConnectionSettings settings = new ConnectionSettingsBuilder()
                .setHostname("localhost")
                .setPort(1883)
                .setClientId("Bellooo")
                .setKeepAliveSeconds(10)
                .setCleanSession(true)
                .setWillMessage(new Message("topicWill", "ciaoo", Qos.QOS_2, true))
                .build();

        System.out.println("Connecting...");
        MQTTClient client = new MQTTClient(settings);
        try {
            System.out.println(client.connect());
            client.subscribe("topicbello", Qos.QOS_1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.in.read();
        System.out.println("Disconnect");
        client.disconnect();
        System.in.read();
        server.shutdownServer().join();
    }
}
