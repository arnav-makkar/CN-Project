// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;
import sip.AbstractState;

public abstract class NonInviteClientTransactionState extends AbstractState {

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
