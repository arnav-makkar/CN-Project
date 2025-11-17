// Peers SIP Softphone - GPL v3 License

package sip.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sip.Config;
import sip.Logger;
import sip.RFC3261;


public class UdpMessageSender extends MessageSender {

    private DatagramSocket datagramSocket;
    
    public UdpMessageSender(InetAddress inetAddress, int port,
            DatagramSocket datagramSocket, Config config,
            Logger logger) throws SocketException {
        super(datagramSocket.getLocalPort(), inetAddress, port,
                config, RFC3261.TRANSPORT_UDP, logger);
        this.datagramSocket = datagramSocket;
    }

    @Override
    public synchronized void sendMessage(SipMessage sipMessage) throws IOException {
        logger.debug("UdpMessageSender.sendMessage");
        if (sipMessage == null) {
            return;
        }
        byte[] buf = sipMessage.toString().getBytes();
        sendBytes(buf);
        StringBuffer direction = new StringBuffer();
        direction.append("SENT to ").append(inetAddress.getHostAddress());
        direction.append("/").append(port);
        logger.traceNetwork(new String(buf), direction.toString());
    }

    @Override
    public synchronized void sendBytes(byte[] bytes) throws IOException {
        logger.debug("UdpMessageSender.sendBytes");
        final DatagramPacket packet = new DatagramPacket(bytes, bytes.length,
                inetAddress, port);
        logger.debug("UdpMessageSender.sendBytes " + bytes.length
                + " " + inetAddress + ":" + port);
        // AccessController.doPrivileged added for plugin compatibility
        AccessController.doPrivileged(
            new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    try {
                        logger.debug(datagramSocket.getLocalAddress().toString());
                        datagramSocket.send(packet);
                    } catch (Throwable t) {
                        logger.error("throwable", new Exception(t));
                    }
                    return null;
                }
            }
        );

        logger.debug("UdpMessageSender.sendBytes packet sent");
    }

}
