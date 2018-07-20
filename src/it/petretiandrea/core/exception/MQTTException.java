package it.petretiandrea.core.exception;

public class MQTTException extends Exception {
    public MQTTException() {
    }

    public MQTTException(String message) {
        super(message);
    }

    public MQTTException(String message, Throwable cause) {
        super(message, cause);
    }

    public MQTTException(Throwable cause) {
        super(cause);
    }

    public MQTTException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
