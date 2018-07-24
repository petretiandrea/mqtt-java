package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.UnsupportedEncodingException;

import static it.petretiandrea.core.Utils.GenerateFixedHeader;
import static it.petretiandrea.core.Utils.Join;

public class PubAck extends ACK {

    public PubAck(int messageID) {
        super(MQTTPacket.Type.PUBACK, false, Qos.QOS_0, false, messageID);
    }

    public PubAck(byte fixedHeader, byte[] body) throws MQTTParseException {
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
