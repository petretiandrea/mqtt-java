package it.petretiandrea.common;

import java.util.EnumSet;

public enum Qos {
    QOS_0,
    QOS_1,
    QOS_2;

    public static Qos fromInteger(int value) {
        return EnumSet.allOf(Qos.class).stream().filter(type -> type.ordinal() == value).findFirst().orElse(null);
    }
}
