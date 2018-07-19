package it.petretiandrea.core.packet;

import it.petretiandrea.common.Qos;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.UnsupportedEncodingException;

import static it.petretiandrea.core.Utils.GenerateFixedHeader;

public class PingResp extends MQTTPacket {

    public PingResp() {
        super(MQTTPacket.Type.PINGRESP, false, Qos.QOS_0, false);
    }

    public PingResp(byte[] packet) {
        super(packet);
    }

    @Override
    public byte[] toByte() throws UnsupportedEncodingException {
        return GenerateFixedHeader(getCommand(), 0, isDup(), getQos().ordinal(), isRetain());
    }
}
