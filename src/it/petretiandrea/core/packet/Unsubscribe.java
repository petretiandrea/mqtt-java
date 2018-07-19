package it.petretiandrea.core.packet;

import it.petretiandrea.common.Qos;
import it.petretiandrea.core.Utils;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static it.petretiandrea.core.Utils.*;

public class Unsubscribe extends MQTTPacket {

    private int mMessageID;
    private String mTopic;

    public Unsubscribe(String topic) {
        this(new Random().nextInt(), topic);
    }

    public Unsubscribe(int messageID, String topic) {
        super(MQTTPacket.Type.UNSUBSCRIBE, false, Qos.QOS_1, false);
        mMessageID = messageID;
        mTopic = topic;
    }

    public Unsubscribe(byte[] packet) throws Exception {
        super(packet);

        int offset = (Utils.getRemainingLength(packet) > 127) ? 3 : 2;
        mMessageID = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]);

        int topicLength = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]);
        mTopic = new String(packet, offset, topicLength, "UTF-8");
    }

    public int getMessageID() {
        return mMessageID;
    }

    public String getTopic() {
        return mTopic;
    }

    @Override
    public byte[] toByte() throws UnsupportedEncodingException {
        List<Byte> variableAndPayload = new ArrayList<>();
        variableAndPayload.add((byte) (getMessageID() >> 8));
        variableAndPayload.add((byte) (getMessageID() & 0xFF));
        AppendString(variableAndPayload, getTopic());
        return Join(
                GenerateFixedHeader(getCommand(), variableAndPayload.size(), isDup(), getQos().ordinal(), isRetain()),
                ToPrimitive(variableAndPayload)
        );
    }
}
