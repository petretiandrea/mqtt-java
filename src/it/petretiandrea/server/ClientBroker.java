package it.petretiandrea.server;

import it.petretiandrea.common.Client;
import it.petretiandrea.common.network.Transport;
import it.petretiandrea.common.session.BrokerSession;
import it.petretiandrea.common.session.ClientSession;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.util.List;

public class ClientBroker extends Client {

    private long mKeepAliveTimeout;
    private ClientBrokerCallback mBrokerCallback;

    public ClientBroker(ConnectionSettings connectionSettings, BrokerSession brokerSession, Transport transport, List<MQTTPacket> pendingQueue) {
        super(connectionSettings, new ClientSession(brokerSession), transport, pendingQueue);

        mKeepAliveTimeout = (connectionSettings.getKeepAliveSeconds()) + (connectionSettings.getKeepAliveSeconds() /2 ) * 1000;
    }

    public ClientBrokerCallback getBrokerCallback() {
        return mBrokerCallback;
    }

    public void setBrokerCallback(ClientBrokerCallback brokerCallback) {
        mBrokerCallback = brokerCallback;
    }

    @Override
    protected void onKeepAliveTimeout() throws MQTTProtocolException {
        throw new MQTTProtocolException("No Response from Client!");
    }

    @Override
    protected long getKeepAliveTimeout() {
        return mKeepAliveTimeout;
    }

    @Override
    public void onConnectReceive(Connect connect) throws MQTTProtocolException {

    }

    @Override
    public void onConnAckReceive(ConnAck connAck) {

    }

    @Override
    public void onSubscribeReceive(Subscribe subscribe) throws MQTTProtocolException {
        send(new SubAck(subscribe.getMessageID(), subscribe.getQosSub()));
        if(getClientCallback() != null)
            getClientCallback().onSubscribeComplete(this, subscribe);
    }

    @Override
    public void onSubAckReceive(SubAck subAck) {

    }

    @Override
    public void onUnsubscribeReceive(Unsubscribe unsubscribe) throws MQTTProtocolException {
        send(new UnsubAck(unsubscribe.getMessageID()));
        if(getClientCallback() != null)
            getClientCallback().onUnsubscribeComplete(this, unsubscribe);
    }

    @Override
    public void onUnsubAckReceive(UnsubAck unsubAck) {

    }

    @Override
    public void onPingReqReceive(PingReq pingReq) {
        System.out.println("ClientBroker.onPingReqReceive");
        send(new PingResp());
    }

    @Override
    public void onPingRespReceive(PingResp pingResp) {

    }

    @Override
    public void onDisconnect(Disconnect disconnect) throws MQTTProtocolException {
        if(getClientCallback() != null)
            getClientCallback().onDisconnect(this);
    }
}
