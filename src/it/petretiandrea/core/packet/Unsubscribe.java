package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.utils.MessageIDGenerator;
import it.petretiandrea.utils.Utils;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.core.exception.MQTTParseException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static it.petretiandrea.utils.Utils.*;

public class Unsubscribe extends MQTTPacket {

    private int mMessageID;
    private String mTopic;

    public Unsubscribe(String topic) {
        super(MQTTPacket.Type.UNSUBSCRIBE, false, Qos.QOS_1, false);
        mMessageID = MessageIDGenerator.getInstance().nextMessageID();
        mTopic = topic;
    }


    public Unsubscribe(byte fixedHeader, byte[] body) throws MQTTParseException, UnsupportedEncodingException {
        super(fixedHeader);

        int offset = 0;
        mMessageID = Utils.getIntFromMSBLSB(body[offset++], body[offset++]);

        int topicLength = Utils.getIntFromMSBLSB(body[offset++], body[offset++]);
        mTopic = new String(body, offset, topicLength, "UTF-8");
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
