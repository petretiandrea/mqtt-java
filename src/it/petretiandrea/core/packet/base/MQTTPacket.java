package it.petretiandrea.core.packet.base;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.exception.MQTTParseException;

import java.io.UnsupportedEncodingException;
import java.util.EnumSet;

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

        public static Type fromInteger(int value) {
            return EnumSet.allOf(Type.class).stream().filter(type -> type.Value() == value).findFirst().orElse(null);
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

    public MQTTPacket(byte[] packet) {
        mCommand = Type.fromInteger((packet[0] & 0xF0) >> 4);
        mQos = Qos.fromInteger((packet[0] & 0x06) >> 1);
        mDup = (packet[0] & 0x08 >> 3) == 1;
        mRetain = (packet[0] & 0x01) == 1;
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

    private static Type getType(byte[] packet) { return Type.fromInteger((packet[0] & 0xF0) >> 4); }

    public static MQTTPacket parse(byte[] packet) throws MQTTParseException, UnsupportedEncodingException {
        switch (getType(packet)) {
            case CONNECT:
                return new Connect(packet);
            case CONNACK:
                return new ConnAck(packet);
            case PUBACK:
                return new PubAck(packet);
            case PUBREL:
                return new PubRel(packet);
            case PUBREC:
                return new PubRec(packet);
            case PUBCOMP:
                return new PubComp(packet);
            case PUBLISH:
                return new Publish(packet);
            case SUBSCRIBE:
                return new Subscribe(packet);
            case SUBACK:
                return new SubAck(packet);
            case PINGRESP:
                return new PingResp(packet);
            case PINGREQ:
                return new PingReq(packet);
            case DISCONNECT:
                return new Disconnect(packet);
            case UNSUBACK:
                return new UnsubAck(packet);
            case UNSUBSCRIBE:
                return new Unsubscribe(packet);
            default:
                throw new MQTTParseException("Invalid MQTT Packet type!");
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
