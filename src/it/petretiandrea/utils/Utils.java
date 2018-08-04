package it.petretiandrea.utils;

import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.core.exception.MQTTParseException;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;

public class Utils {

    public static final String CHARSET = "UTF-8";


    public static MQTTPacket.Type getType(byte header) throws MQTTParseException { return MQTTPacket.Type.fromInteger((header & 0xF0) >> 4); }

    public static int getRemainingLength(byte[] packet) throws MQTTParseException {
        // index start from 2nd byte, beacouse the 1st if for mqtt flags
        int index = 1;
        int multiplier = 1;
        int value = 0;
        do {
            value += (packet[index] & 127) * multiplier;
            multiplier *= 128;
            if(multiplier > 128*128*128)
                throw new MQTTParseException("Malformed Remaining Length", MQTTParseException.Reason.INVALID_MQTT_PACKET);
        } while ((packet[++index] & 128) != 0);
        return value;
    }

    public static int getIntFromMSBLSB(byte msb, byte lsb) {
        return ((msb & 0xFF) << 8) | (lsb & 0xFF);
    }

    public static byte[] GenerateFixedHeader(MQTTPacket.Type type, int remainLength, boolean dup, int qos, boolean retain) {
        byte[] b = new byte[(remainLength > 127) ? 3 : 2];
        b[0] = (byte) (((type.Value() & 0x0F) << 4) | (((dup ? 1 : 0) & 0x01) << 3) | ((qos & 0x3) << 1) | ((retain ? 1 : 0) & 0x1));
        if(remainLength > 127) {
            b[1] = (byte) ((remainLength % 128) | 0x80);
            b[2] = (byte) (remainLength / 128);
        } else
            b[1] = (byte) remainLength;

        return b;
    }

    public static byte[] Join(byte[] ar1, byte[] ar2) {
        int i = ar1.length;
        byte[] b = Arrays.copyOf(ar1, ar1.length + ar2.length);
        for(byte t : ar2) b[i++] = t;
        return b;
    }

    public static byte[] ToPrimitive(Collection<Byte> bytes) {
        int i = 0;
        byte[] b = new byte[bytes.size()];
        for (Byte b1 : bytes) b[i++] = b1;
        return b;
    }

    public static void AppendString(Collection<Byte> byteCollection, String string) throws UnsupportedEncodingException {
        byteCollection.add((byte) (string.length() >> 8));
        byteCollection.add((byte) (string.length() & 0xFF));
        AppendBytes(byteCollection, string.getBytes("UTF-8"));
    }

    public static void AppendBytes(Collection<Byte> bytesList, byte[] bytes) {
        for (byte b : bytes)
            bytesList.add(b);
    }
}
