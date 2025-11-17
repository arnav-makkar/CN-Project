// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;
import sip.RFC3261;

public class NonInviteServerTransactionStateProceeding extends
        NonInviteServerTransactionState {

    public NonInviteServerTransactionStateProceeding(String id,
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
        nonInviteServerTransaction.sendLastResponse();
        int timeout;
        if (RFC3261.TRANSPORT_UDP.equals(
                nonInviteServerTransaction.transport)) {
            timeout = 64 * RFC3261.TIMER_T1;
        } else {
            timeout = 0;
        }
        nonInviteServerTransaction.timer.schedule(
                nonInviteServerTransaction.new TimerJ(), timeout);
    }
    
    @Override
    public void transportError() {
        NonInviteServerTransactionState nextState =
            nonInviteServerTransaction.TERMINATED;
        nonInviteServerTransaction.setState(nextState);
    }
    
    @Override
    public void receivedRequest() {
        NonInviteServerTransactionState nextState =
            nonInviteServerTransaction.PROCEEDING;
        nonInviteServerTransaction.setState(nextState);
    }
    
}
