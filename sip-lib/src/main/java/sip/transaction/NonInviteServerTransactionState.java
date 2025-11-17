// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;
import sip.AbstractState;

//17.2.2
public abstract class NonInviteServerTransactionState extends AbstractState {

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

