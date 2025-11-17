// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;

public class InviteServerTransactionStateConfirmed extends
        InviteServerTransactionState {

    public InviteServerTransactionStateConfirmed(String id,
            InviteServerTransaction inviteServerTransaction, Logger logger) {
        super(id, inviteServerTransaction, logger);
    }

    @Override
    public void timerIFires() {
        InviteServerTransactionState nextState =
            inviteServerTransaction.TERMINATED;
        inviteServerTransaction.setState(nextState);
        // TODO destroy invite server transaction immediately
        // (dereference it in transaction manager serverTransactions hashtable)
        
        inviteServerTransaction.transactionManager.removeServerTransaction(
                inviteServerTransaction.branchId,
                inviteServerTransaction.method);
    }
    
}
