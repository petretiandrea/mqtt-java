package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.utils.Utils;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.core.exception.MQTTParseException;

import static it.petretiandrea.utils.Utils.GenerateFixedHeader;
import static it.petretiandrea.utils.Utils.Join;

public class SubAck extends MQTTPacket {

    private int mMessageID;
    private Qos mGrantedQos;
    private boolean mIsFailure;

    public SubAck(int messageID, Qos grantedQos) {
        super(MQTTPacket.Type.SUBACK, false, Qos.QOS_0, false);
        mMessageID = messageID;
        mGrantedQos = grantedQos;
        mIsFailure = false;
    }

    public SubAck(int messageID, Qos grantedQos, boolean isFailure) {
        super(MQTTPacket.Type.SUBACK, false, Qos.QOS_0, false);
        mMessageID = messageID;
        mGrantedQos = grantedQos;
        mIsFailure = isFailure;
    }

    public SubAck(byte fixedHedaer, byte[] body) throws MQTTParseException {
        super(fixedHedaer);
        int offset = 0;
        mMessageID = Utils.getIntFromMSBLSB(body[offset++], body[offset++]);
        int grantedQos = (body[offset] & 0xFF);
        if(grantedQos != 0x80)
            mGrantedQos = Qos.fromInteger(grantedQos);
        else
            mIsFailure = true;
    }

    @Override
    public byte[] toByte() {
        byte[] variableAndPayload = new byte[3];
        variableAndPayload[0] = ((byte) (getMessageID() >> 8));
        variableAndPayload[1] = ((byte) (getMessageID() & 0xFF));
        variableAndPayload[2] = ((byte) (isFailure() ? 0x80 : (getGrantedQos() == Qos.QOS_0) ? 0x00 : (getGrantedQos() == Qos.QOS_1) ? 0x01 : 0x02));
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

    public boolean isFailure() {
        return mIsFailure;
    }

    @Override
    public String toString() {
        return "SubAck{" +
                "mMessageID=" + mMessageID +
                ", mGrantedQos=" + mGrantedQos +
                "} " + super.toString();
    }
}
