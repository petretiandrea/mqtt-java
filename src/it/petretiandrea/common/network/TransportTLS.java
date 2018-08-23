package it.petretiandrea.common.network;

import it.petretiandrea.core.exception.MQTTParseException;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.server.security.SSLContextProvider;
import it.petretiandrea.server.security.TLSProvider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.ReentrantLock;

public class TransportTLS implements Transport {

    private ReentrantLock mLockWrite;
    private ReentrantLock mLockRead;
    private Socket mSocket;
    private BufferedMQTTReader mMQTTReader;

    private SSLSocketFactory mSSLSocketFactory;

    public TransportTLS(SSLContextProvider contextProvider) throws IOException {
        try {
            SSLContext context = SSLContext.getInstance(contextProvider.getProtocol());
            context.init(contextProvider.getKeyManagers(), contextProvider.getTrustManagers(), null);
            mSSLSocketFactory = context.getSocketFactory();
            mLockWrite = new ReentrantLock(true);
            mLockRead = new ReentrantLock(true);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public TransportTLS(SSLSocketFactory SSLSocketFactory) throws IOException {
        this(null, SSLSocketFactory);

    }

    public TransportTLS(Socket socket) throws IOException {
        this(socket, (SSLSocketFactory) SSLSocketFactory.getDefault());
    }

    public TransportTLS(Socket socket, SSLSocketFactory SSLSocketFactory) throws IOException {
        mSSLSocketFactory = SSLSocketFactory;
        mSocket = socket;
        mLockWrite = new ReentrantLock(true);
        mLockRead = new ReentrantLock(true);
        mMQTTReader = (socket != null) ? new BufferedMQTTReader(mSocket.getInputStream()) : null;
    }

    @Override
    public void connect(String hostname, int port) {
        try {
            if(mSocket == null) {
                mSocket = mSSLSocketFactory.createSocket(hostname, port);
                mMQTTReader = new BufferedMQTTReader(mSocket.getInputStream());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
     * Read a MQTTPacket from network object.
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

    @Override
    public boolean isConnected() {
        if(mSocket != null)
            return mSocket.isConnected();
        return false;
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
            mSocket.getOutputStream().flush();
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
