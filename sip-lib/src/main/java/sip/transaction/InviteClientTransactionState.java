// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;
import sip.AbstractState;


public abstract class InviteClientTransactionState extends AbstractState {

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
