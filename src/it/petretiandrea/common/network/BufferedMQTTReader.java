package it.petretiandrea.common.network;

import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.utils.Utils;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.*;
import java.util.Arrays;

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
        if(fixedHeader != -1) // end of stream reached
        {
            Utils.getType(fixedHeader);

            int multiplier = 1;
            int length = 0;
            byte tmp;
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
        }
        return null;
    }
}
