// Peers SIP Softphone - GPL v3 License

package sip.core.useragent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import sip.Logger;
import sip.RFC3261;
import sip.Utils;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaderParamName;
import sip.syntaxencoding.SipHeaders;
import sip.syntaxencoding.SipUriSyntaxException;
import sip.transaction.ClientTransaction;
import sip.transaction.ClientTransactionUser;
import sip.transaction.InviteClientTransaction;
import sip.transaction.Transaction;
import sip.transaction.TransactionManager;
import sip.transactionuser.Dialog;
import sip.transactionuser.DialogManager;
import sip.transactionuser.DialogState;
import sip.transport.SipMessage;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.TransportManager;

public class UAC {
    
    private InitialRequestManager initialRequestManager;
    private MidDialogRequestManager midDialogRequestManager;

    private String registerCallID;
    private String profileUri;
    
    
    private UserAgent userAgent;
    private TransactionManager transactionManager;
    private DialogManager dialogManager;
    private List<String> guiClosedCallIds;
    private Logger logger;
    
    /**
     * should be instanciated only once, it was a singleton.
     */
    public UAC(UserAgent userAgent,
            InitialRequestManager initialRequestManager,
            MidDialogRequestManager midDialogRequestManager,
            DialogManager dialogManager,
            TransactionManager transactionManager,
            TransportManager transportManager,
            Logger logger) {
        this.userAgent = userAgent;
        this.initialRequestManager = initialRequestManager;
        this.midDialogRequestManager = midDialogRequestManager;
        this.dialogManager = dialogManager;
        this.transactionManager = transactionManager;
        this.logger = logger;
        guiClosedCallIds = Collections.synchronizedList(new ArrayList<String>());
        profileUri = RFC3261.SIP_SCHEME + RFC3261.SCHEME_SEPARATOR
            + userAgent.getUserpart() + RFC3261.AT + userAgent.getDomain();
    }

    /**
     * For the moment we consider that only one profile uri is used at a time.
     * @throws SipUriSyntaxException 
     */
    SipRequest register() throws SipUriSyntaxException {
        String domain = userAgent.getDomain();
        String requestUri = RFC3261.SIP_SCHEME + RFC3261.SCHEME_SEPARATOR
            + domain;
        SipListener sipListener = userAgent.getSipListener();
        profileUri = RFC3261.SIP_SCHEME + RFC3261.SCHEME_SEPARATOR
        	+ userAgent.getUserpart() + RFC3261.AT + domain;
        registerCallID = Utils.generateCallID(
                userAgent.getConfig().getLocalInetAddress());
        SipRequest sipRequest = initialRequestManager.createInitialRequest(
                requestUri, RFC3261.METHOD_REGISTER, profileUri,
                registerCallID);
        if (sipListener != null) {
            sipListener.registering(sipRequest);
        }
        return sipRequest;
    }
    
    void unregister() throws SipUriSyntaxException {
        if (getInitialRequestManager().getRegisterHandler().isRegistered()) {
            String requestUri = RFC3261.SIP_SCHEME + RFC3261.SCHEME_SEPARATOR
                + userAgent.getDomain();
            MessageInterceptor messageInterceptor = new MessageInterceptor() {
                
                @Override
                public void postProcess(SipMessage sipMessage) {
                    initialRequestManager.registerHandler.unregister();
                    SipHeaders sipHeaders = sipMessage.getSipHeaders();
                    SipHeaderFieldValue contact = sipHeaders.get(
                            new SipHeaderFieldName(RFC3261.HDR_CONTACT));
                    contact.addParam(new SipHeaderParamName(RFC3261.PARAM_EXPIRES),
                            "0");
                }
                
            };
            // for any reason, asterisk requires a new Call-ID to unregister
            registerCallID = Utils.generateCallID(
                    userAgent.getConfig().getLocalInetAddress());
            initialRequestManager.createInitialRequest(requestUri,
                    RFC3261.METHOD_REGISTER, profileUri, registerCallID, null,
                    messageInterceptor);
        }
    }
    
    SipRequest invite(String requestUri, String callId)
            throws SipUriSyntaxException {
        return initialRequestManager.createInitialRequest(requestUri,
                RFC3261.METHOD_INVITE, profileUri, callId);
        
    }
    
