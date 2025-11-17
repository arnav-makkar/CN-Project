// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;

public class NonInviteServerTransactionStateTrying extends
        NonInviteServerTransactionState {

    public NonInviteServerTransactionStateTrying(String id,
            NonInviteServerTransaction nonInviteServerTransaction,
            Logger logger) {
        super(id, nonInviteServerTransaction, logger);
    }

    @Override
    public void received1xx() {
        NonInviteServerTransactionState nextState =
            nonInviteServerTransaction.PROCEEDING;
        nonInviteServerTransaction.setState(nextState);
        nonInviteServerTransaction.sendLastResponse();
    }
    
    @Override
    public void received200To699() {
        NonInviteServerTransactionState nextState =
            nonInviteServerTransaction.COMPLETED;
        nonInviteServerTransaction.setState(nextState);
    }
    
}
