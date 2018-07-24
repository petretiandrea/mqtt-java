package it.petretiandrea.common;

import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.base.MQTTPacket;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wrap a socket and provide thread safe IO operation.
 */
public class TransportTCP implements Transport {

    private ReentrantLock mLockWrite;
    private ReentrantLock mLockRead;
    private Socket mSocket;
    private BufferedMQTTReader mMQTTReader;

    public TransportTCP(Socket socket) throws IOException {
        mSocket = socket;
        mLockWrite = new ReentrantLock(true);
        mLockRead = new ReentrantLock(true);
        mMQTTReader = new BufferedMQTTReader(mSocket.getInputStream());
    }

    /**
     * Read a MQTTPacket object.
     * @param timeout Timeout for read operation.
     * @return A valid MQTTPacket, null otherwise.
     * @throws IOException  If there is an error on socket, or timeout expire.
     * @throws MQTTParseException If the packet is not a valid MQTT packet.
     */
    @Override
    public MQTTPacket readPacket(int timeout) throws IOException, MQTTParseException {
        int tmpTimeout = mSocket.getSoTimeout();
        try {
            if(timeout > -1)
                mSocket.setSoTimeout(timeout);
            return mMQTTReader.nextMQTTPacket();
        } finally {
            if(timeout > -1)
                mSocket.setSoTimeout(tmpTimeout);
        }
    }

    /**
     * Read a MQTTPacket from transport object.
     * @return A valid MQTTPacket, null otherwise.
     * @throws IOException If there is an error on socket.
     * @throws MQTTParseException If the packet is not a valid MQTT packet.
     */
    @Override
    public MQTTPacket readPacket() throws IOException, MQTTParseException {
        return readPacket(-1);
    }

    @Override
    public void writePacket(MQTTPacket packet, int timeout) throws IOException {
        int tmpTimeout = mSocket.getSoTimeout();
        try {
            if(timeout > -1)
                mSocket.setSoTimeout(timeout);
            write(packet.toByte());
        } finally {
            if(timeout > -1)
                mSocket.setSoTimeout(tmpTimeout);
        }
    }

    @Override
    public void writePacket(MQTTPacket packet) throws IOException {
        writePacket(packet, -1);
    }

    @Override
    public void close() throws IOException {
        mLockRead.lock();
        mLockWrite.lock();
        try {
            mSocket.close();
        } finally {
            mLockWrite.unlock();
            mLockRead.unlock();
        }
    }


    /**
     * Write on socket. It's thread safe.
     * @param data Data to be writed.
     * @throws IOException If there is an error on socket write.
     */
    public void write(byte[] data) throws IOException {
        mLockWrite.lock();
        try {
            mSocket.getOutputStream().write(data);
        } finally {
            mLockWrite.unlock();
        }
    }

    /**
     * Read from the socket. It's thread safe.
     * @param buffer buffer where save the data.
     * @return The length of data read
     * @throws IOException If there is and error on socket read.
     */
    public int read(byte[] buffer) throws IOException {
        mLockRead.lock();
        try {
            return mSocket.getInputStream().read(buffer);
        } finally {
            mLockRead.unlock();
        }
    }
}
