package it.petretiandrea;

import it.petretiandrea.client.Client;
import it.petretiandrea.client.MQTTClient;
import it.petretiandrea.core.*;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
	    // write your code here

        ConnectionSettings settings = new ConnectionSettingsBuilder()
                .setHostname("192.168.1.105")
                .setPort(1883)
                .setClientId("Bellooo")
                .setKeepAliveSeconds(5)
                .setCleanSession(true)
                .setWillMessage(new Message("topicWill", "ciaoo", Qos.QOS_2, true))
                .build();


        Client client = new Client(settings);
        try {
            System.out.println(client.connect());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.in.read();
        System.out.println("Publish");
        client.publish(new Message("provatopic", "caioo", Qos.QOS_2, false));

        System.in.read();
        System.out.println("Subscribe");
        client.subscribe("provatopic", Qos.QOS_2);

        System.in.read();

        System.out.println("Disconnetion: " + client.disconnect());

        System.in.read();
    }
}
