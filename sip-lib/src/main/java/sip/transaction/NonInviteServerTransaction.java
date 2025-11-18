// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import sip.Logger;
import sip.RFC3261;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.TransportManager;


public class NonInviteServerTransaction extends NonInviteTransaction
        implements ServerTransaction/*, SipServerTransportUser*/ {

    public final NonInviteServerTransactionState TRYING;
    public final NonInviteServerTransactionState PROCEEDING;
    public final NonInviteServerTransactionState COMPLETED;
    public final NonInviteServerTransactionState TERMINATED;
    
    protected ServerTransactionUser serverTransactionUser;
    protected Timer timer;
    protected String transport;
    
    private NonInviteServerTransactionState state;
    //private int port;
    
    NonInviteServerTransaction(String branchId, int port, String transport,
            String method, ServerTransactionUser serverTransactionUser,
            SipRequest sipRequest, Timer timer, TransportManager transportManager,
            TransactionManager transactionManager, Logger logger) {
        super(branchId, method, timer, transportManager, transactionManager,
                logger);
        
        TRYING = new StateTrying(getId(), this, logger);
        state = TRYING;
        PROCEEDING = new StateProceeding(getId(), this, logger);
        COMPLETED = new StateCompleted(getId(), this, logger);
        TERMINATED = new StateTerminated(getId(), this, logger);
        
        //this.port = port;
        this.transport = transport;
        this.serverTransactionUser = serverTransactionUser;
        request = sipRequest;
//        sipServerTransport = SipTransportFactory.getInstance()
//            .createServerTransport(this, port, transport);
        try {
            transportManager.createServerTransport(transport, port);
        } catch (IOException e) {
            logger.error("input/output error", e);
        }
        
        
    }

    public void setState(NonInviteServerTransactionState state) {
        this.state.log(state);
        this.state = state;
    }
    
    public void receivedRequest(SipRequest sipRequest) {
        state.receivedRequest();
    }

    public void sendReponse(SipResponse sipResponse) {
        responses.add(sipResponse);
        int statusCode = sipResponse.getStatusCode();
        if (statusCode < RFC3261.CODE_200_OK) {
            state.received1xx();
        } else if (statusCode <= RFC3261.CODE_MAX) {
            state.received200To699();
        }
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
    
    public void start() {
        // TODO Auto-generated method stub
        
    }

//    public void messageReceived(SipMessage sipMessage) {
//        // TODO Auto-generated method stub
//        
//    }

    class TimerJ extends TimerTask {
        @Override
        public void run() {
            state.timerJFires();
        }
    }

    // ========== Inner State Classes ==========
    
    abstract class NonInviteServerTransactionState extends sip.AbstractState {
        protected NonInviteServerTransaction nonInviteServerTransaction;
        
        public NonInviteServerTransactionState(String id,
                NonInviteServerTransaction nonInviteServerTransaction,
                Logger logger) {
            super(id, logger);
            this.nonInviteServerTransaction = nonInviteServerTransaction;
        }

        public void received200To699() {}
        public void received1xx() {}
        public void receivedRequest() {}
        public void transportError() {}
        public void timerJFires() {}
    }
    
    class StateTrying extends NonInviteServerTransactionState {
        public StateTrying(String id, NonInviteServerTransaction nonInviteServerTransaction, Logger logger) {
            super(id, nonInviteServerTransaction, logger);
        }

        @Override
        public void received1xx() {
            NonInviteServerTransactionState nextState = nonInviteServerTransaction.PROCEEDING;
            nonInviteServerTransaction.setState(nextState);
            nonInviteServerTransaction.sendLastResponse();
        }
        
        @Override
        public void received200To699() {
            NonInviteServerTransactionState nextState = nonInviteServerTransaction.COMPLETED;
            nonInviteServerTransaction.setState(nextState);
        }
    }
    
    class StateProceeding extends NonInviteServerTransactionState {
        public StateProceeding(String id, NonInviteServerTransaction nonInviteServerTransaction, Logger logger) {
            super(id, nonInviteServerTransaction, logger);
        }

        @Override
        public void received1xx() {
            NonInviteServerTransactionState nextState = nonInviteServerTransaction.PROCEEDING;
            nonInviteServerTransaction.setState(nextState);
            nonInviteServerTransaction.sendLastResponse();
        }
        
        @Override
        public void received200To699() {
            NonInviteServerTransactionState nextState = nonInviteServerTransaction.COMPLETED;
            nonInviteServerTransaction.setState(nextState);
            nonInviteServerTransaction.sendLastResponse();
            int timeout;
            if (RFC3261.TRANSPORT_UDP.equals(
                    nonInviteServerTransaction.transport)) {
                timeout = 64 * RFC3261.TIMER_T1;
            } else {
                timeout = 0;
            }
            nonInviteServerTransaction.timer.schedule(
                    nonInviteServerTransaction.new TimerJ(), timeout);
        }
        
        @Override
        public void transportError() {
            NonInviteServerTransactionState nextState = nonInviteServerTransaction.TERMINATED;
            nonInviteServerTransaction.setState(nextState);
        }
        
        @Override
        public void receivedRequest() {
            NonInviteServerTransactionState nextState = nonInviteServerTransaction.PROCEEDING;
            nonInviteServerTransaction.setState(nextState);
        }
    }
    
    class StateCompleted extends NonInviteServerTransactionState {
        public StateCompleted(String id, NonInviteServerTransaction nonInviteServerTransaction, Logger logger) {
            super(id, nonInviteServerTransaction, logger);
        }

        @Override
        public void timerJFires() {
            NonInviteServerTransactionState nextState = nonInviteServerTransaction.TERMINATED;
            nonInviteServerTransaction.setState(nextState);
        }
        
        @Override
        public void transportError() {
            NonInviteServerTransactionState nextState = nonInviteServerTransaction.TERMINATED;
            nonInviteServerTransaction.setState(nextState);
        }
        
        @Override
        public void receivedRequest() {
            NonInviteServerTransactionState nextState = nonInviteServerTransaction.COMPLETED;
            nonInviteServerTransaction.setState(nextState);
            nonInviteServerTransaction.sendLastResponse();
        }
    }
    
    class StateTerminated extends NonInviteServerTransactionState {
        public StateTerminated(String id, NonInviteServerTransaction nonInviteServerTransaction, Logger logger) {
            super(id, nonInviteServerTransaction, logger);
        }
    }
    
}
