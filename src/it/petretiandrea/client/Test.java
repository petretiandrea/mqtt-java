package it.petretiandrea.client;

import it.petretiandrea.common.Client;
import it.petretiandrea.common.MQTTClient;
import it.petretiandrea.common.MQTTClientCallback;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.ConnectionSettingsBuilder;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.Subscribe;
import it.petretiandrea.core.packet.Unsubscribe;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;

public class Test implements MQTTClientCallback {


    public static void main(String[] args) throws MQTTProtocolException, MQTTParseException, IOException {

        ConnectionSettings settings = new ConnectionSettingsBuilder()
                .setClientId(InetAddress.getLocalHost().getHostName()) // default is the hostname
                .setHostname("192.168.1.105")
                .setPort(1883)
                .setCleanSession(true)
                .setKeepAliveSeconds(200)
                .build();

        MQTTClient mqttClient = new MQTTClient(settings);
        mqttClient.setClientCallback(new Test());

        System.out.println("Connection...");
        mqttClient.connect();
        System.out.println("Connected!");

        System.in.read();

        mqttClient.publish(new Message("provatopic", "ciaoo", Qos.QOS_2, false));

        mqttClient.subscribe("provatopic", Qos.QOS_0);

        System.in.read();

        mqttClient.disconnect();

        /*ConnectionSettings settings = new ConnectionSettingsBuilder()
                .setHostname("192.168.1.105")
                .setPort(1883)
                .setCleanSession(true)
                .setKeepAliveSeconds(10)
                .build();

        MQTTClient client = new MQTTClient(settings, new Test());

        client.connect();*/


    }

    @Override
    public void onMessageArrived(Message message) {
        System.out.println("Test.onMessageArrived");
        System.out.println("message = [" + message + "]");
    }

    @Override
    public void onDeliveryComplete(int messageId) {
        System.out.println("Test.onDeliveryComplete");
        System.out.println("messageId = [" + messageId + "]");
    }

    @Override
    public void onConnectionLost(Client client, Throwable ex) {
        System.out.println("Test.onConnectionLost");
        System.out.println("client = [" + client + "], ex = [" + ex + "]");
    }

    @Override
    public void onDisconnect(Client client) {
        System.out.println("Test.onDisconnect");
        System.out.println("client = [" + client + "]");
    }

    @Override
    public void onSubscribeComplete(Subscribe subscribe) {
        System.out.println("Test.onSubscribeComplete");
        System.out.println("subscribe = [" + subscribe + "]");
    }

    @Override
    public void onUnsubscribeComplete(Unsubscribe unsubscribe) {
        System.out.println("Test.onUnsubscribeComplete");
        System.out.println("unsubscribe = [" + unsubscribe + "]");
    }
}
