package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.UnsupportedEncodingException;

import static it.petretiandrea.utils.Utils.GenerateFixedHeader;

public class Disconnect extends MQTTPacket {

    public Disconnect() {
        super(Type.DISCONNECT, false, Qos.QOS_0, false);
    }

    public Disconnect(byte fixedHeader, byte[] body) throws MQTTParseException {
        super(fixedHeader);
    }

    @Override
    public byte[] toByte() throws UnsupportedEncodingException {
        return GenerateFixedHeader(getCommand(), 0, isDup(), getQos().ordinal(), isRetain());
    }
}
