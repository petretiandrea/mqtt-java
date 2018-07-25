package it.petretiandrea.core;

import it.petretiandrea.core.exception.MQTTParseException;

import java.util.EnumSet;
import java.util.function.Supplier;

public enum Qos {
    QOS_0,
    QOS_1,
    QOS_2;

    public static Qos fromInteger(int value) throws MQTTParseException {
        return EnumSet
                .allOf(Qos.class)
                .stream()
                .filter(type -> type.ordinal() == value)
                .findFirst()
                .orElseThrow(() -> new MQTTParseException("Invalid QOS", MQTTParseException.Reason.INVALID_QOS));
    }

    public static Qos min(Qos qos1, Qos qos2) {
        try {
            return fromInteger(Math.min(qos1.ordinal(), qos2.ordinal()));
        } catch (MQTTParseException e) {
            e.printStackTrace();
        }
        return QOS_0;
    }
}
