// Peers SIP Softphone - GPL v3 License

package sip.transport;

import static sip.RFC3261.DEFAULT_SIP_VERSION;
import static sip.RFC3261.IPV4_TTL;
import static sip.RFC3261.PARAM_MADDR;
import static sip.RFC3261.PARAM_TTL;
import static sip.RFC3261.TRANSPORT_DEFAULT_PORT;
import static sip.RFC3261.TRANSPORT_PORT_SEP;
import static sip.RFC3261.TRANSPORT_SCTP;
import static sip.RFC3261.TRANSPORT_TCP;
import static sip.RFC3261.TRANSPORT_TLS_PORT;
import static sip.RFC3261.TRANSPORT_UDP;
import static sip.RFC3261.TRANSPORT_UDP_USUAL_MAX_SIZE;
import static sip.RFC3261.TRANSPORT_VIA_SEP;
import static sip.RFC3261.TRANSPORT_VIA_SEP2;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import sip.Config;
import sip.Logger;
import sip.RFC3261;
import sip.Utils;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaderParamName;
import sip.syntaxencoding.SipHeaders;
import sip.syntaxencoding.SipParser;
import sip.transaction.TransactionManager;


public class TransportManager {

    public static final int SOCKET_TIMEOUT = RFC3261.TIMER_T1;

    private static int NO_TTL = -1;
    
    private Logger logger;

    //private UAS uas;
    private SipServerTransportUser sipServerTransportUser;
    
    protected SipParser sipParser;
    
    private Hashtable<SipTransportConnection, DatagramSocket> datagramSockets;
    private Hashtable<SipTransportConnection, MessageSender> messageSenders;
    private Hashtable<SipTransportConnection, MessageReceiver> messageReceivers;

    private TransactionManager transactionManager;

    private Config config;
    private int sipPort;

    public TransportManager(TransactionManager transactionManager,
            Config config, Logger logger) {
        sipParser = new SipParser();
        datagramSockets = new Hashtable<SipTransportConnection, DatagramSocket>();
        messageSenders = new Hashtable<SipTransportConnection, MessageSender>();
        messageReceivers = new Hashtable<SipTransportConnection, MessageReceiver>();
        this.transactionManager = transactionManager;
        this.config = config;
        this.logger = logger;
    }
    
    public MessageSender createClientTransport(SipRequest sipRequest,
            InetAddress inetAddress, int port, String transport)
                throws IOException {
        return createClientTransport(sipRequest, inetAddress, port, transport,
                NO_TTL);
    }
    
    public MessageSender createClientTransport(SipRequest sipRequest,
            InetAddress inetAddress, int port, String transport, int ttl)
                throws IOException {
        //18.1
        
        //via created by transaction layer to add branchid
        SipHeaderFieldValue via = Utils.getTopVia(sipRequest);
        StringBuffer buf = new StringBuffer(DEFAULT_SIP_VERSION);
        buf.append(TRANSPORT_VIA_SEP);
        boolean isMessageRequest = RFC3261.METHOD_MESSAGE.equalsIgnoreCase(sipRequest.getMethod());
        if (!isMessageRequest && sipRequest.toString().getBytes().length > TRANSPORT_UDP_USUAL_MAX_SIZE) {
            transport = TRANSPORT_TCP;
        }
        buf.append(transport);
        if (inetAddress.isMulticastAddress()) {
            SipHeaderParamName maddrName = new SipHeaderParamName(PARAM_MADDR);
            via.addParam(maddrName, inetAddress.getHostAddress());
            if (inetAddress instanceof Inet4Address) {
                SipHeaderParamName ttlName = new SipHeaderParamName(PARAM_TTL);
                via.addParam(ttlName, IPV4_TTL);
            }
        }
        //RFC3581
        
        via.addParam(new SipHeaderParamName(RFC3261.PARAM_RPORT), "");

        buf.append(TRANSPORT_VIA_SEP2);//space
        
        
        
        InetAddress myAddress = config.getPublicInetAddress();
        if (myAddress == null) {
            myAddress = config.getLocalInetAddress();
        }

        buf.append(myAddress.getHostAddress()); 
        buf.append(TRANSPORT_PORT_SEP);
        

        if (sipPort < 1) {
            //use default port
            if (TRANSPORT_TCP.equals(transport) || TRANSPORT_UDP.equals(transport)
                    || TRANSPORT_SCTP.equals(transport)) {
                sipPort = TRANSPORT_DEFAULT_PORT;
            } else if (TRANSPORT_SCTP.equals(transport)) {
                sipPort = TRANSPORT_TLS_PORT;
            } else {
                throw new RuntimeException("unknown transport type");
            }
        }
        buf.append(sipPort);
        
        
        via.setValue(buf.toString());
        
        SipTransportConnection connection = new SipTransportConnection(
                config.getLocalInetAddress(), sipPort, inetAddress, port,
                transport);

        MessageSender messageSender = messageSenders.get(connection);
        if (messageSender == null) {
            messageSender = createMessageSender(connection);
        }
        return messageSender;
    }
    
