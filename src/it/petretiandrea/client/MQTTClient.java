package it.petretiandrea.client;

import it.petretiandrea.common.QueueMQTT;
import it.petretiandrea.common.Transport;
import it.petretiandrea.common.TransportTCP;
import it.petretiandrea.core.*;
import it.petretiandrea.core.exception.MQTTException;
import it.petretiandrea.core.exception.MQTTProtocolException;
import it.petretiandrea.core.packet.*;
import it.petretiandrea.core.packet.base.MQTTPacket;
import it.petretiandrea.core.exception.MQTTParseException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public class MQTTClient {

    private static double SOCKET_IO_TIMEOUT = 0.5;

    private Transport mTransport;
    private Socket mSocket;
    private boolean mConnected;

    private QueueMQTT mIncoming;
    private ConnectionSettings mConnectionSettings;

    private Thread mReadThread;

    private long mLastPingRequest;
    private long mLastPingResponse;
    private long mPingRequestTimeout;
    private long mPingResponseTimeout;

    public MQTTClient(ConnectionSettings connectionSettings) {
        mIncoming = new QueueMQTT();
        mConnected = false;
        mConnectionSettings = connectionSettings;
        mReadThread = new Thread(this::readTaskThread);
        mLastPingRequest = mLastPingResponse = System.currentTimeMillis();

        // timeout for send a request ping is the real keep alive - 1/4 of real keep alive.
        mPingRequestTimeout = (mConnectionSettings.getKeepAliveSeconds() - (mConnectionSettings.getKeepAliveSeconds() / 4)) * 1000;
        // timeout for receive ping response is the real keep alive + 1/4 of real keep alive.
        mPingResponseTimeout = (mConnectionSettings.getKeepAliveSeconds() + (mConnectionSettings.getKeepAliveSeconds() / 4)) * 1000;
    }

    /**
     * Get the connected status
     * @return True if client is connect, false otherwise.
     */
    public boolean isConnected() {
        return mConnected;
    }

    /**
     * Connect to MQTT broker using the connection settings.
     * @return True if the connection is successful, false otherwise.
     * @throws MQTTParseException If the server response is invalid.
     * @throws IOException If there is and error on socket.
     * @throws MQTTException If the server not accept the connection.
     */
    public synchronized boolean connect() throws MQTTParseException, IOException, MQTTException {
        mConnected = connectMQTT();
        return mConnected;
    }

    /**
     * Disconnet the client.
     * @return True if disconnection is successful, False otherwise.
     */
    public synchronized boolean disconnet() {
        if(mConnected && !mSocket.isClosed()) {
            try {
                mTransport.write(new Disconnect().toByte());
                mSocket.close();
                mReadThread.interrupt();
                mConnected = false;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mSocket = null;
                mReadThread = null;
            }
        }
        return !mConnected;
    }

    public void publish(Message message){
        try {
            if (mConnected) {
                if(message.getQos().ordinal() > Qos.QOS_0.ordinal()) {
                    // need to add to queue for wait the puback, or pubrel, pubrec, ecc..
                }
                mTransport.write(new Publish(message).toByte());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // same of connect()
    private boolean connectMQTT() throws IOException, MQTTException, MQTTParseException {
        if(mSocket == null) {
            mSocket = new Socket();
            mSocket.setSoTimeout((int) (SOCKET_IO_TIMEOUT * 1000)); // milliseconds
            mTransport = new TransportTCP(mSocket);
        }

        if(!mConnected) {
             // milliseconds
            mSocket.connect(new InetSocketAddress(mConnectionSettings.getHostname(), mConnectionSettings.getPort()));

            if(mSocket.isConnected()) {
                MQTTPacket connack = null;
                mTransport.write(new Connect(MQTTVersion.MQTT_311, mConnectionSettings).toByte());
                if((connack = readPacket()) != null) {
                    if(connack.getCommand() == MQTTPacket.Type.CONNACK) {
                        if (((ConnAck) connack).getConnectionStatus() == ConnectionStatus.ACCEPT) {
                            mReadThread.start();
                            return true;
                        } else throw new MQTTException(((ConnAck) connack).getConnectionStatus().toString());
                    }
                }
            }
        }
        return false;
    }

    // behaviour of Read Thread.
    private void readTaskThread() {
        try {
            while (!Thread.interrupted() && !mSocket.isClosed()) {
                try {
                    MQTTPacket packet = readPacket();
                    if(packet != null) {
                        handleMQTTPacket(packet);
                        System.out.println(packet);
                    }
                } catch (SocketTimeoutException | MQTTParseException ex) {
                    if(ex instanceof  MQTTParseException)
                        ex.printStackTrace();
                }
                // Keep alive algorithm
                checkKeepAlive();
            }
        } catch (IOException | MQTTProtocolException ex) {
            ex.printStackTrace();
        } finally {
            disconnet();
        }
    }

    /**
     * Method for check the KeepAlive status.
     * @throws IOException If there is an error during Ping Request
     * @throws MQTTProtocolException If the timeout for receive a Ping Response from server expire.
     */
    private void checkKeepAlive() throws IOException, MQTTProtocolException {
        long now = System.currentTimeMillis();
        if(now - mLastPingRequest >= mPingRequestTimeout) {
            // timeout reached need to send ping req.
            mTransport.write(new PingReq().toByte());
            mLastPingRequest = now;
            System.out.println("Sended PING REQUEST");
        } else if(now - mLastPingResponse > mPingResponseTimeout) {
            // here no response from server from my ping request, throw and exception for no ping response received
            throw new MQTTProtocolException(String.format("No Ping Response received %d ms", mPingResponseTimeout));
        }
    }

    /**
     * Method for handle the incoming packet.
     * @param packet The incoming packet
     * @throws IOException If there is an error during response on socket.
     */
    private void handleMQTTPacket(MQTTPacket packet) throws IOException {
        switch (packet.getCommand()) {
            case PINGREQ: // server request for a ping
                System.out.println("Received PING REQUEST");
                mTransport.write(new PingResp().toByte()); // send a ping response to server.
                break;
            case PINGRESP:
                System.out.println("Received PING RESPONSE");
                mLastPingResponse = System.currentTimeMillis(); // save the last response from server.
                break;
        }
    }

    /**
     * Read a MQTTPacket from transport object.
     * @return A valid MQTTPacket, null otherwise.
     * @throws IOException If there is an error on socket.
     * @throws MQTTParseException If the packet is not a valid MQTT packet.
     */
    private MQTTPacket readPacket() throws IOException, MQTTParseException {
        byte[] buffer = new byte[1024];
        if (mTransport.read(buffer) > 0) {
            return MQTTPacket.parse(buffer);
        }
        return null;
    }

}
