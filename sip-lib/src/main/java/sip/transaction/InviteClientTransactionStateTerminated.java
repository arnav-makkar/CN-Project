// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;

public class InviteClientTransactionStateTerminated extends
        InviteClientTransactionState {

    public InviteClientTransactionStateTerminated(String id,
            InviteClientTransaction inviteClientTransaction, Logger logger) {
        super(id, inviteClientTransaction, logger);
    }

}
