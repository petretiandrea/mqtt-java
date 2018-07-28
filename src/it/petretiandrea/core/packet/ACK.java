package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.utils.Utils;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.base.MQTTPacket;

public abstract class ACK extends MQTTPacket {

    private int mMessageID;

    public ACK(Type command, boolean retain, Qos qos, boolean dup, int messageID) {
        super(command, retain, qos, dup);
        mMessageID = messageID;
    }

    public ACK(byte fixedHeader, byte[] body) throws MQTTParseException {
        super(fixedHeader);
        mMessageID = Utils.getIntFromMSBLSB(body[0], body[1]);
    }

    public int getMessageID() {
        return mMessageID;
    }

    @Override
    public String toString() {
        return "ACK{" +
                "mMessageID=" + mMessageID +
                "} " + super.toString();
    }
}
