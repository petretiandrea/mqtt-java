package it.petretiandrea.common;

import it.petretiandrea.common.network.Transport;
import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public class Receiver {

    private PacketDispatcher.IPacketReceiver mPacketReceiver;
    private Thread mThread;
    private boolean mRunning;
    private ReentrantLock mLock;

    private QueueMQTT<MQTTPacket> mOutgoing;


    public Receiver(PacketDispatcher.IPacketReceiver packetReceiver) {
        mPacketReceiver = packetReceiver;
        mThread = new Thread(this::loop);
        mLock = new ReentrantLock(true);
        mRunning = false;
    }

    private void setRunning(boolean state) {
        mLock.lock();
        try {
            mRunning = state;
        } finally {
            mLock.unlock();
        }
    }

    private boolean isRunning() {
        mLock.lock();
        try {
            return mRunning;
        } finally {
            mLock.unlock();
        }
    }

    public void startLoop() {
        if(!isRunning()) {
            setRunning(true);
            mThread.start();
        } else throw new IllegalStateException("Loop already in execution!");
    }

    private void sendOutgoing() {

    }

    private void loop() {

    }


   /* private long mIOTimeout;
    private Transport mTransport;
    private Thread mThread;

    private long mLastReceiveTime;
    private long mKeepAliveTimeout;
    private boolean mKeepAliveExpired;
    private QueueMQTT<MQTTPacket> mPendingSend;

    public Receiver(long keepAliveTimeout, long IOTimeout) {
        mThread = new Thread(this::loop);
        mIOTimeout = IOTimeout;
        mKeepAliveTimeout = keepAliveTimeout;
    }

    protected void startLoop(Transport transport) {
        mTransport = transport;
        mThread.start();
    }

    public long getLastReceiveTime() {
        return mLastReceiveTime;
    }

    private void loop() {
        try {
            while (!Thread.interrupted()) {
                try {
                    // send pending packet.
                    MQTTPacket packet = mTransport.readPacket((int) mIOTimeout);
                    if(packet != null) {
                        mLastReceiveTime = System.currentTimeMillis();
                        //(packet);
                        System.out.println(packet);
                    }
                } catch (SocketTimeoutException ignored) {
                } finally {
                    // check keep alive timeout
                    checkKeepAlive();
                }
            }
        } catch (IOException | MQTTProtocolException | MQTTParseException ex) {
            ex.printStackTrace();
        } finally {
            // stop loop and notify disconnect
        }
    }

    private void checkKeepAlive() throws MQTTProtocolException {
        long now = System.currentTimeMillis();
        if(now - mLastReceiveTime >= mKeepAliveTimeout) {
            if(mKeepAliveExpired) // is already expired
                throw new MQTTProtocolException(String.format("No Response received %d ms", mKeepAliveTimeout));
            // here timeout is expired
            mKeepAliveExpired = true;
            //keepAliveExpired();
            mLastReceiveTime = mKeepAliveTimeout;
        } else mKeepAliveExpired = false;
    }
*/

}
