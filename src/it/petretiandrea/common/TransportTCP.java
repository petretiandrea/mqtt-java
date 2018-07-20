package it.petretiandrea.common;

import java.io.IOException;
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

    public TransportTCP(Socket socket) {
        mSocket = socket;
        mLockWrite = new ReentrantLock(true);
        mLockRead = new ReentrantLock(true);
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