    private String threadName(int port) {
        return getClass().getSimpleName() + " " + port;
    }
    
    public void createServerTransport(String transportType, int port)
            throws SocketException {
        SipTransportConnection conn = new SipTransportConnection(
                    config.getLocalInetAddress(), port, null,
                    SipTransportConnection.EMPTY_PORT, transportType);
        
        MessageReceiver messageReceiver = messageReceivers.get(conn);
        if (messageReceiver == null) {
            messageReceiver = createMessageReceiver(conn);
            new Thread(messageReceiver, threadName(port)).start();
        }
        if (!messageReceiver.isListening()) {
            new Thread(messageReceiver, threadName(port)).start();
        }
    }
    
    public void sendResponse(SipResponse sipResponse) throws IOException {
        //18.2.2
        SipHeaderFieldValue topVia = Utils.getTopVia(sipResponse);
        String topViaValue = topVia.getValue();
        StringBuffer buf = new StringBuffer(topViaValue);
        String hostport = null;
        int i = topViaValue.length() - 1;
        while (i > 0) {
            char c = buf.charAt(i);
            if (c == ' ' || c == '\t') {
                hostport = buf.substring(i + 1);
                break;
            }
            --i;
        }
        if (hostport == null) {
            throw new RuntimeException("host or ip address not found in top via");
        }
        String host;
        int port;
        int colonPos = hostport.indexOf(RFC3261.TRANSPORT_PORT_SEP);
        if (colonPos > -1) {
            host = hostport.substring(0, colonPos);
            port = Integer.parseInt(
                    hostport.substring(colonPos + 1, hostport.length()));
        } else {
            host = hostport;
            port = RFC3261.TRANSPORT_DEFAULT_PORT;
        }
        
        String transport;
        if (buf.indexOf(RFC3261.TRANSPORT_TCP) > -1) {
            transport = RFC3261.TRANSPORT_TCP;
        } else if (buf.indexOf(RFC3261.TRANSPORT_UDP) > -1) {
            transport = RFC3261.TRANSPORT_UDP;
        } else {
            logger.error("no transport found in top via header," +
                    " discarding response");
            return;
        }
        
        String received =
            topVia.getParam(new SipHeaderParamName(RFC3261.PARAM_RECEIVED));
        if (received != null) {
            host = received;
        }
        //RFC3581
        
        String rport = topVia.getParam(new SipHeaderParamName(
                RFC3261.PARAM_RPORT));
        if (rport != null && !"".equals(rport.trim())) {
            port = Integer.parseInt(rport);
        }
        SipTransportConnection connection;
        try {
            connection = new SipTransportConnection(config.getLocalInetAddress(),
                    sipPort, InetAddress.getByName(host),
                    port, transport);
        } catch (UnknownHostException e) {
            logger.error("unknwon host", e);
            return;
        }
        
        //actual sending
        
        
        if (buf.indexOf(RFC3261.TRANSPORT_TCP) > -1) {
//            Socket socket = (Socket)factory.connections.get(connection);
//            if (!socket.isClosed()) {
//                try {
//                    socket.getOutputStream().write(data);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return;
//                    
//                }
//            } else {
//                try {
//                    socket = new Socket(host, port);
//                    factory.connections.put(connection, socket);
//                    socket.getOutputStream().write(data);
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                    /*
//                     * TODO
//                     * If connection attempt fails, use the procedures in RFC3263
//                     * for servers in order to determine the IP address and
//                     * port to open the connection and send the response to.
//                     */
//                    return;
//                }
//            }
        } else {
            MessageSender messageSender = messageSenders.get(connection);
            if (messageSender == null) {
                messageSender = createMessageSender(connection);
            }
            //add contact header
            SipHeaderFieldName contactName = new SipHeaderFieldName(RFC3261.HDR_CONTACT);
            SipHeaders respHeaders = sipResponse.getSipHeaders();
            StringBuffer contactBuf = new StringBuffer();
            contactBuf.append(RFC3261.LEFT_ANGLE_BRACKET);
            contactBuf.append(RFC3261.SIP_SCHEME);
            contactBuf.append(RFC3261.SCHEME_SEPARATOR);
            contactBuf.append(messageSender.getContact());
            contactBuf.append(RFC3261.RIGHT_ANGLE_BRACKET);
            respHeaders.add(contactName, new SipHeaderFieldValue(contactBuf.toString()));
            messageSender.sendMessage(sipResponse);

        }
        
        
    }
    
