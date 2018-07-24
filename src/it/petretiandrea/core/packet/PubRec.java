package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;

import java.io.UnsupportedEncodingException;

import static it.petretiandrea.core.Utils.GenerateFixedHeader;
import static it.petretiandrea.core.Utils.Join;

public class PubRec extends ACK {

    public PubRec(int messageID) {
        super(Type.PUBREC, false, Qos.QOS_0, false, messageID);
    }

    public PubRec(byte fixedHedaer, byte[] body) throws MQTTParseException {
        super(fixedHedaer, body);
    }

    @Override
    public byte[] toByte() throws UnsupportedEncodingException {
        byte[] variableHeader = new byte[2];
        variableHeader[0] = (byte) (getMessageID() >> 8);
        variableHeader[1] = (byte) (getMessageID() & 0xFF);
        return Join(
                GenerateFixedHeader(getCommand(), 2, isDup(), getQos().ordinal(), isRetain()),
                variableHeader
        );
    }

}
