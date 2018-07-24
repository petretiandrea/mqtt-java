package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.Utils;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.core.exception.MQTTParseException;

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
        this(new Random().nextInt(65535), topic, qosSub);
    }

    public Subscribe(int messageID, String topic, Qos qosSub) {
        super(MQTTPacket.Type.SUBSCRIBE, false, Qos.QOS_1, false);
        mMessageID = messageID > 65535 ? new Random().nextInt(65535) : messageID;
        mTopic = topic;
        mQosSub = qosSub;
    }

    public Subscribe(byte fixedHeader, byte[] body) throws MQTTParseException, UnsupportedEncodingException {
        super(fixedHeader);
        int offset = 0;
        mMessageID = Utils.getIntFromMSBLSB(body[offset++], body[offset++]);

        int topicLength = Utils.getIntFromMSBLSB(body[offset++], body[offset++]);
        mTopic = new String(body, offset, topicLength, "UTF-8");
        offset += topicLength;

        mQosSub = Qos.fromInteger(body[offset] & 0x03);
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
