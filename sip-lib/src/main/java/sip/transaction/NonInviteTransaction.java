// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import java.util.Timer;

import sip.Logger;
import sip.transport.TransportManager;

public abstract class NonInviteTransaction extends Transaction {

    protected NonInviteTransaction(String branchId, String method, Timer timer,
            TransportManager transportManager,
            TransactionManager transactionManager, Logger logger) {
        super(branchId, method, timer, transportManager, transactionManager,
                logger);
    }

}
