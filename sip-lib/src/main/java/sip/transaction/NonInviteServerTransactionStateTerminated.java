// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;

public class NonInviteServerTransactionStateTerminated extends
        NonInviteServerTransactionState {

    public NonInviteServerTransactionStateTerminated(String id,
            NonInviteServerTransaction nonInviteServerTransaction,
            Logger logger) {
        super(id, nonInviteServerTransaction, logger);
    }

}
