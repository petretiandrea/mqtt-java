package it.petretiandrea.common.session;

import it.petretiandrea.core.packet.base.MQTTPacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ClientSession extends Session {

    public ClientSession(String clientID, boolean cleanSession) {
        super(clientID, cleanSession);
    }

    public ClientSession(String clientID, boolean cleanSession, List<MQTTPacket> sendedNotAck, List<MQTTPacket> receivedNotAck) {
        super(clientID, cleanSession, sendedNotAck, receivedNotAck);
    }

    public ClientSession(BrokerSession brokerSession) {
        super(brokerSession.getClientID(), brokerSession.isCleanSession(),
                new ArrayList<>(brokerSession.getSendedNotAck()), new ArrayList<>(brokerSession.getReceivedNotAck()));
    }
}
