// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import sip.Logger;
import sip.RFC3261;
import sip.transport.SipMessage;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.SipServerTransportUser;
import sip.transport.TransportManager;


public class InviteServerTransaction extends InviteTransaction
        implements ServerTransaction, SipServerTransportUser {

    public final InviteServerTransactionState INIT;
    public final InviteServerTransactionState PROCEEDING;
    public final InviteServerTransactionState COMPLETED;
    public final InviteServerTransactionState CONFIRMED;
    public final InviteServerTransactionState TERMINATED;
    
    protected String transport;
    protected int nbRetrans;
    protected ServerTransactionUser serverTransactionUser;
    
    private InviteServerTransactionState state;
    //private SipServerTransport sipServerTransport;
    private int port;
    
    InviteServerTransaction(String branchId, int port, String transport,
            SipResponse sipResponse, ServerTransactionUser serverTransactionUser,
            SipRequest sipRequest, Timer timer, TransactionManager transactionManager,
            TransportManager transportManager, Logger logger) {
        super(branchId, timer, transportManager, transactionManager, logger);
        
        INIT = new StateInit(getId(), this, logger);
        state = INIT;
        PROCEEDING = new StateProceeding(getId(), this, logger);
        COMPLETED = new StateCompleted(getId(), this, logger);
        CONFIRMED = new StateConfirmed(getId(), this, logger);
        TERMINATED = new StateTerminated(getId(), this, logger);
        
        this.request = sipRequest;
        this.port = port;
        this.transport = transport;
        responses.add(sipResponse);
        nbRetrans = 0;
        this.serverTransactionUser = serverTransactionUser;
        
    }

    public void start() {
        state.start();
        
//        sipServerTransport = SipTransportFactory.getInstance()
//            .createServerTransport(this, port, transport);
        try {
            transportManager.createServerTransport(transport, port);
        } catch (IOException e) {
            logger.error("input/output error", e);
        }
    }
    
    public void receivedRequest(SipRequest sipRequest) {
        String method = sipRequest.getMethod();
        if (RFC3261.METHOD_INVITE.equals(method)) {
            state.receivedInvite();
        } else {
            // if not INVITE, we consider that a ACK is received
            // in the case the call was not successful
            state.receivedAck();
        }
        
    }

    public void sendReponse(SipResponse sipResponse) {
        
        //equal (for contains) to the first response
        if (!responses.contains(sipResponse)) {
            responses.add(sipResponse);
        }
        int statusCode = sipResponse.getStatusCode();
        if (statusCode == RFC3261.CODE_MIN_PROV) {
            // TODO 100 trying
        } else if (statusCode < RFC3261.CODE_MIN_SUCCESS) {
            state.received101To199();
        } else if (statusCode < RFC3261.CODE_MIN_REDIR) {
            state.received2xx();
        } else if (statusCode <= RFC3261.CODE_MAX) {
            state.received300To699();
        } else {
            logger.error("invalid response code");
        }
    }

    public void setState(InviteServerTransactionState state) {
        this.state.log(state);
        this.state = state;
    }

    public void messageReceived(SipMessage sipMessage) {
        // TODO Auto-generated method stub
        
    }
    
    void sendLastResponse() {
        //sipServerTransport.sendResponse(responses.get(responses.size() - 1));
        int nbOfResponses = responses.size();
        if (nbOfResponses > 0) {
            try {
                transportManager.sendResponse(responses.get(nbOfResponses - 1));
            } catch (IOException e) {
                logger.error("input/output error", e);
            }
        }
    }
    
    public SipResponse getLastResponse() {
        int nbOfResponses = responses.size();
        if (nbOfResponses > 0) {
            return responses.get(nbOfResponses - 1);
        }
        return null;
    }
    
    // TODO send provional response
    /*
     * maybe the 200 response mechanism could be retrieved for 1xx responses.
     */

// void stopSipServerTransport() {
//        sipServerTransport.stop();
//    }
    
    class TimerG extends TimerTask {
        @Override
        public void run() {
            state.timerGFires();
        }
    }
    
    class TimerH extends TimerTask {
        @Override
        public void run() {
            state.timerHFiresOrTransportError();
        }
    }
    
    class TimerI extends TimerTask {
        @Override
        public void run() {
            state.timerIFires();
        }
    }

    // ========== Inner State Classes ==========
    
    abstract class InviteServerTransactionState extends sip.AbstractState {
        protected InviteServerTransaction inviteServerTransaction;
        
        public InviteServerTransactionState(String id,
                InviteServerTransaction inviteServerTransaction, Logger logger) {
            super(id, logger);
            this.inviteServerTransaction = inviteServerTransaction;
        }

        public void start() {}
        public void receivedInvite() {}
        public void received101To199() {}
        public void transportError() {}
        public void received2xx() {}
        public void received300To699() {}
        public void timerGFires() {}
        public void timerHFiresOrTransportError() {}
        public void receivedAck() {}
        public void timerIFires() {}
    }
    
    class StateInit extends InviteServerTransactionState {
        public StateInit(String id, InviteServerTransaction inviteServerTransaction, Logger logger) {
            super(id, inviteServerTransaction, logger);
        }

        @Override
        public void start() {
            InviteServerTransactionState nextState = inviteServerTransaction.PROCEEDING;
            inviteServerTransaction.setState(nextState);
        }
    }
    
    class StateProceeding extends InviteServerTransactionState {
        public StateProceeding(String id, InviteServerTransaction inviteServerTransaction, Logger logger) {
            super(id, inviteServerTransaction, logger);
        }

        @Override
        public void received101To199() {
            InviteServerTransactionState nextState = inviteServerTransaction.PROCEEDING;
            inviteServerTransaction.setState(nextState);
            inviteServerTransaction.sendLastResponse();
        }
        
        @Override
        public void transportError() {
            InviteServerTransactionState nextState = inviteServerTransaction.TERMINATED;
            inviteServerTransaction.setState(nextState);
        }
        
        @Override
        public void received2xx() {
            InviteServerTransactionState nextState = inviteServerTransaction.TERMINATED;
            inviteServerTransaction.setState(nextState);
            inviteServerTransaction.sendLastResponse();
        }
        
        @Override
        public void received300To699() {
            InviteServerTransactionState nextState = inviteServerTransaction.COMPLETED;
            inviteServerTransaction.setState(nextState);
            inviteServerTransaction.sendLastResponse();
            if (RFC3261.TRANSPORT_UDP.equals(inviteServerTransaction.transport)) {
                inviteServerTransaction.timer.schedule(
                        inviteServerTransaction.new TimerG(), RFC3261.TIMER_T1);
            }
            inviteServerTransaction.timer.schedule(
                    inviteServerTransaction.new TimerH(), 64 * RFC3261.TIMER_T1);
        }
        
        @Override
        public void receivedInvite() {
            InviteServerTransactionState nextState = inviteServerTransaction.PROCEEDING;
            inviteServerTransaction.setState(nextState);
        }
    }
    
    class StateCompleted extends InviteServerTransactionState {
        public StateCompleted(String id, InviteServerTransaction inviteServerTransaction, Logger logger) {
            super(id, inviteServerTransaction, logger);
        }

        @Override
        public void timerGFires() {
            InviteServerTransactionState nextState = inviteServerTransaction.COMPLETED;
            inviteServerTransaction.setState(nextState);
            inviteServerTransaction.sendLastResponse();
            long delay = (long)Math.pow(2,
                    ++inviteServerTransaction.nbRetrans) * RFC3261.TIMER_T1;
            inviteServerTransaction.timer.schedule(
                    inviteServerTransaction.new TimerG(),
                    Math.min(delay, RFC3261.TIMER_T2));
        }
        
        @Override
        public void timerHFiresOrTransportError() {
            InviteServerTransactionState nextState = inviteServerTransaction.TERMINATED;
            inviteServerTransaction.setState(nextState);
            inviteServerTransaction.serverTransactionUser.transactionFailure();
        }
        
        @Override
        public void receivedAck() {
            InviteServerTransactionState nextState = inviteServerTransaction.CONFIRMED;
            inviteServerTransaction.setState(nextState);
            int delay;
            if (RFC3261.TRANSPORT_UDP.equals(inviteServerTransaction.transport)) {
                delay = RFC3261.TIMER_T4;
            } else {
                delay = 0;
            }
            inviteServerTransaction.timer.schedule(
                    inviteServerTransaction.new TimerI(), delay);
        }
        
        @Override
        public void receivedInvite() {
            InviteServerTransactionState nextState = inviteServerTransaction.COMPLETED;
            inviteServerTransaction.setState(nextState);
            inviteServerTransaction.sendLastResponse();
        }
    }
    
    class StateConfirmed extends InviteServerTransactionState {
        public StateConfirmed(String id, InviteServerTransaction inviteServerTransaction, Logger logger) {
            super(id, inviteServerTransaction, logger);
        }

        @Override
        public void timerIFires() {
            InviteServerTransactionState nextState = inviteServerTransaction.TERMINATED;
            inviteServerTransaction.setState(nextState);
            inviteServerTransaction.transactionManager.removeServerTransaction(
                    inviteServerTransaction.branchId,
                    inviteServerTransaction.method);
        }
    }
    
    class StateTerminated extends InviteServerTransactionState {
        public StateTerminated(String id, InviteServerTransaction inviteServerTransaction, Logger logger) {
            super(id, inviteServerTransaction, logger);
        }
    }

}
