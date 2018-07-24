package it.petretiandrea.core.packet.base;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.Utils;

import javax.rmi.CORBA.Util;
import java.io.UnsupportedEncodingException;
import java.util.EnumSet;
import java.util.function.Supplier;

public abstract class MQTTPacket {

    public enum  Type {
        CONNECT(1),
        CONNACK(2),
        PUBLISH(3),
        PUBACK(4),
        PUBREC(5),
        PUBREL(6),
        PUBCOMP(7),
        SUBSCRIBE(8),
        SUBACK(9),
        UNSUBSCRIBE(10),
        UNSUBACK(11),
        PINGREQ(12),
        PINGRESP(13),
        DISCONNECT(14);

        private int mVal;
        public int Value() { return mVal; }
        Type(int val) {mVal = val;}

        public static Type fromInteger(int value) throws MQTTParseException {
            return EnumSet.allOf(Type.class).stream()
                    .filter(type -> type.Value() == value)
                    .findFirst()
                    .orElseThrow(() -> new MQTTParseException("Type of MQTT Packet not recognized!", MQTTParseException.Reason.INVALID_MQTT_PACKET));
        }
    }

    private Type mCommand;
    private boolean mRetain;
    private Qos mQos;
    private boolean mDup;

    public MQTTPacket(Type command, boolean retain, Qos qos, boolean dup) {
        mCommand = command;
        mRetain = retain;
        mQos = qos;
        mDup = dup;
    }

    public MQTTPacket(byte fixedHeader) throws MQTTParseException {
        mCommand = Type.fromInteger((fixedHeader & 0xF0) >> 4);
        mQos = Qos.fromInteger((fixedHeader & 0x06) >> 1);
        mDup = (fixedHeader & 0x08 >> 3) == 1;
        mRetain = (fixedHeader & 0x01) == 1;
    }

    public abstract byte[] toByte() throws UnsupportedEncodingException;

    public Type getCommand() {
        return mCommand;
    }

    public boolean isRetain() {
        return mRetain;
    }

    public Qos getQos() {
        return mQos;
    }

    public boolean isDup() {
        return mDup;
    }

    public static MQTTPacket parseBody(byte fixedHeader, byte[] body) throws MQTTParseException, UnsupportedEncodingException {
        switch (Utils.getType(fixedHeader)) {
            case CONNECT:
                return new Connect(fixedHeader, body);
            case CONNACK:
                return new ConnAck(fixedHeader, body);
            case PUBACK:
                return new PubAck(fixedHeader, body);
            case PUBREL:
                return new PubRel(fixedHeader, body);
            case PUBREC:
                return new PubRec(fixedHeader, body);
            case PUBCOMP:
                return new PubComp(fixedHeader, body);
            case PUBLISH:
                return new Publish(fixedHeader, body);
            case SUBSCRIBE:
                return new Subscribe(fixedHeader, body);
            case SUBACK:
                return new SubAck(fixedHeader, body);
            case PINGRESP:
                return new PingResp(fixedHeader, body);
            case PINGREQ:
                return new PingReq(fixedHeader, body);
            case DISCONNECT:
                return new Disconnect(fixedHeader, body);
            case UNSUBACK:
                return new UnsubAck(fixedHeader, body);
            case UNSUBSCRIBE:
                return new Unsubscribe(fixedHeader, body);
            default:
                throw new MQTTParseException("Invalid MQTT Packet type!", MQTTParseException.Reason.INVALID_MQTT_PACKET);
        }
    }

    @Override
    public String toString() {
        return "MQTTPacket{" +
                "mCommand=" + mCommand +
                ", mRetain=" + mRetain +
                ", mQos=" + mQos +
                ", mDup=" + mDup +
                '}';
    }
}
