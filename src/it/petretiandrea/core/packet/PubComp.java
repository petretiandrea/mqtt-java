package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;

import java.io.UnsupportedEncodingException;

import static it.petretiandrea.utils.Utils.GenerateFixedHeader;
import static it.petretiandrea.utils.Utils.Join;

public class PubComp extends ACK {

    public PubComp(int messageID) {
        super(Type.PUBCOMP, false, Qos.QOS_0, false, messageID);
    }

    public PubComp(byte fixedHeader, byte[] body) throws MQTTParseException {
        super(fixedHeader, body);
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
