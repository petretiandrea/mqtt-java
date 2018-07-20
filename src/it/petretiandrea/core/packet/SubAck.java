package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.Utils;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.core.exception.MQTTParseException;

import static it.petretiandrea.core.Utils.GenerateFixedHeader;
import static it.petretiandrea.core.Utils.Join;

public class SubAck extends MQTTPacket {

    private int mMessageID;
    private Qos mGrantedQos;

    public SubAck(int messageID, Qos grantedQos) {
        super(MQTTPacket.Type.SUBACK, false, Qos.QOS_0, false);
        mMessageID = messageID;
        mGrantedQos = grantedQos;
    }

    public SubAck(byte[] packet) throws MQTTParseException {
        super(packet);
        int offset = (Utils.getRemainingLength(packet) > 127) ? 3 : 2;
        mMessageID = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]);
        int grantedQos = (packet[offset] & 0xFF);
        if(grantedQos != 0x80)
            mGrantedQos = Qos.fromInteger(grantedQos);
        else throw new MQTTParseException("Granted Qos Failure");
    }

    @Override
    public byte[] toByte() {
        byte[] variableAndPayload = new byte[3];
        variableAndPayload[0] = ((byte) (getMessageID() >> 8));
        variableAndPayload[1] = ((byte) (getMessageID() & 0xFF));
        variableAndPayload[2] = ((byte) ((getGrantedQos() == Qos.QOS_0) ? 0x00 : (getGrantedQos() == Qos.QOS_1) ? 0x01 : 0x02));
        return Join(
                GenerateFixedHeader(getCommand(), 3, isDup(), getQos().ordinal(), isRetain()),
                variableAndPayload
        );
    }

    public int getMessageID() {
        return mMessageID;
    }

    public Qos getGrantedQos() {
        return mGrantedQos;
    }

    @Override
    public String toString() {
        return "SubAck{" +
                "mMessageID=" + mMessageID +
                ", mGrantedQos=" + mGrantedQos +
                "} " + super.toString();
    }
}
