// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;

public class NonInviteServerTransactionStateCompleted extends
        NonInviteServerTransactionState {

    public NonInviteServerTransactionStateCompleted(String id,
            NonInviteServerTransaction nonInviteServerTransaction,
            Logger logger) {
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
