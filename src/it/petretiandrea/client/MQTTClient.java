package it.petretiandrea.client;

import it.petretiandrea.common.network.TransportTLS;
import it.petretiandrea.utils.CustomLogger;
import it.petretiandrea.common.Client;
import it.petretiandrea.common.network.TransportTCP;
import it.petretiandrea.common.session.ClientSession;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

public class MQTTClient extends Client {

    private long mKeepAliveTimeout;
    private long mTimePingResp;

    public MQTTClient(ConnectionSettings connectionSettings) throws IOException, NoSuchAlgorithmException {
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
        CustomLogger.LOGGER.info("Client: SubAck Received " + subAck);
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
    public void onUnsubAckReceive(UnsubAck unsubscribe) {
        CustomLogger.LOGGER.info("Client: Unsuback Received " + unsubscribe);
        getClientSession().getSendedNotAck().stream()
                .filter(packet -> (packet instanceof Unsubscribe) && ((Unsubscribe)packet).getMessageID() == unsubscribe.getMessageID())
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
        CustomLogger.LOGGER.info("Client: Received Ping Response");
    }

    @Override
    public void onDisconnect(Disconnect disconnect) throws MQTTProtocolException {
        CustomLogger.LOGGER.info("Client disconnected " + getClientSession().getClientID());
        throw new MQTTProtocolException("Client could not receive a Disconnect Message!");
    }

    @Override
    protected void onKeepAliveTimeout() throws MQTTProtocolException {
        if(mTimePingResp == -1) {
            CustomLogger.LOGGER.info("Client " + getClientSession().getClientID() + ": Timeout keep alive reached, send Ping Request");
            send(new PingReq());
            mTimePingResp = System.currentTimeMillis();
        } else if(System.currentTimeMillis() - mTimePingResp >= mKeepAliveTimeout) {
            CustomLogger.LOGGER.info("Client " + getClientSession().getClientID() + ": No Ping Response Received");
            throw new MQTTProtocolException("No Ping response received!");
        }
    }

    @Override
    protected long getKeepAliveTimeout() {
        return mKeepAliveTimeout;
    }
}
