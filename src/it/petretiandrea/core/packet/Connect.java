package it.petretiandrea.core.packet;

import it.petretiandrea.core.*;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static it.petretiandrea.utils.Utils.*;
import static it.petretiandrea.core.exception.MQTTParseException.Reason;

public class Connect extends MQTTPacket {

    // 0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ
    private static final String PATTERN_CLIENT_ID = "[a-zA-Z0-9]";
    private static final byte FLAG_CLEAN_SESSION = 2;
    private static final byte FLAG_WILL = (1 << 2);
    private static final byte FLAG_WILL_RETAIN = (1 << 5);
    private static final byte FLAG_PASSWORD = (1 << 6);
    private static final byte FLAG_USERNAME = (byte) (1 << 7);

    private MQTTVersion mMQTTVersion;
    private String mClientID;
    private String mProtocolName;
    private int mProtocolLevel;
    private String mUsername;
    private String mPassword;
    private boolean mCleanSession;
    private int mKeepAliveSeconds;
    private Message mWillMessage;


    public Connect(MQTTVersion mqttVersion, ConnectionSettings connectionSettings) {
        this(mqttVersion, connectionSettings.getClientId(), (mqttVersion == MQTTVersion.MQTT_31) ? "MQIsdp" : "MQTT",
                (mqttVersion == MQTTVersion.MQTT_31) ? 3 : 4, connectionSettings.getUsername(),
                connectionSettings.getPassword(), connectionSettings.isCleanSession(), connectionSettings.getKeepAliveSeconds(),
                connectionSettings.getWillMessage());

    }

    private Connect(MQTTVersion MQTTVersion, String clientID, String protocolName, int protocolLevel,
                   String username, String password, boolean cleanSession, int keepAliveSeconds, Message willMessage) {
        super(Type.CONNECT, false, Qos.QOS_0, false);
        mMQTTVersion = MQTTVersion;
        mClientID = clientID;
        mProtocolName = protocolName;
        mProtocolLevel = protocolLevel;
        mUsername = username;
        mPassword = password;
        mCleanSession = cleanSession;
        mKeepAliveSeconds = keepAliveSeconds;
        mWillMessage = willMessage;
    }

