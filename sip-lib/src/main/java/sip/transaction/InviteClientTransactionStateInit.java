// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;

public class InviteClientTransactionStateInit extends InviteClientTransactionState {

    public InviteClientTransactionStateInit(String id,
            InviteClientTransaction inviteClientTransaction, Logger logger) {
        super(id, inviteClientTransaction, logger);
    }

    @Override
    public void start() {
        InviteClientTransactionState nextState = inviteClientTransaction.CALLING;
        inviteClientTransaction.setState(nextState);
    }
    
}
