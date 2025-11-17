// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;
import sip.RFC3261;

public class InviteServerTransactionStateProceeding extends
        InviteServerTransactionState {

    public InviteServerTransactionStateProceeding(String id,
            InviteServerTransaction inviteServerTransaction, Logger logger) {
        super(id, inviteServerTransaction, logger);
    }

    @Override
    public void received101To199() {
        InviteServerTransactionState nextState = inviteServerTransaction.PROCEEDING;
        inviteServerTransaction.setState(nextState);
        
        inviteServerTransaction.sendLastResponse();
    }
    
    @Override
    public void transportError() {
        InviteServerTransactionState nextState = inviteServerTransaction.TERMINATED;
        inviteServerTransaction.setState(nextState);
    }
    
    @Override
    public void received2xx() {
        InviteServerTransactionState nextState = inviteServerTransaction.TERMINATED;
        inviteServerTransaction.setState(nextState);
        inviteServerTransaction.sendLastResponse();
    }
    
    @Override
    public void received300To699() {
        InviteServerTransactionState nextState = inviteServerTransaction.COMPLETED;
        inviteServerTransaction.setState(nextState);
        inviteServerTransaction.sendLastResponse();
        if (RFC3261.TRANSPORT_UDP.equals(inviteServerTransaction.transport)) {
            inviteServerTransaction.timer.schedule(
                    inviteServerTransaction.new TimerG(), RFC3261.TIMER_T1);
        }
        inviteServerTransaction.timer.schedule(
                inviteServerTransaction.new TimerH(), 64 * RFC3261.TIMER_T1);
    }
    
    @Override
    public void receivedInvite() {
        InviteServerTransactionState nextState = inviteServerTransaction.PROCEEDING;
        inviteServerTransaction.setState(nextState);
    }
    
    
}
