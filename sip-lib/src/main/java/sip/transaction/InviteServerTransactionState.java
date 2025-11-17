// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;
import sip.AbstractState;

public abstract class InviteServerTransactionState extends AbstractState {

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
