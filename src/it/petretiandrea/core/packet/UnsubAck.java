package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.UnsupportedEncodingException;

import static it.petretiandrea.utils.Utils.GenerateFixedHeader;
import static it.petretiandrea.utils.Utils.Join;

public class UnsubAck extends ACK {

    public UnsubAck(int messageID) {
        super(MQTTPacket.Type.UNSUBACK, false, Qos.QOS_0, false, messageID);
    }

    public UnsubAck(byte fixedHeader, byte[] body) throws MQTTParseException {
        super(fixedHeader, body);
    }

    @Override
    public byte[] toByte() throws UnsupportedEncodingException {
        byte[] variableAndPayload = new byte[2];
        variableAndPayload[0] = ((byte) (getMessageID() >> 8));
        variableAndPayload[1] = ((byte) (getMessageID() & 0xFF));
        return Join(
                GenerateFixedHeader(getCommand(), 2, isDup(), getQos().ordinal(), isRetain()),
                variableAndPayload
        );
    }
}
