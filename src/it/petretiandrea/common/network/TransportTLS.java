package it.petretiandrea.common.network;

import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.IOException;
import java.net.SocketAddress;

public class TransportTLS implements Transport {


    @Override
    public void connect(SocketAddress socketAddress) {

    }

    @Override
    public MQTTPacket readPacket(int timeout) throws IOException, MQTTParseException {
        return null;
    }

    @Override
    public MQTTPacket readPacket() throws IOException, MQTTParseException {
        return null;
    }

    @Override
    public void writePacket(MQTTPacket packet, int timeout) throws IOException {

    }

    @Override
    public void writePacket(MQTTPacket packet) throws IOException {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
