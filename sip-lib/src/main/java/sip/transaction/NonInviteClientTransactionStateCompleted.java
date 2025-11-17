// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;
import sip.RFC3261;

public class NonInviteClientTransactionStateCompleted extends
        NonInviteClientTransactionState {

    public NonInviteClientTransactionStateCompleted(String id,
            NonInviteClientTransaction nonInviteClientTransaction,
            Logger logger) {
        super(id, nonInviteClientTransaction, logger);
        int delay = 0;
        if (RFC3261.TRANSPORT_UDP.equals(
                nonInviteClientTransaction.transport)) {
            delay = RFC3261.TIMER_T4;
        }
        nonInviteClientTransaction.timer.schedule(
                nonInviteClientTransaction.new TimerK(), delay);
    }

    @Override
    public void timerKFires() {
        NonInviteClientTransactionState nextState =
            nonInviteClientTransaction.TERMINATED;
        nonInviteClientTransaction.setState(nextState);
    }
    
}
