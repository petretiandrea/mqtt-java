package it.petretiandrea.client;

import it.petretiandrea.common.Client;
import it.petretiandrea.common.network.TransportTCP;
import it.petretiandrea.common.session.ClientSession;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;

import java.io.IOException;
import java.util.Collections;

public class MQTTClient extends Client {

    private long mKeepAliveTimeout;
    private long mTimePingResp;

    public MQTTClient(ConnectionSettings connectionSettings) throws IOException {
        super(connectionSettings,
                new ClientSession(connectionSettings.getClientId(), connectionSettings.isCleanSession()),
                new TransportTCP(),
                Collections.emptyList());
        mTimePingResp = -1;
        mKeepAliveTimeout = (getConnectionSettings().getKeepAliveSeconds() - (getKeepAliveTimeout() / 2)) * 1000;
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
                    if(getClientCallback() != null) getClientCallback().onSubscribeComplete(this, (Subscribe) packet);
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
                    if(getClientCallback() != null) getClientCallback().onUnsubscribeComplete(this, (Unsubscribe) packet);
                });
    }

    @Override
    public void onPingReqReceive(PingReq pingReq) {

    }

    @Override
    public void onPingRespReceive(PingResp pingResp) {
        mTimePingResp = -1;
    }

    @Override
    public void onDisconnect(Disconnect disconnect) throws MQTTProtocolException {
        throw new MQTTProtocolException("Client could not receive a Disconnect Message!");
    }

    @Override
    protected void onKeepAliveTimeout() throws MQTTProtocolException {
        if(mTimePingResp == -1) {
            System.out.println("MQTTClient.onKeepAliveTimeout");
            send(new PingReq());
            mTimePingResp = System.currentTimeMillis();
        } else if(System.currentTimeMillis() - mTimePingResp >= mKeepAliveTimeout) {
            throw new MQTTProtocolException("No Ping response received!");
        }
    }

    @Override
    protected long getKeepAliveTimeout() {
        return mKeepAliveTimeout;
    }
}
