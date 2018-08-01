package it.petretiandrea.common;

import it.petretiandrea.common.network.Transport;
import it.petretiandrea.common.network.TransportTCP;
import it.petretiandrea.common.session.ClientSession;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MQTTClient extends Client {

    public MQTTClient(ConnectionSettings connectionSettings) throws IOException {
        super(connectionSettings,
                new ClientSession(connectionSettings.getClientId(), connectionSettings.isCleanSession()),
                new TransportTCP(),
                Collections.emptyList());
    }

    @Override
    public void onConnectReceive(Connect connect) throws MQTTProtocolException {
        throw new MQTTProtocolException("Client could not receive Connect Message!");
    }

    @Override
    public void onConnAckReceive(ConnAck connAck) {

    }

    @Override
    public void onSubscribeReceive(Subscribe subscribe) throws MQTTProtocolException {
        throw new MQTTProtocolException("Client could not receive Subscribe Message!");
    }

    @Override
    public void onSubAckReceive(SubAck subAck) {
        // check for subscribe not ack
        getClientSession().getSendedNotAck().stream()
                .filter(packet -> (packet instanceof Subscribe) && ((Subscribe)packet).getMessageID() == subAck.getMessageID())
                .findFirst()
                .ifPresent(packet -> {
                    getClientSession().getSendedNotAck().remove(packet);
                    if(getClientCallback() != null) getClientCallback().onSubscribeComplete((Subscribe) packet);
                });
    }

    @Override
    public void onUnsubscribeReceive(Unsubscribe unsubscribe) throws MQTTProtocolException {
        throw new MQTTProtocolException("Client could not receive a Unsubscribe Message!");
    }

    @Override
    public void onUnsubAckReceive(UnsubAck Unsubscribe) {
        getClientSession().getSendedNotAck().stream()
                .filter(packet -> (packet instanceof Unsubscribe) && ((Unsubscribe)packet).getMessageID() == Unsubscribe.getMessageID())
                .findFirst()
                .ifPresent(packet -> {
                    getClientSession().getSendedNotAck().remove(packet);
                    if(getClientCallback() != null) getClientCallback().onUnsubscribeComplete((Unsubscribe) packet);
                });
    }

    @Override
    public void onPingReqReceive(PingReq pingReq) {

    }

    @Override
    public void onPingRespReceive(PingResp pingResp) {

    }

    @Override
    public void onDisconnect(Disconnect disconnect) throws MQTTProtocolException {
        throw new MQTTProtocolException("Client could not receive a Disconnect Message!");
    }
}
