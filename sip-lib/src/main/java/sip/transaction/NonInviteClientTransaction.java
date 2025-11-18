// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import sip.Logger;
import sip.RFC3261;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaderParamName;
import sip.transport.MessageSender;
import sip.transport.SipClientTransportUser;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.TransportManager;


public class NonInviteClientTransaction extends NonInviteTransaction
        implements ClientTransaction, SipClientTransportUser {

    public final NonInviteClientTransactionState INIT;
    public final NonInviteClientTransactionState TRYING;
    public final NonInviteClientTransactionState PROCEEDING;
    public final NonInviteClientTransactionState COMPLETED;
    public final NonInviteClientTransactionState TERMINATED;
    
    protected ClientTransactionUser transactionUser;
    protected String transport;
    protected int nbRetrans;
    
    private NonInviteClientTransactionState state;
    //private SipClientTransport sipClientTransport;
    private MessageSender messageSender;
    private int remotePort;
    private InetAddress remoteInetAddress;
    
    NonInviteClientTransaction(String branchId, InetAddress inetAddress,
            int port, String transport, SipRequest sipRequest,
            ClientTransactionUser transactionUser, Timer timer,
            TransportManager transportManager,
            TransactionManager transactionManager, Logger logger) {
        super(branchId, sipRequest.getMethod(), timer, transportManager,
                transactionManager, logger);
        
        this.transport = transport;
        
        SipHeaderFieldValue via = new SipHeaderFieldValue("");
        via.addParam(new SipHeaderParamName(RFC3261.PARAM_BRANCH), branchId);
        sipRequest.getSipHeaders().add(new SipHeaderFieldName(RFC3261.HDR_VIA), via, 0);
        
        nbRetrans = 0;
        
        INIT = new StateInit(getId(), this, logger);
        state = INIT;
        TRYING = new StateTrying(getId(), this, logger);
        PROCEEDING = new StateProceeding(getId(), this, logger);
        COMPLETED = new StateCompleted(getId(), this, logger);
        TERMINATED = new StateTerminated(getId(), this, logger);
        
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
    
    public void setState(NonInviteClientTransactionState state) {
        this.state.log(state);
        this.state = state;
    }

    public void start() {
        state.start();
        
        //17.1.2.2
        
//        try {
//            sipClientTransport = SipTransportFactory.getInstance()
//                    .createClientTransport(this, request, remoteInetAddress,
//                            remotePort, transport);
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
        
        if (RFC3261.TRANSPORT_UDP.equals(transport)) {
            //start timer E with value T1 for retransmission
            timer.schedule(new TimerE(), RFC3261.TIMER_T1);
        }
    
        timer.schedule(new TimerF(), 64 * RFC3261.TIMER_T1);
    }
    
    void sendRetrans(long delay) {
        //sipClientTransport.send(request);
        try {
            messageSender.sendMessage(request);
        } catch (IOException e) {
            logger.error("input/output error", e);
            transportError();
        }
        timer.schedule(new TimerE(), delay);
    }
    
    public void transportError() {
        state.transportError();
    }
    
    public synchronized void receivedResponse(SipResponse sipResponse) {
        responses.add(sipResponse);
        // 17.1.1
        int statusCode = sipResponse.getStatusCode();
        if (statusCode < RFC3261.CODE_MIN_PROV) {
            logger.error("invalid response code");
        } else if (statusCode < RFC3261.CODE_MIN_SUCCESS) {
            state.received1xx();
        } else if (statusCode <= RFC3261.CODE_MAX) {
            state.received200To699();
        } else {
            logger.error("invalid response code");
        }
    }
    
    public void requestTransportError(SipRequest sipRequest, Exception e) {
        // TODO Auto-generated method stub
        
    }

    public void responseTransportError(Exception e) {
        // TODO Auto-generated method stub
        
    }
    
    class TimerE extends TimerTask {
        @Override
        public void run() {
            state.timerEFires();
        }
    }
    
    class TimerF extends TimerTask {
        @Override
        public void run() {
            state.timerFFires();
        }
    }
    
    class TimerK extends TimerTask {
        @Override
        public void run() {
            state.timerKFires();
        }
    }

    public String getContact() {
        if (messageSender != null) {
            return messageSender.getContact();
        }
        return null;
    }

    // ========== Inner State Classes ==========
    
    abstract class NonInviteClientTransactionState extends sip.AbstractState {
        protected NonInviteClientTransaction nonInviteClientTransaction;
        
        public NonInviteClientTransactionState(String id,
                NonInviteClientTransaction nonInviteClientTransaction,
                Logger logger) {
            super(id, logger);
            this.nonInviteClientTransaction = nonInviteClientTransaction;
        }
        
        public void start() {}
        public void timerEFires() {}
        public void timerFFires() {}
        public void transportError() {}
        public void received1xx() {}
        public void received200To699() {}
        public void timerKFires() {}
    }
    
    class StateInit extends NonInviteClientTransactionState {
        public StateInit(String id, NonInviteClientTransaction nonInviteClientTransaction, Logger logger) {
            super(id, nonInviteClientTransaction, logger);
        }

        @Override
        public void start() {
            NonInviteClientTransactionState nextState = nonInviteClientTransaction.TRYING;
            nonInviteClientTransaction.setState(nextState);
        }
    }
    
    class StateTrying extends NonInviteClientTransactionState {
        public StateTrying(String id, NonInviteClientTransaction nonInviteClientTransaction, Logger logger) {
            super(id, nonInviteClientTransaction, logger);
        }

        @Override
        public void timerEFires() {
            NonInviteClientTransactionState nextState = nonInviteClientTransaction.TRYING;
            nonInviteClientTransaction.setState(nextState);
            long delay = (long)Math.pow(2,
                    ++nonInviteClientTransaction.nbRetrans) * RFC3261.TIMER_T1;
            nonInviteClientTransaction.sendRetrans(Math.min(delay, RFC3261.TIMER_T2));
        }

        @Override
        public void timerFFires() {
            timerFFiresOrTransportError();
        }
        
        @Override
        public void transportError() {
            timerFFiresOrTransportError();
        }
        
        private void timerFFiresOrTransportError() {
            NonInviteClientTransactionState nextState = nonInviteClientTransaction.TERMINATED;
            nonInviteClientTransaction.setState(nextState);
            nonInviteClientTransaction.transactionUser.transactionTimeout(
                    nonInviteClientTransaction);
        }
        
        @Override
        public void received1xx() {
            NonInviteClientTransactionState nextState = nonInviteClientTransaction.PROCEEDING;
            nonInviteClientTransaction.setState(nextState);
            nonInviteClientTransaction.transactionUser.provResponseReceived(
                    nonInviteClientTransaction.getLastResponse(), nonInviteClientTransaction);
        }
        
        @Override
        public void received200To699() {
            NonInviteClientTransactionState nextState = nonInviteClientTransaction.COMPLETED;
            nonInviteClientTransaction.setState(nextState);
            SipResponse response = nonInviteClientTransaction.getLastResponse();
            int code = response.getStatusCode();
            if (code < RFC3261.CODE_MIN_REDIR) {
                nonInviteClientTransaction.transactionUser.successResponseReceived(
                        response, nonInviteClientTransaction);
            } else {
                nonInviteClientTransaction.transactionUser.errResponseReceived(
                        response);
            }
        }
    }
    
    class StateProceeding extends NonInviteClientTransactionState {
        public StateProceeding(String id, NonInviteClientTransaction nonInviteClientTransaction, Logger logger) {
            super(id, nonInviteClientTransaction, logger);
        }

        @Override
        public void timerEFires() {
            NonInviteClientTransactionState nextState = nonInviteClientTransaction.PROCEEDING;
            nonInviteClientTransaction.setState(nextState);
            ++nonInviteClientTransaction.nbRetrans;
            nonInviteClientTransaction.sendRetrans(RFC3261.TIMER_T2);
        }
        
        @Override
        public void timerFFires() {
            timerFFiresOrTransportError();
        }
        
        @Override
        public void transportError() {
            timerFFiresOrTransportError();
        }
        
        private void timerFFiresOrTransportError() {
            NonInviteClientTransactionState nextState = nonInviteClientTransaction.TERMINATED;
            nonInviteClientTransaction.setState(nextState);
            nonInviteClientTransaction.transactionUser.transactionTimeout(
                    nonInviteClientTransaction);
        }
        
        @Override
        public void received1xx() {
            NonInviteClientTransactionState nextState = nonInviteClientTransaction.PROCEEDING;
            nonInviteClientTransaction.setState(nextState);
        }
        
        @Override
        public void received200To699() {
            NonInviteClientTransactionState nextState = nonInviteClientTransaction.COMPLETED;
            nonInviteClientTransaction.setState(nextState);
            SipResponse response = nonInviteClientTransaction.getLastResponse();
            int code = response.getStatusCode();
            if (code < RFC3261.CODE_MIN_REDIR) {
                nonInviteClientTransaction.transactionUser.successResponseReceived(
                        response, nonInviteClientTransaction);
            } else {
                nonInviteClientTransaction.transactionUser.errResponseReceived(
                        response);
            }
        }
    }
    
    class StateCompleted extends NonInviteClientTransactionState {
        public StateCompleted(String id, NonInviteClientTransaction nonInviteClientTransaction, Logger logger) {
            super(id, nonInviteClientTransaction, logger);
            int delay = 0;
            if (RFC3261.TRANSPORT_UDP.equals(
                    nonInviteClientTransaction.transport)) {
                delay = RFC3261.TIMER_T4;
            }
            nonInviteClientTransaction.timer.schedule(
                    nonInviteClientTransaction.new TimerK(), delay);
        }

        @Override
        public void timerKFires() {
            NonInviteClientTransactionState nextState = nonInviteClientTransaction.TERMINATED;
            nonInviteClientTransaction.setState(nextState);
        }
    }
    
    class StateTerminated extends NonInviteClientTransactionState {
        public StateTerminated(String id, NonInviteClientTransaction nonInviteClientTransaction, Logger logger) {
            super(id, nonInviteClientTransaction, logger);
        }
    }
}
