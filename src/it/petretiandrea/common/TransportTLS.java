package it.petretiandrea.common;

import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.IOException;

public class TransportTLS implements Transport {


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
}
