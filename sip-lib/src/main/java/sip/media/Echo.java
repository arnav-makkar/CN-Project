// Peers SIP Softphone - GPL v3 License

package sip.media;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import sip.Logger;

public class Echo implements Runnable {

    public static final int BUFFER_SIZE = 2048;

    private DatagramSocket datagramSocket;
    private InetAddress remoteAddress;
    private int remotePort;
    private boolean isRunning;
    private Logger logger;

    public Echo(DatagramSocket datagramSocket,
            String remoteAddress, int remotePort, Logger logger)
            throws UnknownHostException {
        this.datagramSocket = datagramSocket;
        this.remoteAddress = InetAddress.getByName(remoteAddress);
        this.remotePort = remotePort;
        this.logger = logger;
        isRunning = true;
    }

    @Override
    public void run() {
        try {
            while (isRunning) {
                byte[] buf = new byte[BUFFER_SIZE];
                DatagramPacket datagramPacket = new DatagramPacket(buf,
                        buf.length);
                try {
                    datagramSocket.receive(datagramPacket);
                } catch (SocketTimeoutException e) {
                    logger.debug("echo socket timeout");
                    continue;
                }
                datagramPacket = new DatagramPacket(buf,
                        datagramPacket.getLength(), remoteAddress, remotePort);
                datagramSocket.send(datagramPacket);
            }
        } catch (IOException e) {
            logger.error("input/output error", e);
        } finally {
            datagramSocket.close();
        }

    }

    public synchronized void stop() {
        isRunning = false;
    }
}
