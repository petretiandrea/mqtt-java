package it.petretiandrea.core;

import it.petretiandrea.common.ConnectionSettings;
import it.petretiandrea.common.MQTTVersion;
import it.petretiandrea.common.Message;
import it.petretiandrea.common.Qos;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class PacketBuilder {

    private static final String CHARSET = "UTF-8";
    private static final byte FLAG_CLEAN_SESSION = 2;
    private static final byte FLAG_WILL = (1 << 2);
    private static final byte FLAG_WILL_RETAIN = (1 << 5);
    private static final byte FLAG_PASSWORD = (1 << 6);
    private static final byte FLAG_USERNAME = (byte) (1 << 7);

    /**
     * Build a MQTT Connect Packet.
     * @param version The version of MQTT Protocol
     * @param settings The setting of connection.
     * @return Packet as array of byte
     * @throws UnsupportedEncodingException If the machine not support UTF-8 encoding.
     */
    public static byte[] buildConnectPacket(MQTTVersion version, ConnectionSettings settings) throws UnsupportedEncodingException {
        List<Byte> bytes = new ArrayList<>(15);

        // length MSB
        bytes.add((byte) 0);
        // length LSB
        bytes.add((version == MQTTVersion.MQTT_31) ? (byte) 6 : 4);
        // protocol name
        AppendBytes(bytes, (version == MQTTVersion.MQTT_31) ? "MQIsdp".getBytes(CHARSET) : "MQTT".getBytes(CHARSET));
        // protocol version
        bytes.add((version == MQTTVersion.MQTT_31) ? (byte) 3 : 4);

        // connection flags
        int indexConnectionFlags = bytes.size();
        bytes.add((byte) 0);

        if(settings.isCleanSession())
            bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | FLAG_CLEAN_SESSION));

        // keep alive
        // MSB and LSB
        bytes.add((byte)(settings.getKeepAliveSeconds() >> 8));
        bytes.add((byte)(settings.getKeepAliveSeconds() & 0xFF)); // mask for 8bit.

        // payload Client Identifier, Will Topic, Will Message, User Name, Password
        // 1. Client ID
        AppendString(bytes, settings.getClientId());

        // 2-3. Topic Will and Topic Message
        if(settings.getWillMessage() != null) {
            AppendString(bytes, settings.getWillMessage().getTopic());
            AppendString(bytes, settings.getWillMessage().getMessage());
            // flag message will
            bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | FLAG_WILL));
            // qos will
            bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | (settings.getWillMessage().getQos().ordinal() & 3) << 3));
            // will retain
            if(settings.getWillMessage().isRetain())
                bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | FLAG_WILL_RETAIN));
        }
        // 4. username
        if(settings.getUsername() != null && settings.getUsername().length() > 0) {
            // add connection flag
            bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | FLAG_USERNAME));
            AppendString(bytes, settings.getUsername());
        }
        // 5. Password
        if(settings.getPassword() != null && settings.getPassword().length() > 0) {
            // add connection flag
            bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | FLAG_PASSWORD));
            AppendString(bytes, settings.getPassword());
        }

        // Join fixed header to variable header and payload
        return Join(GenerateFixedHeader(MQTTPacket.Type.CONNECT, bytes.size(), false, 0, false), ToPrimitive(bytes));
    }

    public static byte[] buildConnackPacket(boolean sessionPresent, ConnectionStatus connectionStatus) {
        byte[] variableHeader = new byte[2];
        // if the connection is not setted to accept, the sessionePresent is set to 0.
        // first byte is setted if the server have the sessione for this client.
        variableHeader[0] = (connectionStatus != ConnectionStatus.ACCEPT) ? 0 : (byte) (sessionPresent ? 1 : 0);
        variableHeader[1] = (byte) connectionStatus.Value();
        return GenerateFixedHeader(MQTTPacket.Type.CONNACK, 0, false, 0, false);
    }

    public static byte[] buildPublishPacket(Message message, boolean dup) throws UnsupportedEncodingException {
        List<Byte> variableAndPayload = new ArrayList<>();
        if(message != null) {
            // append the topic
            AppendString(variableAndPayload, message.getTopic());
            // for qos 1 and 2 is need the message id
            if(message.getQos().ordinal() > Qos.QOS_0.ordinal()) {
                variableAndPayload.add((byte) (message.getMessageID() >> 8));
                variableAndPayload.add((byte) (message.getMessageID() & 0xFF));
            }
            // append the content of message.
            AppendBytes(variableAndPayload, message.getMessage().getBytes(CHARSET));
            return Join(
                    GenerateFixedHeader(MQTTPacket.Type.PUBLISH, variableAndPayload.size(), dup, message.getQos().ordinal(), message.isRetain()),
                    ToPrimitive(variableAndPayload)
            );
        }
        return new byte[]{0};
    }

    public static byte[] buildPubAck(Message message) {
        if(message.getQos() == Qos.QOS_1) {
            byte[] variableHeader = new byte[2];
            variableHeader[0] = (byte) (message.getMessageID() >> 8);
            variableHeader[1] = (byte) (message.getMessageID() & 0xFF);
            return Join(
                    GenerateFixedHeader(MQTTPacket.Type.PUBACK, 2, false, message.getQos().ordinal(), false),
                    variableHeader
            );
        }
        return new byte[]{0};
    }

    public static byte[] buildPubRel(Message message) {
        if(message.getQos() == Qos.QOS_2) {
            byte[] variableHeader = new byte[2];
            variableHeader[0] = (byte) (message.getMessageID() >> 8);
            variableHeader[1] = (byte) (message.getMessageID() & 0xFF);
            return Join(
                    GenerateFixedHeader(MQTTPacket.Type.PUBREL, 2, false, message.getQos().ordinal(), false),
                    variableHeader
            );
        }
        return new byte[]{0};
    }

    public static byte[] buildPubComp(Message message) {
        if(message.getQos() == Qos.QOS_2) {
            byte[] variableHeader = new byte[2];
            variableHeader[0] = (byte) (message.getMessageID() >> 8);
            variableHeader[1] = (byte) (message.getMessageID() & 0xFF);
            return Join(
                    GenerateFixedHeader(MQTTPacket.Type.PUBCOMP, 2, false, message.getQos().ordinal(), false),
                    variableHeader
            );
        }
        return new byte[]{0};
    }

    public static byte[] buildSubscribe(String topic, Qos qos) throws UnsupportedEncodingException {
        List<Byte> variableAndPayload = new ArrayList<>();

        if (topic != null && topic.length() > 0) {
            int messageId = new Random().nextInt();
            variableAndPayload.add((byte) (messageId >> 8));
            variableAndPayload.add((byte) (messageId & 0xFF));
            AppendString(variableAndPayload, topic);
            variableAndPayload.add((byte) qos.ordinal());
            return Join(
                    GenerateFixedHeader(MQTTPacket.Type.SUBSCRIBE, variableAndPayload.size(), false, 1, false),
                    ToPrimitive(variableAndPayload)
            );
        }
        return new byte[]{0};
    }

    public static byte[] buildSubAck(int messageId, Qos qos) {
        byte[] variableAndPayload = new byte[3];
        variableAndPayload[0] = ((byte) (messageId >> 8));
        variableAndPayload[1] = ((byte) (messageId & 0xFF));
        variableAndPayload[2] = ((byte) ((qos == Qos.QOS_0) ? 0x00 : (qos == Qos.QOS_1) ? 0x01 : 0x02));
        return Join(
                GenerateFixedHeader(MQTTPacket.Type.SUBACK, 3, false, 0, false),
                variableAndPayload
        );
    }

    public static byte[] buildUnsubscribe(String topic) throws UnsupportedEncodingException {
        List<Byte> variableAndPayload = new ArrayList<>();
        if(topic != null && topic.length() > 0) {
            int messageID = new Random().nextInt();
            variableAndPayload.add((byte) (messageID >> 8));
            variableAndPayload.add((byte) (messageID & 0xFF));
            AppendString(variableAndPayload, topic);
            return Join(
                    GenerateFixedHeader(MQTTPacket.Type.UNSUBSCRIBE, variableAndPayload.size(), false, 1, false),
                    ToPrimitive(variableAndPayload)
            );
        }
        return new byte[]{0};
    }

    public static byte[] buildUnsuback(int messageId) {
        byte[] variableAndPayload = new byte[2];
        variableAndPayload[0] = ((byte) (messageId >> 8));
        variableAndPayload[1] = ((byte) (messageId & 0xFF));
        return Join(
                GenerateFixedHeader(MQTTPacket.Type.UNSUBACK, 2, false, 0, false),
                variableAndPayload
        );
    }

    public static byte[] buildPingReq() {
        return GenerateFixedHeader(MQTTPacket.Type.PINGREQ, 0, false, 0, false);
    }

    public static byte[] buildPingResp() {
        return GenerateFixedHeader(MQTTPacket.Type.PINGRESP, 0, false, 0, false);
    }

    public static byte[] buildDisconnect() {
        return GenerateFixedHeader(MQTTPacket.Type.DISCONNECT, 0, false, 0, false);
    }

    private static byte[] GenerateFixedHeader(MQTTPacket.Type type, int remainLength, boolean dup, int qos, boolean retain) {
        byte[] b = new byte[(remainLength > 127) ? 3 : 2];
        b[0] = (byte) (((type.Value() & 0x0F) << 4) | (((dup ? 1 : 0) & 0x01) << 3) | ((qos & 0x3) << 1) | ((retain ? 1 : 0) & 0x1));
        if(remainLength > 127) {
            b[1] = (byte) ((remainLength % 128) | 0x80);
            b[2] = (byte) (remainLength / 128);
        } else
            b[1] = (byte) remainLength;

        return b;
    }

    private static byte[] Join(byte[] ar1, byte[] ar2) {
        int i = ar1.length;
        byte[] b = Arrays.copyOf(ar1, ar1.length + ar2.length);
        for(byte t : ar2) b[i++] = t;
        return b;
    }

    private static byte[] ToPrimitive(Collection<Byte> bytes) {
        int i = 0;
        byte[] b = new byte[bytes.size()];
        for (Byte b1 : bytes) b[i++] = b1;
        return b;
    }

    private static void AppendString(Collection<Byte> byteCollection, String string) throws UnsupportedEncodingException {
        byteCollection.add((byte) (string.length() >> 8));
        byteCollection.add((byte) (string.length() & 0xFF));
        AppendBytes(byteCollection, string.getBytes(CHARSET));
    }

    private static void AppendBytes(Collection<Byte> bytesList, byte[] bytes) {
        for (byte b : bytes)
            bytesList.add(b);
    }
}
