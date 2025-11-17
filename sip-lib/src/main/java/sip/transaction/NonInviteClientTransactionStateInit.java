// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;

public class NonInviteClientTransactionStateInit extends
        NonInviteClientTransactionState {

    public NonInviteClientTransactionStateInit(String id,
            NonInviteClientTransaction nonInviteClientTransaction,
            Logger logger) {
        super(id, nonInviteClientTransaction, logger);
    }

    @Override
    public void start() {
        NonInviteClientTransactionState nextState = nonInviteClientTransaction.TRYING;
        nonInviteClientTransaction.setState(nextState);
    }
}
