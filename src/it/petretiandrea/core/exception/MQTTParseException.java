package it.petretiandrea.core.exception;

public class MQTTParseException extends Exception {

    public MQTTParseException() {
    }

    public MQTTParseException(String message) {
        super(message);
    }

    public MQTTParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public MQTTParseException(Throwable cause) {
        super(cause);
    }

    public MQTTParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
