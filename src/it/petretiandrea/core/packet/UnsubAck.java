package it.petretiandrea.core.packet;

import it.petretiandrea.common.Qos;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.UnsupportedEncodingException;

import static it.petretiandrea.core.Utils.GenerateFixedHeader;
import static it.petretiandrea.core.Utils.Join;

public class UnsubAck extends ACK {

    public UnsubAck(int messageID) {
        super(MQTTPacket.Type.UNSUBACK, false, Qos.QOS_0, false, messageID);
    }

    public UnsubAck(byte[] packet) {
        super(packet);
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