    SipRequest sendMessage(String toUri, String messageText, byte[] attachment,
            String attachmentName, String contentType) throws SipUriSyntaxException {
        String callId = Utils.generateCallID(userAgent.getConfig().getLocalInetAddress());
        SipRequest sipRequest = initialRequestManager.getGenericRequest(
                toUri, RFC3261.METHOD_MESSAGE, profileUri, callId, null);
        
        // Set message body
        if (attachment != null && attachment.length > 0) {
            // Multipart message with attachment
            String boundary = "----Boundary" + System.currentTimeMillis();
            StringBuilder body = new StringBuilder();
            
            // Text part (only if text is not empty)
            if (messageText != null && !messageText.trim().isEmpty()) {
                body.append("--").append(boundary).append("\r\n");
                body.append("Content-Type: text/plain\r\n");
                body.append("\r\n");
                body.append(messageText).append("\r\n");
                body.append("\r\n");
            }
            
            // Attachment part
            body.append("--").append(boundary).append("\r\n");
            body.append("Content-Type: ").append(contentType != null ? contentType : "application/octet-stream").append("\r\n");
            body.append("Content-Disposition: attachment; filename=\"").append(attachmentName != null ? attachmentName : "attachment").append("\"\r\n");
            body.append("Content-Transfer-Encoding: base64\r\n");
            body.append("\r\n");
            body.append(java.util.Base64.getEncoder().encodeToString(attachment)).append("\r\n");
            body.append("--").append(boundary).append("--\r\n");
            
            sipRequest.setBody(body.toString().getBytes());
            SipHeaders headers = sipRequest.getSipHeaders();
            headers.add(new SipHeaderFieldName(RFC3261.HDR_CONTENT_TYPE),
                    new SipHeaderFieldValue("multipart/mixed; boundary=\"" + boundary + "\""));
        } else {
            // Text-only message
            if (messageText != null && !messageText.isEmpty()) {
                sipRequest.setBody(messageText.getBytes());
                SipHeaders headers = sipRequest.getSipHeaders();
                headers.add(new SipHeaderFieldName(RFC3261.HDR_CONTENT_TYPE),
                        new SipHeaderFieldValue("text/plain"));
            } else {
                // Empty message - set empty body
                sipRequest.setBody(new byte[0]);
                SipHeaders headers = sipRequest.getSipHeaders();
                headers.add(new SipHeaderFieldName(RFC3261.HDR_CONTENT_TYPE),
                        new SipHeaderFieldValue("text/plain"));
            }
        }
        
        // Create and send the MESSAGE request via transaction
        // Use static method like other handlers (InviteHandler, RegisterHandler)
        sip.syntaxencoding.SipURI destinationUri = 
            sip.core.useragent.RequestManager.getDestinationUri(sipRequest, logger);
        
        if (destinationUri == null) {
            throw new SipUriSyntaxException("Destination URI is null");
        }
        
        int port = destinationUri.getPort();
        if (port == sip.syntaxencoding.SipURI.DEFAULT_PORT) {
            port = sip.RFC3261.TRANSPORT_DEFAULT_PORT;
        }
        
        String transport = sip.RFC3261.TRANSPORT_UDP;
        java.util.Hashtable<String, String> params = destinationUri.getUriParameters();
        if (params != null) {
            String reqUriTransport = params.get(sip.RFC3261.PARAM_TRANSPORT);
            if (reqUriTransport != null) {
                transport = reqUriTransport;
            }
        }
        
        // Check for outbound proxy (like InviteHandler and RegisterHandler do)
        sip.syntaxencoding.SipURI sipUri = userAgent.getConfig().getOutboundProxy();
        if (sipUri == null) {
            sipUri = destinationUri;
        }
        
        String host = sipUri.getHost();
        if (host == null || host.isEmpty()) {
            throw new SipUriSyntaxException("Destination host is null or empty");
        }
        
        java.net.InetAddress inetAddress;
        try {
            inetAddress = java.net.InetAddress.getByName(host);
        } catch (java.net.UnknownHostException e) {
            throw new SipUriSyntaxException("unknown host: " + host, e);
        }
        
        if (transactionManager == null) {
            throw new SipUriSyntaxException("TransactionManager is null");
        }
        
        ClientTransaction clientTransaction = transactionManager.createClientTransaction(
                sipRequest, inetAddress, port, transport, null, new MessageClientTransactionUser(logger));
        
        if (clientTransaction == null) {
            throw new SipUriSyntaxException("Failed to create client transaction");
        }
        
        clientTransaction.start();
        
        return sipRequest;
    }

    private static class MessageClientTransactionUser implements ClientTransactionUser {

        private final Logger logger;

