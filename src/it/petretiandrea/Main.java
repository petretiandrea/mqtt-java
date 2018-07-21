package it.petretiandrea;

import it.petretiandrea.client.MQTTClient;
import it.petretiandrea.core.*;
import it.petretiandrea.core.packet.ConnAck;
import it.petretiandrea.core.packet.Connect;
import it.petretiandrea.core.packet.SubAck;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.core.packet.Subscribe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

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


        MQTTClient client = new MQTTClient(settings);
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
        client.subscribe("provatopic", Qos.QOS_0);

        System.in.read();

        System.out.println("Disconnetion: " + client.disconnet());

        /*

        ServerSocket socketServer = new ServerSocket(1883);

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("192.168.1.105", 1883));

        //Socket socket = socketServer.accept();

        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                byte[] b = new byte[1024];
                int readLen = 0;
                while ((readLen = socket.getInputStream().read(b)) > 0) {
                    MQTTPacket packet = MQTTPacket.parse(b);
                    System.out.println("From server: " + packet);
                    if(packet.getCommand() == MQTTPacket.Type.CONNECT)
                        socket.getOutputStream().write(new ConnAck(false, ConnectionStatus.ACCEPT).toByte());
                    else if(packet.getCommand() == MQTTPacket.Type.SUBSCRIBE) {
                        Subscribe sub = (Subscribe) packet;
                        socket.getOutputStream().write(new SubAck(sub.getMessageID(), sub.getQosSub()).toByte());
                    }
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println(socket.isConnected());
        System.out.println("Sending...");
        socket.getOutputStream().write(new Connect(MQTTVersion.MQTT_311, settings).toByte());
        System.out.println("Writed...");

        Thread.sleep(3000);
        socket.getOutputStream().write(new Subscribe("provatopic", Qos.QOS_1).toByte());*/
        System.in.read();
    }
}
