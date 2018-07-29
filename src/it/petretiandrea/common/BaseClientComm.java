package it.petretiandrea.common;

import it.petretiandrea.common.network.Transport;
import it.petretiandrea.core.ConnectionSettings;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.server.ServerSession;

import java.util.concurrent.locks.ReentrantLock;

public class BaseClientComm {

    private Thread mLoop;
    private boolean mRunning;
    private ReentrantLock mLock;
    private Transport mTransport;

    private Session mSession;
    private PacketDispatcher mPacketDispatcher;
    private final ConnectionSettings mConnectionSettings;
    private QueueMQTT<MQTTPacket> mOutgoing;

    public BaseClientComm(ConnectionSettings connectionSettings) {
        this(null, null, connectionSettings);
    }

    public BaseClientComm(Transport transport, Session session, ConnectionSettings settings) {
        mConnectionSettings = settings;
        mSession = session;
        mTransport = transport;
        mOutgoing = new QueueMQTT<>();
        mLock = new ReentrantLock(true);
        mRunning = false;
    }

    private void setRunning(boolean running) {
        mLock.lock();
        try {
            mRunning = running;
        } finally {
            mLock.unlock();
        }
    }

    public boolean isRunning() {
        mLock.lock();
        try {
            return mRunning;
        } finally {
            mLock.unlock();
        }
    }

    public boolean isConnect() {
        return isRunning();
    }
}
