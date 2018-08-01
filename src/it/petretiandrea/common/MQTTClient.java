package it.petretiandrea.common;

import it.petretiandrea.common.session.ClientSession;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.util.List;

public class MQTTClient extends Client {

    public MQTTClient(ConnectionSettings connectionSettings) {
        super(connectionSettings, new ClientSession(connectionSettings.getClientId(), connectionSettings.isCleanSession()));
    }

    @Override
    public void onSubscribeReceive(Subscribe subscribe) {

    }

    @Override
    public void onSubAckReceive(SubAck subAck) {

    }

    @Override
    public void onUnsubscribeReceive(Unsubscribe unsubscribe) {

    }

    @Override
    public void onUnsubAckReceive(UnsubAck unsubAck) {

    }

    @Override
    public void onPingReqReceive(PingReq pingReq) {

    }

    @Override
    public void onPingRespReceive(PingResp pingResp) {

    }

    @Override
    public void onDisconnect(Disconnect disconnect) {

    }
}
