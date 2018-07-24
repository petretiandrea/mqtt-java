package it.petretiandrea.common;

import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.Utils;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.*;

public class BufferedMQTTReader {

    private BufferedInputStream mBufferedInputStream;

    public BufferedMQTTReader(InputStream in) {
        mBufferedInputStream = new BufferedInputStream(in);
    }

    public BufferedMQTTReader(InputStream in, int size) {
        mBufferedInputStream = new BufferedInputStream(in, size);
    }

    public synchronized MQTTPacket nextMQTTPacket() throws IOException, MQTTParseException {
        byte fixedHeader = (byte) mBufferedInputStream.read();

        Utils.getType(fixedHeader);

        int multiplier = 1;
        int length = 0;
        byte tmp = 0;
        do {
            tmp = (byte) mBufferedInputStream.read();
            length += (tmp & 127) * multiplier;
            multiplier *= 128;
            if(multiplier > 128*128*128)
                throw new MQTTParseException("Malformed Remaining Length", MQTTParseException.Reason.INVALID_MQTT_PACKET);
        } while ((tmp & 128) != 0);

        // read all packet with length
        byte[] packet = new byte[length];
        if(mBufferedInputStream.read(packet, 0, packet.length) >= 0) {
            return MQTTPacket.parseBody(fixedHeader, packet);
        }
        return null;
    }

    /*
    public static void main(String[] args) {
        byte[] test = {48, 17, 0, 10, 112, 114, 111, 118, 97, 116, 111, 112, 105, 99, 99, 105, 97, 111, 111, -32, 0};
        ByteArrayInputStream bytes = new ByteArrayInputStream(test);
        BufferedMQTTReader reader = new BufferedMQTTReader(bytes);

        try {
            MQTTPacket packet = reader.nextMQTTPacket();
            System.out.println(packet);
            packet = reader.nextMQTTPacket();
            System.out.println(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    } */

}
