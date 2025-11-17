// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;

public class InviteClientTransactionStateProceeding extends
        InviteClientTransactionState {

    public InviteClientTransactionStateProceeding(String id,
            InviteClientTransaction inviteClientTransaction, Logger logger) {
        super(id, inviteClientTransaction, logger);
    }

    @Override
    public void received1xx() {
        InviteClientTransactionState nextState = inviteClientTransaction.PROCEEDING;
        inviteClientTransaction.setState(nextState);
        inviteClientTransaction.transactionUser.provResponseReceived(
                inviteClientTransaction.getLastResponse(), inviteClientTransaction);
    }
    
    @Override
    public void received2xx() {
        InviteClientTransactionState nextState = inviteClientTransaction.TERMINATED;
        inviteClientTransaction.setState(nextState);
        inviteClientTransaction.transactionUser.successResponseReceived(
                inviteClientTransaction.getLastResponse(), inviteClientTransaction);
    }
    
    @Override
    public void received300To699() {
        InviteClientTransactionState nextState = inviteClientTransaction.COMPLETED;
        inviteClientTransaction.setState(nextState);
        inviteClientTransaction.createAndSendAck();
        inviteClientTransaction.transactionUser.errResponseReceived(
                inviteClientTransaction.getLastResponse());
    }
    
    
}
