package it.petretiandrea.client;

import it.petretiandrea.common.MQTTClientCallback;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.ConnectionSettingsBuilder;
import it.petretiandrea.core.Message;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.Subscribe;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;

public class Test implements MQTTClientCallback {


    public static void main(String[] args) throws MQTTProtocolException, MQTTParseException, IOException {

        ConnectionSettings settings = new ConnectionSettingsBuilder()
                .setClientId(InetAddress.getLocalHost().getHostName()) // default is the hostname
                .setHostname("localhost")
                .setPort(1883)
                .setCleanSession(true)
                .setKeepAliveSeconds(15)
                .build();

        MQTTClient mqttClient = new MQTTClient(settings, new Test());

        System.out.println("Connection...");
        mqttClient.connect();
        System.out.println("Connected!");



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
    public void onConnectionLost(Throwable throwable) {

    }

    @Override
    public void onMessageArrived(Message message) {

    }

    @Override
    public void onDeliveryComplete(int messageId) {

    }

    @Override
    public void onSubscribe(Subscribe subscribe) {

    }
}
