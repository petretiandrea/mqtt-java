package it.petretiandrea.common.network;

import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.IOException;
import java.net.SocketAddress;

public interface Transport {

    void connect(String hostname, int port) throws IOException;

    MQTTPacket readPacket(int timeout) throws IOException, MQTTParseException;
    MQTTPacket readPacket() throws IOException, MQTTParseException;

    void writePacket(MQTTPacket packet, int timeout) throws IOException;
    void writePacket(MQTTPacket packet) throws IOException;

    void close() throws IOException;

    boolean isConnected();
}
