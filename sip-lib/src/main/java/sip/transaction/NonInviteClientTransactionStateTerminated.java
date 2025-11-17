// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;

public class NonInviteClientTransactionStateTerminated extends
        NonInviteClientTransactionState {

    public NonInviteClientTransactionStateTerminated(String id,
            NonInviteClientTransaction nonInviteClientTransaction,
            Logger logger) {
        super(id, nonInviteClientTransaction, logger);
    }

}
