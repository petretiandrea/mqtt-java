package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.Utils;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static it.petretiandrea.core.Utils.*;

public class Subscribe extends MQTTPacket {

    private int mMessageID;
    private String mTopic;
    private Qos mQosSub;

    public Subscribe(String topic, Qos qosSub) {
        this(new Random().nextInt(), topic, qosSub);
    }

    public Subscribe(int messageID, String topic, Qos qosSub) {
        super(MQTTPacket.Type.SUBSCRIBE, false, Qos.QOS_1, false);
        mMessageID = messageID;
        mTopic = topic;
        mQosSub = qosSub;
    }

    public Subscribe(byte[] packet) throws Exception {
        super(packet);
        int offset = (Utils.getRemainingLength(packet) > 127) ? 3 : 2;
        mMessageID = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]);

        int topicLength = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]);
        mTopic = new String(packet, offset, topicLength, "UTF-8");
        offset += topicLength;

        mQosSub = Qos.fromInteger(packet[offset] & 0x03);
    }

    @Override
    public byte[] toByte() throws UnsupportedEncodingException {
        List<Byte> variableAndPayload = new ArrayList<>();
        variableAndPayload.add((byte) (getMessageID() >> 8));
        variableAndPayload.add((byte) (getMessageID() & 0xFF));
        AppendString(variableAndPayload, getTopic());
        variableAndPayload.add((byte) getQosSub().ordinal());
        return Join(
                GenerateFixedHeader(getCommand(), variableAndPayload.size(), isDup(), getQos().ordinal(), isRetain()),
                ToPrimitive(variableAndPayload)
        );
    }
    public int getMessageID() {
        return mMessageID;
    }

    public String getTopic() {
        return mTopic;
    }

    public Qos getQosSub() {
        return mQosSub;
    }
}
