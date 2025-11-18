// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import sip.Logger;
import sip.RFC3261;
import sip.Utils;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaderParamName;
import sip.syntaxencoding.SipHeaders;
import sip.transport.MessageSender;
import sip.transport.SipClientTransportUser;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.TransportManager;


public class InviteClientTransaction extends InviteTransaction
        implements ClientTransaction, SipClientTransportUser {

    public final InviteClientTransactionState INIT;
    public final InviteClientTransactionState CALLING;
    public final InviteClientTransactionState PROCEEDING;
    public final InviteClientTransactionState COMPLETED;
    public final InviteClientTransactionState TERMINATED;

    protected ClientTransactionUser transactionUser;
    protected String transport;
    
    private InviteClientTransactionState state;
    //private SipClientTransport sipClientTransport;
    private MessageSender messageSender;
    private int nbRetrans;
    private SipRequest ack;
    private int remotePort;
    private InetAddress remoteInetAddress;
    
    InviteClientTransaction(String branchId, InetAddress inetAddress,
            int port, String transport, SipRequest sipRequest,
            ClientTransactionUser transactionUser, Timer timer,
            TransportManager transportManager,
            TransactionManager transactionManager, Logger logger) {
        super(branchId, timer, transportManager, transactionManager,
                logger);
        
        this.transport = transport;
        
        SipHeaderFieldValue via = new SipHeaderFieldValue("");
        via.addParam(new SipHeaderParamName(RFC3261.PARAM_BRANCH), branchId);
        sipRequest.getSipHeaders().add(new SipHeaderFieldName(RFC3261.HDR_VIA), via, 0);
        
        nbRetrans = 0;
        
        INIT = new StateInit(getId(), this, logger);
        state = INIT;
        CALLING = new StateCalling(getId(), this, logger);
        PROCEEDING = new StateProceeding(getId(), this, logger);
        COMPLETED = new StateCompleted(getId(), this, logger);
        TERMINATED = new StateTerminated(getId(), this, logger);

        //17.1.1.2
        
        request = sipRequest;
        this.transactionUser = transactionUser;
        
        remotePort = port;
        remoteInetAddress = inetAddress;
        
        try {
            messageSender = transportManager.createClientTransport(
                    request, remoteInetAddress, remotePort, transport);
        } catch (IOException e) {
            logger.error("input/output error", e);
            transportError();
        }

    }
    
    public void setState(InviteClientTransactionState state) {
        this.state.log(state);
        this.state = state;
        if(TERMINATED.equals(state)) {
            //transactionManager.removeClientTransaction(branchId, method);
            transactionManager = null;
        }
    }
    
    public void start() {
        state.start();
        //send request using transport information and sipRequest
//        try {
//            sipClientTransport = SipTransportFactory.getInstance()
//                .createClientTransport(this, request, remoteInetAddress,
//                        remotePort, transport);
//            sipClientTransport.send(request);
//        } catch (IOException e) {
//            //e.printStackTrace();
//            transportError();
//        }
        
        try {
            messageSender.sendMessage(request);
        } catch (IOException e) {
            logger.error("input/output error", e);
            transportError();
        }
        logger.debug("InviteClientTransaction.start");
        
        if (RFC3261.TRANSPORT_UDP.equals(transport)) {
            //start timer A with value T1 for retransmission
            timer.schedule(new TimerA(), RFC3261.TIMER_T1);
        }
        
        
        timer.schedule(new TimerB(), 64 * RFC3261.TIMER_T1);
    }
    
    public synchronized void receivedResponse(SipResponse sipResponse) {
        responses.add(sipResponse);
        // 17.1.1
        int statusCode = sipResponse.getStatusCode();
        if (statusCode < RFC3261.CODE_MIN_PROV) {
            logger.error("invalid response code");
        } else if (statusCode < RFC3261.CODE_MIN_SUCCESS) {
            state.received1xx();
        } else if (statusCode < RFC3261.CODE_MIN_REDIR) {
            state.received2xx();
        } else if (statusCode <= RFC3261.CODE_MAX) {
            state.received300To699();
        } else {
            logger.error("invalid response code");
        }
    }
    
    public void transportError() {
        state.transportError();
    }
    
    void createAndSendAck() {
        
        //p.126 last paragraph
        
        //17.1.1.3
        ack = new SipRequest(RFC3261.METHOD_ACK, request.getRequestUri());
        SipHeaderFieldValue topVia = Utils.getTopVia(request);
        SipHeaders ackSipHeaders = ack.getSipHeaders();
        ackSipHeaders.add(new SipHeaderFieldName(RFC3261.HDR_VIA), topVia);
        Utils.copyHeader(request, ack, RFC3261.HDR_CALLID);
        Utils.copyHeader(request, ack, RFC3261.HDR_FROM);
        Utils.copyHeader(getLastResponse(), ack, RFC3261.HDR_TO);
        
        SipHeaders requestSipHeaders = request.getSipHeaders();
        SipHeaderFieldName cseqName = new SipHeaderFieldName(RFC3261.HDR_CSEQ);
        SipHeaderFieldValue cseq = requestSipHeaders.get(cseqName);
        cseq.setValue(cseq.toString().replace(RFC3261.METHOD_INVITE, RFC3261.METHOD_ACK));
        ackSipHeaders.add(cseqName, cseq);
        Utils.copyHeader(request, ack, RFC3261.HDR_ROUTE);
        
        sendAck();
    }
    
    void sendAck() {
        //ack is passed to the transport layer...
        
        //sipClientTransport.send(ack);
        try {
            messageSender.sendMessage(ack);
        } catch (IOException e) {
            logger.error("input/output error", e);
            transportError();
        }
    }
    
    void sendRetrans() {
        ++nbRetrans;
        //sipClientTransport.send(request);
        try {
            messageSender.sendMessage(request);
        } catch (IOException e) {
            logger.error("input/output error", e);
            transportError();
        }
        timer.schedule(new TimerA(), (long)Math.pow(2, nbRetrans) * RFC3261.TIMER_T1);
    }
    
    public void requestTransportError(SipRequest sipRequest, Exception e) {
        // TODO Auto-generated method stub
        
    }

    public void responseTransportError(Exception e) {
        // TODO Auto-generated method stub
        
    }
    
    class TimerA extends TimerTask {
        @Override
        public void run() {
            state.timerAFires();
        }
    }
    
    class TimerB extends TimerTask {
        @Override
        public void run() {
            state.timerBFires();
        }
    }
    
    class TimerD extends TimerTask {
        @Override
        public void run() {
            state.timerDFires();
        }
    }

    public String getContact() {
        if (messageSender != null) {
            return messageSender.getContact();
        }
        return null;
    }

    // ========== Inner State Classes ==========
    
    abstract class InviteClientTransactionState extends sip.AbstractState {
        protected InviteClientTransaction inviteClientTransaction;
        
        public InviteClientTransactionState(String id,
                InviteClientTransaction inviteClientTransaction, Logger logger) {
            super(id, logger);
            this.inviteClientTransaction = inviteClientTransaction;
        }
        
        public void start() {}
        public void timerAFires() {}
        public void timerBFires() {}
        public void received2xx() {}
        public void received1xx() {}
        public void received300To699() {}
        public void transportError() {}
        public void timerDFires() {}
    }
    
    class StateInit extends InviteClientTransactionState {
        public StateInit(String id, InviteClientTransaction inviteClientTransaction, Logger logger) {
            super(id, inviteClientTransaction, logger);
        }

        @Override
        public void start() {
            InviteClientTransactionState nextState = inviteClientTransaction.CALLING;
            inviteClientTransaction.setState(nextState);
        }
    }
    
    class StateCalling extends InviteClientTransactionState {
        public StateCalling(String id, InviteClientTransaction inviteClientTransaction, Logger logger) {
            super(id, inviteClientTransaction, logger);
        }

        @Override
        public void timerAFires() {
            InviteClientTransactionState nextState = inviteClientTransaction.CALLING;
            inviteClientTransaction.setState(nextState);
            inviteClientTransaction.sendRetrans();
        }
        
        @Override
        public void timerBFires() {
            timerBFiresOrTransportError();
        }
        
        @Override
        public void transportError() {
            timerBFiresOrTransportError();
        }
        
        private void timerBFiresOrTransportError() {
            InviteClientTransactionState nextState = inviteClientTransaction.TERMINATED;
            inviteClientTransaction.setState(nextState);
            inviteClientTransaction.transactionUser.transactionTimeout(
                    inviteClientTransaction);
        }
        
        @Override
        public void received2xx() {
            InviteClientTransactionState nextState = inviteClientTransaction.TERMINATED;
            inviteClientTransaction.setState(nextState);
            inviteClientTransaction.transactionUser.successResponseReceived(
                    inviteClientTransaction.getLastResponse(), inviteClientTransaction);
        }
        
        @Override
        public void received1xx() {
            InviteClientTransactionState nextState = inviteClientTransaction.PROCEEDING;
            inviteClientTransaction.setState(nextState);
            inviteClientTransaction.transactionUser.provResponseReceived(
                    inviteClientTransaction.getLastResponse(), inviteClientTransaction);
        }
        
        @Override
        public void received300To699() {
            InviteClientTransactionState nextState = inviteClientTransaction.COMPLETED;
            inviteClientTransaction.setState(nextState);
            inviteClientTransaction.createAndSendAck();
            inviteClientTransaction.transactionUser.errResponseReceived(
                    inviteClientTransaction.getLastResponse());
        }
    }
    
    class StateProceeding extends InviteClientTransactionState {
        public StateProceeding(String id, InviteClientTransaction inviteClientTransaction, Logger logger) {
            super(id, inviteClientTransaction, logger);
        }

        @Override
        public void received1xx() {
            InviteClientTransactionState nextState = inviteClientTransaction.PROCEEDING;
            inviteClientTransaction.setState(nextState);
            inviteClientTransaction.transactionUser.provResponseReceived(
                    inviteClientTransaction.getLastResponse(), inviteClientTransaction);
        }
        
        @Override
        public void received2xx() {
            InviteClientTransactionState nextState = inviteClientTransaction.TERMINATED;
            inviteClientTransaction.setState(nextState);
            inviteClientTransaction.transactionUser.successResponseReceived(
                    inviteClientTransaction.getLastResponse(), inviteClientTransaction);
        }
        
        @Override
        public void received300To699() {
            InviteClientTransactionState nextState = inviteClientTransaction.COMPLETED;
            inviteClientTransaction.setState(nextState);
            inviteClientTransaction.createAndSendAck();
            inviteClientTransaction.transactionUser.errResponseReceived(
                    inviteClientTransaction.getLastResponse());
        }
    }
    
    class StateCompleted extends InviteClientTransactionState {
        public StateCompleted(String id, InviteClientTransaction inviteClientTransaction, Logger logger) {
            super(id, inviteClientTransaction, logger);
            int delay = 0;
            if (RFC3261.TRANSPORT_UDP.equals(inviteClientTransaction.transport)) {
                delay = RFC3261.TIMER_INVITE_CLIENT_TRANSACTION;
            }
            inviteClientTransaction.timer.schedule(inviteClientTransaction.new TimerD(), delay);
        }

        @Override
        public void received300To699() {
            InviteClientTransactionState nextState = inviteClientTransaction.COMPLETED;
            inviteClientTransaction.setState(nextState);
            inviteClientTransaction.sendAck();
        }
        
        @Override
        public void transportError() {
            InviteClientTransactionState nextState = inviteClientTransaction.TERMINATED;
            inviteClientTransaction.setState(nextState);
            inviteClientTransaction.transactionUser.transactionTransportError();
        }
        
        @Override
        public void timerDFires() {
            InviteClientTransactionState nextState = inviteClientTransaction.TERMINATED;
            inviteClientTransaction.setState(nextState);
        }
    }
    
    class StateTerminated extends InviteClientTransactionState {
        public StateTerminated(String id, InviteClientTransaction inviteClientTransaction, Logger logger) {
            super(id, inviteClientTransaction, logger);
        }
    }

}