    private MessageSender createMessageSender(final SipTransportConnection conn)
            throws IOException {
        MessageSender messageSender = null;
        Object socket = null;
        if (RFC3261.TRANSPORT_UDP.equalsIgnoreCase(conn.getTransport())) {
            
            DatagramSocket datagramSocket = datagramSockets.get(conn);
            if (datagramSocket == null) {
                logger.debug("new DatagramSocket(" + conn.getLocalPort()
                        + ", " + conn.getLocalInetAddress() + ")");
                // AccessController.doPrivileged added for plugin compatibility
                datagramSocket = AccessController.doPrivileged(
                    new PrivilegedAction<DatagramSocket>() {

                        @Override
                        public DatagramSocket run() {
                            try {
                                return new DatagramSocket(conn.getLocalPort(),
                                        conn.getLocalInetAddress());
                            } catch (SocketException e) {
                                logger.error("cannot create socket", e);
                            } catch (SecurityException e) {
                                logger.error("security exception", e);
                            }
                            return null;
                        }
                    }
                );
                if (datagramSocket == null) {
                    throw new SocketException();
                }
                datagramSocket.setSoTimeout(SOCKET_TIMEOUT);
                datagramSockets.put(conn, datagramSocket);
                logger.info("added datagram socket " + conn);
            }
            socket = datagramSocket;
            messageSender = new UdpMessageSender(conn.getRemoteInetAddress(),
                    conn.getRemotePort(), datagramSocket, config, logger);
        } else {
            // TODO
            // messageReceiver = new TcpMessageReceiver(port);
        }
        messageSenders.put(conn, messageSender);
        //when a mesage is sent over a transport, the transport layer
        //must also be able to receive messages on this transport
        
//        MessageReceiver messageReceiver =
//            createMessageReceiver(conn, socket);
        MessageReceiver messageReceiver = messageReceivers.get(conn);
        if (messageReceiver == null) {
        	messageReceiver = createMessageReceiver(conn, socket);
        	new Thread(messageReceiver, threadName(conn.getLocalPort())).start();
        }
//        if (RFC3261.TRANSPORT_UDP.equalsIgnoreCase(conn.getTransport())) {
//            messageSender = new UdpMessageSender(conn.getRemoteInetAddress(),
//                    conn.getRemotePort(), (DatagramSocket)socket, config, logger);
//            messageSenders.put(conn, messageSender);
//        }
        return messageSender;
    }
    
