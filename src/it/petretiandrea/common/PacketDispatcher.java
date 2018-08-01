package it.petretiandrea.common;

import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;

public class PacketDispatcher {

    public interface IPacketReceiver {
        void onConnectReceive(Connect connect) throws MQTTProtocolException;
        void onConnAckReceive(ConnAck connAck);
        void onPublishReceive(Publish publish);
        void onPubAckReceive(PubAck pubAck);
        void onPubRecReceive(PubRec pubRec);
        void onPubRelReceive(PubRel pubRel);
        void onPubCompReceive(PubComp pubComp);
        void onSubscribeReceive(Subscribe subscribe) throws MQTTProtocolException;
        void onSubAckReceive(SubAck subAck);
        void onUnsubscribeReceive(Unsubscribe unsubscribe) throws MQTTProtocolException;
        void onUnsubAckReceive(UnsubAck unsubAck);
        void onPingReqReceive(PingReq pingReq);
        void onPingRespReceive(PingResp pingResp);
        void onDisconnect(Disconnect disconnect) throws MQTTProtocolException;
    }

    private IPacketReceiver mPacketReceiver;

    public PacketDispatcher(IPacketReceiver packetReceiver) {
        mPacketReceiver = packetReceiver;
    }

    public void dispatch(MQTTPacket packet) throws MQTTProtocolException {
        if(packet != null) {
            switch (packet.getCommand()) {
                case CONNECT:
                    mPacketReceiver.onConnectReceive((Connect) packet);
                    break;
                case CONNACK:
                    mPacketReceiver.onConnAckReceive((ConnAck) packet);
                    break;
                case PUBLISH:
                    mPacketReceiver.onPublishReceive((Publish) packet);
                    break;
                case PUBACK:
                    mPacketReceiver.onPubAckReceive((PubAck) packet);
                    break;
                case PUBREC:
                    mPacketReceiver.onPubRecReceive((PubRec) packet);
                    break;
                case PUBREL:
                    mPacketReceiver.onPubRelReceive((PubRel) packet);
                    break;
                case PUBCOMP:
                    mPacketReceiver.onPubCompReceive((PubComp) packet);
                    break;
                case SUBSCRIBE:
                    mPacketReceiver.onSubscribeReceive((Subscribe) packet);
                    break;
                case SUBACK:
                    mPacketReceiver.onSubAckReceive((SubAck) packet);
                    break;
                case UNSUBSCRIBE:
                    mPacketReceiver.onUnsubscribeReceive((Unsubscribe) packet);
                    break;
                case UNSUBACK:
                    mPacketReceiver.onUnsubAckReceive((UnsubAck) packet);
                    break;
                case PINGRESP:
                    mPacketReceiver.onPingRespReceive((PingResp) packet);
                    break;
                case PINGREQ:
                    mPacketReceiver.onPingReqReceive((PingReq) packet);
                    break;
                case DISCONNECT:
                    mPacketReceiver.onDisconnect((Disconnect) packet);
                    break;
            }
        }
    }

}
