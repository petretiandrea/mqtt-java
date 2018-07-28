package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.UnsupportedEncodingException;

import static it.petretiandrea.utils.Utils.GenerateFixedHeader;

public class PingReq extends MQTTPacket {

    public PingReq() {
        super(MQTTPacket.Type.PINGREQ, false, Qos.QOS_0, false);
    }

    public PingReq(byte fixedHeader, byte[] body) throws MQTTParseException {
        super(fixedHeader);
    }

    @Override
    public byte[] toByte() throws UnsupportedEncodingException {
        return GenerateFixedHeader(getCommand(), 0, isDup(), Qos.QOS_0.ordinal(), isRetain());
    }
}
