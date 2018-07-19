package it.petretiandrea.core.packet;

import it.petretiandrea.core.Message;
import it.petretiandrea.core.Qos;
import it.petretiandrea.core.Utils;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static it.petretiandrea.core.Utils.*;

public class Publish extends MQTTPacket {

    private String mTopic;
    private String mMessageContent;
    private int mMessageID;

    // TODO: Method for transform a publish packet, into Message.

    public Publish(Message message) {
        super(MQTTPacket.Type.PUBLISH, message.isRetain(), message.getQos(), message.isDup());
        mTopic = message.getTopic();
        mMessageContent = message.getMessage();
        mMessageID = message.getMessageID();
    }

    public Publish(byte[] packet) throws Exception {
        super(packet);
        // offset iniziale in base alla lunghezza, ovvero se la lunghezza rimananente è maggiore
        int remainingLength = Utils.getRemainingLength(packet);
        int offset = (remainingLength > 127) ? 3 : 2;

        int topicLen = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]); // posizione dopo la RemainingLength, inizia MSB e poi LSB.

        mTopic = new String(packet, offset, topicLen, CHARSET); // parse string topic id

        offset += topicLen;

        // message ID
        if(getQos().ordinal() > Qos.QOS_0.ordinal()) {
            // qos 1 or qos 2, retrive message id.
            mMessageID = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]);
        } else mMessageID = 0;

        // message content
        mMessageContent = new String(packet, offset, remainingLength - offset + 2 /*2 Fixed header length*/, CHARSET);
    }

    public Message getMessage() {
        return new Message(getMessageID(), getTopic(), getMessageContent(), getQos(), isRetain(), isDup());
    }

    private String getTopic() {
        return mTopic;
    }

    private String getMessageContent() {
        return mMessageContent;
    }

    private int getMessageID() {
        return mMessageID;
    }

    @Override
    public byte[] toByte() throws UnsupportedEncodingException {
        List<Byte> variableAndPayload = new ArrayList<>();
        // append the topic
        AppendString(variableAndPayload, getTopic());
        // for qos 1 and 2 is need the message id
        if(getQos().ordinal() > Qos.QOS_0.ordinal()) {
            variableAndPayload.add((byte) (getMessageID() >> 8));
            variableAndPayload.add((byte) (getMessageID() & 0xFF));
        }
        // append the content of message.
        AppendBytes(variableAndPayload, getMessageContent().getBytes(CHARSET));
        return Join(
                GenerateFixedHeader(getCommand(), variableAndPayload.size(), isDup(), getQos().ordinal(), isRetain()),
                ToPrimitive(variableAndPayload)
        );
    }



    @Override
    public String toString() {
        return "Publish{" +
                "mTopic='" + mTopic + '\'' +
                ", mMessageContent='" + mMessageContent + '\'' +
                ", mMessageID=" + mMessageID +
                "} " + super.toString();
    }
}
