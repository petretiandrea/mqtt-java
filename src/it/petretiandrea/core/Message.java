package it.petretiandrea.core;

import it.petretiandrea.utils.MessageIDGenerator;

public class Message {

    private int mMessageID;
    private String mTopic;
    private String mMessage;
    private Qos mQos;
    private boolean mRetain;
    private boolean mDup;

    public Message(String topic, String message, Qos qos, boolean retain) {
        mRetain = retain;
        mTopic = topic;
        mMessage = message;
        mQos = qos;
        // for qos 1 and 2 the message id need to be set
        if(qos.ordinal() > Qos.QOS_0.ordinal())
            mMessageID = MessageIDGenerator.getInstance().nextMessageID();
        else
            mMessageID = 0; // for qos0 the message id is 0.
    }

    public Message(String topic, String message, Qos qos, boolean retain, int messageID) {
        mRetain = retain;
        mTopic = topic;
        mMessage = message;
        mQos = qos;
        if(qos.ordinal() > Qos.QOS_0.ordinal())
            mMessageID = MessageIDGenerator.getInstance().nextMessageID();
        else
            mMessageID = 0; // for qos0 the message id is 0.
    }

    public Message(int messageID, String topic, String message, Qos qos, boolean retain, boolean dup) {
        mMessageID = messageID;
        mTopic = topic;
        mMessage = message;
        mQos = qos;
        mRetain = retain;
        mDup = dup;
    }

    public int getMessageID() {
        return mMessageID;
    }

    public String getTopic() {
        return mTopic;
    }

    public String getMessage() {
        return mMessage;
    }

    public Qos getQos() {
        return mQos;
    }

    public boolean isRetain() {
        return mRetain;
    }

    public void setQos(Qos qos) {
        mQos = qos;
    }

    public boolean isDup() {
        return mDup;
    }

    @Override
    public String toString() {
        return "Message{" +
                "mMessageID=" + mMessageID +
                ", mTopic='" + mTopic + '\'' +
                ", mMessage='" + mMessage + '\'' +
                ", mQos=" + mQos +
                ", mRetain=" + mRetain +
                '}';
    }
}
