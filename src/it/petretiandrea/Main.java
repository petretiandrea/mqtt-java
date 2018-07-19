package it.petretiandrea;

import it.petretiandrea.core.*;
import it.petretiandrea.core.packet.ConnAck;
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
                .setClientId("Bellooo")
                .setKeepAliveSeconds(10000)
                .setCleanSession(true)
                .setWillMessage(new Message("topicWill", "ciaoo", Qos.QOS_2, true))
                .build();

        byte[] bytes = PacketBuilder.buildConnectPacket(MQTTVersion.MQTT_311, settings);

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
                        socket.getOutputStream().write(PacketBuilder.buildSubAck(sub.getMessageID(), sub.getQosSub()));
                    }
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println(socket.isConnected());
        System.out.println("Sending...");
        socket.getOutputStream().write(bytes);
        System.out.println("Writed...");

        Thread.sleep(3000);
        //byte[] pub = PacketBuilder.buildPublishPacket(new Message("provatopic", "prova", Qos.QOS_1, false), false);
        //socket.getOutputStream().write(pub);
        byte[] sub = PacketBuilder.buildSubscribe("provatopic", Qos.QOS_1);
        socket.getOutputStream().write(sub);
        System.in.read();
    }
}