    // constructor from packet of byte to object.
    public Connect(byte fixedHeader, byte[] packet) throws UnsupportedEncodingException, MQTTParseException {
        super(fixedHeader);
        int offset = 0;

        int protocolNameLength = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]);
        mProtocolName = new String(packet, offset, protocolNameLength, "UTF-8");

        offset += protocolNameLength;

        mProtocolLevel = packet[offset++] & 0xFF;
        boolean usernameFlag = (packet[offset] & 0x80) == 0x80;
        boolean passwordFlag = (packet[offset] & 0x40) == 0x40;


        boolean willRetainFlag = (packet[offset] & 0x20) == 0x20;
        int willQos = (packet[offset] & 0x18) >> 3;
        boolean willFlag = (packet[offset] & 0x04) == 0x04;
        mCleanSession = (packet[offset] & 0x02) == 0x02;

        mKeepAliveSeconds = Utils.getIntFromMSBLSB(packet[++offset], packet[++offset]);

        // parse MQTTClient ID.
        int clientIDLength = Utils.getIntFromMSBLSB(packet[++offset], packet[++offset]);
        mClientID = new String(packet, ++offset, clientIDLength, "UTF-8");
        offset += clientIDLength;
        if(clientIDLength > 23 || clientIDLength < 0 || mClientID.trim().isEmpty() /*|| !mClientID.matches(PATTERN_CLIENT_ID)*/)
            throw new MQTTParseException("Invalid MQTTClient ID", Reason.INVALID_CLIENT_ID);

        // parse of Will Message
        if(willFlag) {
            // parse will topic.
            int strLength = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]);
            String willTopic = new String(packet, offset, strLength, "UTF-8");
            offset += strLength;
            if(strLength <= 0 || willTopic.trim().isEmpty()) throw new MQTTParseException("Invalid Will Topic", Reason.INVALID_WILL);
            // parse will message content
            strLength = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]);
            String willMessage = new String(packet, offset, strLength, "UTF-8");
            offset += strLength;
            if(strLength <= 0 || willMessage.trim().isEmpty()) throw new MQTTParseException("Invalid Will Topic", Reason.INVALID_WILL);
            mWillMessage = new Message(willTopic, willMessage, Qos.fromInteger(willQos), willRetainFlag);
        } else {
            mWillMessage = null;
            if(Qos.fromInteger(willQos) != Qos.QOS_0) throw new MQTTParseException("Invalid Will QOS", Reason.INVALID_QOS);
        }

        if(usernameFlag) {
            int usernameLength = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]);
            String username = new String(packet, offset, usernameLength, "UTF-8");
            offset += usernameLength;
            mUsername = username;
            if(usernameLength <= 0 || mUsername.trim().isEmpty()) throw new MQTTParseException("No username", Reason.NO_USERNAME);
        } else mUsername = null;

        if(passwordFlag) {
            int passwordLength = Utils.getIntFromMSBLSB(packet[offset++], packet[offset++]);
            String password = new String(packet, offset, passwordLength, "UTF-8");
            offset += passwordLength;
            mPassword = password;
            if(passwordLength <= 0 || mPassword.trim().isEmpty()) throw new MQTTParseException("No password", Reason.NO_PASSWORD);
        } else mPassword = null;

        if(mProtocolName.equals("MQTT") && mProtocolLevel == 4)
            mMQTTVersion = MQTTVersion.MQTT_311;
        else if(mProtocolName.equals("MQIsdp") && mProtocolLevel == 3)
            mMQTTVersion = MQTTVersion.MQTT_31;
        else
            throw new MQTTParseException("Invalid MQTT Version!", Reason.INVALID_MQTT_NAME_LEVEL);
    }

    @Override
    public byte[] toByte() throws UnsupportedEncodingException {
        List<Byte> bytes = new ArrayList<>(15);

        // length MSB
        bytes.add((byte) 0);
        // length LSB
        bytes.add((mMQTTVersion == MQTTVersion.MQTT_31) ? (byte) 6 : 4);
        // protocol name
        AppendBytes(bytes, getProtocolName().getBytes(CHARSET));
        // protocol version
        bytes.add((byte) getProtocolLevel());

        // connection flags
        int indexConnectionFlags = bytes.size();
        bytes.add((byte) 0);

        if(isCleanSession())
            bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | FLAG_CLEAN_SESSION));

        // keep alive
        // MSB and LSB
        bytes.add((byte)(getKeepAliveSeconds() >> 8));
        bytes.add((byte)(getKeepAliveSeconds() & 0xFF)); // mask for 8bit.

        // payload MQTTClient Identifier, Will Topic, Will Message, User Name, Password
        // 1. MQTTClient ID
        AppendString(bytes, getClientID());

        // 2-3. Topic Will and Topic Message
        if(getWillMessage() != null) {
            AppendString(bytes, getWillMessage().getTopic());
            AppendString(bytes, getWillMessage().getMessage());
            // flag message will
            bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | FLAG_WILL));
            // qos will
            bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | (getWillMessage().getQos().ordinal() & 3) << 3));
            // will retain
            if(getWillMessage().isRetain())
                bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | FLAG_WILL_RETAIN));
        }
        // 4. username
        if(getUsername() != null && getUsername().length() > 0) {
            // add connection flag
            bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | FLAG_USERNAME));
            AppendString(bytes, getUsername());
        }
        // 5. Password
        if(getPassword() != null && getPassword().length() > 0) {
            // add connection flag
            bytes.set(indexConnectionFlags, (byte) (bytes.get(indexConnectionFlags) | FLAG_PASSWORD));
            AppendString(bytes, getPassword());
        }

        // Join fixed header to variable header and payload
        return Join(GenerateFixedHeader(getCommand(), bytes.size(), isDup(), getQos().ordinal(), isRetain()), ToPrimitive(bytes));
    }

    public MQTTVersion getMQTTVersion() {
        return mMQTTVersion;
    }

    public String getClientID() {
        return mClientID;
    }

    public String getProtocolName() {
        return mProtocolName;
    }

    public int getProtocolLevel() {
        return mProtocolLevel;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getPassword() {
        return mPassword;
    }

    public boolean isCleanSession() {
        return mCleanSession;
    }

    public int getKeepAliveSeconds() {
        return mKeepAliveSeconds;
    }

    public Message getWillMessage() {
        return mWillMessage;
    }

    @Override
    public String toString() {
        return "Connect{" +
                "mProtocolName='" + mProtocolName + '\'' +
                ", mProtocolLevel=" + mProtocolLevel +
                ", mUsername='" + mUsername + '\'' +
                ", mPassword='" + mPassword + '\'' +
                ", mCleanSession=" + mCleanSession +
                ", mKeepAliveSeconds=" + mKeepAliveSeconds +
                ", mWillMessage=" + mWillMessage +
                "} " + super.toString();
    }
}
