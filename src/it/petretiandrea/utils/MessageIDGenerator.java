package it.petretiandrea.utils;

public class MessageIDGenerator {

    private static final int MAX_MESSAGE_ID = 65535;
    private int mLastMessageID;

    private static MessageIDGenerator mInstance;

    public static MessageIDGenerator getInstance() {
        if(mInstance == null)
            mInstance = new MessageIDGenerator();
        return mInstance;
    }

    private MessageIDGenerator() {
        mLastMessageID = 0;
    }

    public int nextMessageID() {
        mLastMessageID = (mLastMessageID + 1 > MAX_MESSAGE_ID) ? 0 : mLastMessageID + 1;
        return mLastMessageID;
    }

}
