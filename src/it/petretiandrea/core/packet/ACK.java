package it.petretiandrea.core.packet;

import it.petretiandrea.common.Qos;
import it.petretiandrea.core.Utils;
import it.petretiandrea.core.packet.base.MQTTPacket;

public abstract class ACK extends MQTTPacket {

    private int mMessageID;

    public ACK(Type command, boolean retain, Qos qos, boolean dup, int messageID) {
        super(command, retain, qos, dup);
        mMessageID = messageID;
    }

    public ACK(byte[] packet) {
        super(packet);
        mMessageID = Utils.getIntFromMSBLSB(packet[2], packet[3]);
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
