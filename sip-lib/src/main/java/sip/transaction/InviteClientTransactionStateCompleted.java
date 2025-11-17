// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;
import sip.RFC3261;

public class InviteClientTransactionStateCompleted extends
        InviteClientTransactionState {

    public InviteClientTransactionStateCompleted(String id,
            InviteClientTransaction inviteClientTransaction, Logger logger) {
        super(id, inviteClientTransaction, logger);
        int delay = 0;
        if (RFC3261.TRANSPORT_UDP.equals(inviteClientTransaction.transport)) {
            delay = RFC3261.TIMER_INVITE_CLIENT_TRANSACTION;
        }
        inviteClientTransaction.timer.schedule(inviteClientTransaction.new TimerD(), delay);
    }

    @Override
    public void received300To699() {
        InviteClientTransactionState nextState = inviteClientTransaction.COMPLETED;
        inviteClientTransaction.setState(nextState);
        inviteClientTransaction.sendAck();
    }
    
    @Override
    public void transportError() {
        InviteClientTransactionState nextState = inviteClientTransaction.TERMINATED;
        inviteClientTransaction.setState(nextState);
        inviteClientTransaction.transactionUser.transactionTransportError();
    }
    
    @Override
    public void timerDFires() {
        InviteClientTransactionState nextState = inviteClientTransaction.TERMINATED;
        inviteClientTransaction.setState(nextState);
    }
    
    
}
