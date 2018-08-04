package it.petretiandrea.core.exception;

public class MQTTParseException extends Exception {

    public static enum Reason {
        INVALID_MQTT_NAME_LEVEL,
        INVALID_WILL,
        NO_USERNAME,
        NO_PASSWORD,
        INVALID_CLIENT_ID,
        INVALID_QOS,
        INVALID_MQTT_PACKET,
        INVALID_TOPIC
    }

    private Reason mReason;

    public MQTTParseException(Reason reason) {
        mReason = reason;
    }

    public MQTTParseException(String message, Reason reason) {
        super(message);
        mReason = reason;
    }

    public MQTTParseException(String message, Throwable cause, Reason reason) {
        super(message, cause);
        mReason = reason;
    }

    public MQTTParseException(Throwable cause, Reason reason) {
        super(cause);
        mReason = reason;
    }

    public MQTTParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Reason reason) {
        super(message, cause, enableSuppression, writableStackTrace);
        mReason = reason;
    }

    public Reason getReason() {
        return mReason;
    }
}
