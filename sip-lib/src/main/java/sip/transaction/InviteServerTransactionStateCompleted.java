// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;
import sip.RFC3261;

public class InviteServerTransactionStateCompleted extends
        InviteServerTransactionState {

    public InviteServerTransactionStateCompleted(String id,
            InviteServerTransaction inviteServerTransaction, Logger logger) {
        super(id, inviteServerTransaction, logger);
    }

    @Override
    public void timerGFires() {
        InviteServerTransactionState nextState = inviteServerTransaction.COMPLETED;
        inviteServerTransaction.setState(nextState);
        inviteServerTransaction.sendLastResponse();
        long delay = (long)Math.pow(2,
                ++inviteServerTransaction.nbRetrans) * RFC3261.TIMER_T1;
        inviteServerTransaction.timer.schedule(
                inviteServerTransaction.new TimerG(),
                Math.min(delay, RFC3261.TIMER_T2));
    }
    
    @Override
    public void timerHFiresOrTransportError() {
        InviteServerTransactionState nextState = inviteServerTransaction.TERMINATED;
        inviteServerTransaction.setState(nextState);
        inviteServerTransaction.serverTransactionUser.transactionFailure();
    }
    
    @Override
    public void receivedAck() {
        InviteServerTransactionState nextState = inviteServerTransaction.CONFIRMED;
        inviteServerTransaction.setState(nextState);
        int delay;
        if (RFC3261.TRANSPORT_UDP.equals(inviteServerTransaction.transport)) {
            delay = RFC3261.TIMER_T4;
        } else {
            delay = 0;
        }
        inviteServerTransaction.timer.schedule(
                inviteServerTransaction.new TimerI(), delay);
    }
    
    @Override
    public void receivedInvite() {
        InviteServerTransactionState nextState = inviteServerTransaction.COMPLETED;
        inviteServerTransaction.setState(nextState);
        // retransmission
        inviteServerTransaction.sendLastResponse();
    }
    
}
