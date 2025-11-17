// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import java.util.Timer;

import sip.Logger;
import sip.RFC3261;
import sip.transport.TransportManager;

public abstract class InviteTransaction extends Transaction {
    
    protected InviteTransaction(String branchId, Timer timer,
            TransportManager transportManager,
            TransactionManager transactionManager, Logger logger) {
        super(branchId, RFC3261.METHOD_INVITE, timer, transportManager,
                transactionManager, logger);
    }

}
