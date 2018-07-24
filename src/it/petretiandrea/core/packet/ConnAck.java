package it.petretiandrea.core.packet;

import it.petretiandrea.core.Qos;
import it.petretiandrea.core.ConnectionStatus;
import it.petretiandrea.core.Utils;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.base.MQTTPacket;

public class ConnAck extends MQTTPacket {

    private boolean mSessionPresent;
    private ConnectionStatus mConnectionStatus;

    public ConnAck(boolean sessionPresent, ConnectionStatus connectionStatus) {
        super(Type.CONNACK, false, Qos.QOS_0, false);
        mSessionPresent = sessionPresent;
        mConnectionStatus = connectionStatus;
    }

    public ConnAck(byte fixedHeader, byte[] body) throws MQTTParseException {
        super(fixedHeader);
        mSessionPresent = (body[0] & 0x01) == 1;;
        mConnectionStatus = ConnectionStatus.fromInteger(body[1] & 0xFF);
    }

    @Override
    public byte[] toByte() {
        byte[] variableHeader = new byte[2];
        // if the connection is not setted to accept, the sessionePresent is set to 0.
        // first byte is setted if the server have the sessione for this client.
        variableHeader[0] = (mConnectionStatus != ConnectionStatus.ACCEPT) ? 0 : (byte) (mSessionPresent ? 1 : 0);
        variableHeader[1] = (byte) mConnectionStatus.Value();
        return Utils.Join(
                Utils.GenerateFixedHeader(getCommand(), 2, isDup(), getQos().ordinal(), isRetain()),
                variableHeader
        );
    }

    public boolean isSessionPresent() {
        return mSessionPresent;
    }

    public ConnectionStatus getConnectionStatus() {
        return mConnectionStatus;
    }

    @Override
    public String toString() {
        return "ConnAck{" +
                "mSessionPresent=" + mSessionPresent +
                ", mConnectionStatus=" + mConnectionStatus +
                "} " + super.toString();
    }
}
