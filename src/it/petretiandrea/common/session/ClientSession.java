package it.petretiandrea.common.session;

import it.petretiandrea.core.packet.base.MQTTPacket;

import java.util.List;

public class ClientSession extends Session {

    public ClientSession(String clientID, boolean cleanSession) {
        super(clientID, cleanSession);
    }

    public ClientSession(String clientID, boolean cleanSession, List<MQTTPacket> sendedNotAck, List<MQTTPacket> receivedNotAck) {
        super(clientID, cleanSession, sendedNotAck, receivedNotAck);
    }
}
