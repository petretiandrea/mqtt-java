package it.petretiandrea.core.exception;

public class MQTTProtocolException extends Exception {
    public MQTTProtocolException() {
    }

    public MQTTProtocolException(String message) {
        super(message);
    }

    public MQTTProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public MQTTProtocolException(Throwable cause) {
        super(cause);
    }

    public MQTTProtocolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