        MessageClientTransactionUser(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void transactionTimeout(ClientTransaction clientTransaction) {
            logger.error("MESSAGE transaction timeout for " + safeTransactionInfo(clientTransaction));
        }

        @Override
        public void provResponseReceived(SipResponse sipResponse, Transaction transaction) {
            logger.debug("MESSAGE provisional response: " + sipResponse.getStatusCode());
        }

        @Override
        public void errResponseReceived(SipResponse sipResponse) {
            logger.error("MESSAGE error response: " + sipResponse.getStatusCode());
        }

        @Override
        public void successResponseReceived(SipResponse sipResponse, Transaction transaction) {
            logger.debug("MESSAGE success response: " + sipResponse.getStatusCode());
        }

        @Override
        public void transactionTransportError() {
            logger.error("MESSAGE transport error");
        }

        private String safeTransactionInfo(ClientTransaction clientTransaction) {
            if (clientTransaction == null) {
                return "null transaction";
            }
            return clientTransaction.toString();
        }
    }

    private SipRequest getInviteWithAuth(String callId) {
        List<ClientTransaction> clientTransactions =
            transactionManager.getClientTransactionsFromCallId(callId,
                    RFC3261.METHOD_INVITE);
        SipRequest sipRequestNoAuth = null;
        for (ClientTransaction clientTransaction: clientTransactions) {
            InviteClientTransaction inviteClientTransaction =
                (InviteClientTransaction)clientTransaction;
            SipRequest sipRequest = inviteClientTransaction.getRequest();
            SipHeaders sipHeaders = sipRequest.getSipHeaders();
            SipHeaderFieldName authorization = new SipHeaderFieldName(
                    RFC3261.HDR_AUTHORIZATION);
            SipHeaderFieldValue value = sipHeaders.get(authorization);
            if (value == null) {
                SipHeaderFieldName proxyAuthorization = new SipHeaderFieldName(
                        RFC3261.HDR_PROXY_AUTHORIZATION);
                value = sipHeaders.get(proxyAuthorization);
            }
            if (value != null) {
                return sipRequest;
            }
            sipRequestNoAuth = sipRequest;
        }
        return sipRequestNoAuth;
    }

    void terminate(SipRequest sipRequest) {
        String callId = Utils.getMessageCallId(sipRequest);
        if (!guiClosedCallIds.contains(callId)) {
            guiClosedCallIds.add(callId);
        }
        Dialog dialog = dialogManager.getDialog(callId);
        SipRequest inviteWithAuth = getInviteWithAuth(callId);
        if (dialog != null) {
            SipRequest originatingRequest;
            if (inviteWithAuth != null) {
                originatingRequest = inviteWithAuth;
            } else {
                originatingRequest = sipRequest;
            }
            ClientTransaction clientTransaction =
                transactionManager.getClientTransaction(originatingRequest);
            if (clientTransaction != null) {
                synchronized (clientTransaction) {
                    DialogState dialogState = dialog.getState();
                    if (dialog.EARLY.equals(dialogState)) {
                        initialRequestManager.createCancel(inviteWithAuth,
                                midDialogRequestManager, profileUri);
                    } else if (dialog.CONFIRMED.equals(dialogState)) {
                        // clientTransaction not yet removed
                        midDialogRequestManager.generateMidDialogRequest(
                                dialog, RFC3261.METHOD_BYE, null);
                        guiClosedCallIds.remove(callId);
                    }
                }
            } else {
                // clientTransaction Terminated and removed
                logger.debug("clientTransaction null");
                midDialogRequestManager.generateMidDialogRequest(
                        dialog, RFC3261.METHOD_BYE, null);
                guiClosedCallIds.remove(callId);
            }
        } else {
            InviteClientTransaction inviteClientTransaction =
                (InviteClientTransaction)transactionManager
                    .getClientTransaction(inviteWithAuth);
            if (inviteClientTransaction == null) {
                logger.error("cannot find invite client transaction" +
                        " for call " + callId);
            } else {
                SipResponse sipResponse =
                    inviteClientTransaction.getLastResponse();
                if (sipResponse != null) {
                    int statusCode = sipResponse.getStatusCode();
                    if (statusCode < RFC3261.CODE_200_OK) {
                        initialRequestManager.createCancel(inviteWithAuth,
                                midDialogRequestManager, profileUri);
                    }
                }
            }
        }
        userAgent.getMediaManager().stopSession();
    }

    public InitialRequestManager getInitialRequestManager() {
        return initialRequestManager;
    }

    public List<String> getGuiClosedCallIds() {
        return guiClosedCallIds;
    }

}