    private MessageReceiver createMessageReceiver(SipTransportConnection conn,
            Object socket) throws IOException {
        MessageReceiver messageReceiver = null;
        if (RFC3261.TRANSPORT_UDP.equalsIgnoreCase(conn.getTransport())) {
            DatagramSocket datagramSocket = (DatagramSocket)socket;
            messageReceiver = new UdpMessageReceiver(datagramSocket,
                    transactionManager, this, config, logger);
            messageReceiver.setSipServerTransportUser(sipServerTransportUser);
        }
        messageReceivers.put(conn, messageReceiver);
        return messageReceiver;
    }
    
    private MessageReceiver createMessageReceiver(final SipTransportConnection conn)
            throws SocketException {
        MessageReceiver messageReceiver = null;
        SipTransportConnection sipTransportConnection = conn;
        if (RFC3261.TRANSPORT_UDP.equals(conn.getTransport())) {
            DatagramSocket datagramSocket = datagramSockets.get(conn);
            if (datagramSocket == null) {
                logger.debug("new DatagramSocket(" + conn.getLocalPort()
                        + ", " + conn.getLocalInetAddress());
                // AccessController.doPrivileged added for plugin compatibility
                datagramSocket = AccessController.doPrivileged(
                        new PrivilegedAction<DatagramSocket>() {

                            @Override
                            public DatagramSocket run() {
                                try {
                                    return new DatagramSocket(conn.getLocalPort(),
                                            conn.getLocalInetAddress());
                                } catch (SocketException e) {
                                    logger.error("cannot create socket", e);
                                } catch (SecurityException e) {
                                    logger.error("security exception", e);
                                }
                                return null;
                            }
                        }
                    );
                datagramSocket.setSoTimeout(SOCKET_TIMEOUT);
                if (conn.getLocalPort() == 0) {
                    sipTransportConnection = new SipTransportConnection(
                            conn.getLocalInetAddress(),
                            datagramSocket.getLocalPort(),
                            conn.getRemoteInetAddress(),
                            conn.getRemotePort(),
                            conn.getTransport());
                    //config.setSipPort(datagramSocket.getLocalPort());
                }
                sipPort = datagramSocket.getLocalPort();
                datagramSockets.put(sipTransportConnection, datagramSocket);
                logger.info("added datagram socket " + sipTransportConnection);
            }
            messageReceiver = new UdpMessageReceiver(datagramSocket,
                    transactionManager, this, config, logger);
            messageReceiver.setSipServerTransportUser(sipServerTransportUser);
            
        } else {
            
            //messageReceiver = new TcpMessageReceiver(port);
        }
        messageReceivers.put(sipTransportConnection, messageReceiver);
        logger.info("added " + sipTransportConnection + ": " + messageReceiver
                + " to message receivers");
        return messageReceiver;
    }

    public void setSipServerTransportUser(
            SipServerTransportUser sipServerTransportUser) {
        this.sipServerTransportUser = sipServerTransportUser;
    }

    public void closeTransports() {
        for (MessageReceiver messageReceiver: messageReceivers.values()) {
            messageReceiver.setListening(false);
        }
        for (MessageSender messageSender: messageSenders.values()) {
            messageSender.stopKeepAlives();
        }
        try
		{
			Thread.sleep(SOCKET_TIMEOUT);
		}
		catch (InterruptedException e)
		{
			return;
		}
        // AccessController.doPrivileged added for plugin compatibility
        AccessController.doPrivileged(
            new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    for (DatagramSocket datagramSocket: datagramSockets.values()) {
                        datagramSocket.close();
                    }
                    return null;
                }
            }
        );

		datagramSockets.clear();
		messageReceivers.clear();
		messageSenders.clear();
    }

    public MessageSender getMessageSender(
            SipTransportConnection sipTransportConnection) {
        return messageSenders.get(sipTransportConnection);
    }

    public int getSipPort() {
        return sipPort;
    }

    public void setSipPort(int sipPort) {
        this.sipPort = sipPort;
    }

}
