package it.petretiandrea.core;

import java.util.EnumSet;

public enum  ConnectionStatus {

    ACCEPT(0),
    REFUSED_UNACCEPTABLE_PROTOCOL_VERSION(1),
    REFUSED_IDENTIFIER_REJECTED(2),
    REFUSED_SERVER_UNAVAILABLE(3),
    REFUSED_BAD_LOGIN(4),
    REFUSED_NOT_AUTHORIZED(5);

    private int mVal;

    public int Value() { return mVal; }

    private ConnectionStatus(int val) { mVal = val; }

    public static ConnectionStatus fromInteger(int value) {
        return EnumSet.allOf(ConnectionStatus.class).stream().filter(type -> type.Value() == value).findFirst().orElse(null);
    }
}
