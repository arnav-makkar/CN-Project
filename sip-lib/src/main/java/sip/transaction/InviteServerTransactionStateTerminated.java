// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;

public class InviteServerTransactionStateTerminated extends
        InviteServerTransactionState {

    public InviteServerTransactionStateTerminated(String id,
            InviteServerTransaction inviteServerTransaction, Logger logger) {
        super(id, inviteServerTransaction, logger);
    }

}
