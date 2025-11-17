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
        
        TRYING = new NonInviteServerTransactionStateTrying(getId(), this,
                logger);
        state = TRYING;
        PROCEEDING = new NonInviteServerTransactionStateProceeding(getId(),
                this, logger);
        COMPLETED = new NonInviteServerTransactionStateCompleted(getId(), this,
                logger);
        TERMINATED = new NonInviteServerTransactionStateTerminated(getId(),
                this, logger);
        
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
    
}
