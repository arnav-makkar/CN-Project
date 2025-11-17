// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;

public class InviteServerTransactionStateInit extends
        InviteServerTransactionState {

    public InviteServerTransactionStateInit(String id,
            InviteServerTransaction inviteServerTransaction, Logger logger) {
        super(id, inviteServerTransaction, logger);
    }

    @Override
    public void start() {
        InviteServerTransactionState nextState = inviteServerTransaction.PROCEEDING;
        inviteServerTransaction.setState(nextState);
    }
}
