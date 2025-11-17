// Peers SIP Softphone - GPL v3 License

package sip.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sip.Config;
import sip.Logger;
import sip.RFC3261;
import sip.transaction.TransactionManager;


public class UdpMessageReceiver extends MessageReceiver {

    private DatagramSocket datagramSocket;
    
    public UdpMessageReceiver(DatagramSocket datagramSocket,
            TransactionManager transactionManager,
            TransportManager transportManager, Config config,
            Logger logger)
            throws SocketException {
        super(datagramSocket.getLocalPort(), transactionManager,
                transportManager, config, logger);
        this.datagramSocket = datagramSocket;
    }

    @Override
    protected void listen() throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        final DatagramPacket packet = new DatagramPacket(buf, buf.length);
        final int noException = 0;
        final int socketTimeoutException = 1;
        final int ioException = 2;
        // AccessController.doPrivileged added for plugin compatibility
        int result = AccessController.doPrivileged(
            new PrivilegedAction<Integer>() {
                public Integer run() {
                    try {
                        datagramSocket.receive(packet);
                    } catch (SocketTimeoutException e) {
                        return socketTimeoutException;
                    } catch (IOException e) {
                        logger.error("cannot receive packet", e);
                        return ioException;
                    }
                    return noException;
                }
            });
        switch (result) {
        case socketTimeoutException:
            return;
        case ioException:
            throw new IOException();
        case noException:
            break;
        default:
            break;
        }
        byte[] trimmedPacket = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), 0,
                trimmedPacket, 0, trimmedPacket.length);
        processMessage(trimmedPacket, packet.getAddress(),
                packet.getPort(), RFC3261.TRANSPORT_UDP);
    }


}
