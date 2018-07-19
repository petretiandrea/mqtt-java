package it.petretiandrea.core.packet;

import it.petretiandrea.common.Qos;

import java.io.UnsupportedEncodingException;

import static it.petretiandrea.core.Utils.GenerateFixedHeader;
import static it.petretiandrea.core.Utils.Join;

public class PubRel extends ACK {

    public PubRel(int messageID) {
        super(Type.PUBREL, false, Qos.QOS_0, false, messageID);
    }

    public PubRel(byte[] packet) {
        super(packet);
    }

    @Override
    public byte[] toByte() throws UnsupportedEncodingException {
        byte[] variableHeader = new byte[2];
        variableHeader[0] = (byte) (getMessageID() >> 8);
        variableHeader[1] = (byte) (getMessageID() & 0xFF);
        return Join(
                // the 3,2,1,0 bit need to be set to 0 0 1 0. From RFC.
                GenerateFixedHeader(getCommand(), 2, isDup(), Qos.QOS_1.ordinal(), isRetain()),
                variableHeader
        );
    }
}
