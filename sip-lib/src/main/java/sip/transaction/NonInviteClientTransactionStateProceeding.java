// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.Logger;
import sip.RFC3261;
import sip.transport.SipResponse;

public class NonInviteClientTransactionStateProceeding extends
        NonInviteClientTransactionState {

    public NonInviteClientTransactionStateProceeding(String id,
            NonInviteClientTransaction nonInviteClientTransaction,
            Logger logger) {
        super(id, nonInviteClientTransaction, logger);
    }

    @Override
    public void timerEFires() {
        NonInviteClientTransactionState nextState = nonInviteClientTransaction.PROCEEDING;
        nonInviteClientTransaction.setState(nextState);
        ++nonInviteClientTransaction.nbRetrans;
        nonInviteClientTransaction.sendRetrans(RFC3261.TIMER_T2);
    }
    
    @Override
    public void timerFFires() {
        timerFFiresOrTransportError();
    }
    
    @Override
    public void transportError() {
        timerFFiresOrTransportError();
    }
    
    private void timerFFiresOrTransportError() {
        NonInviteClientTransactionState nextState = nonInviteClientTransaction.TERMINATED;
        nonInviteClientTransaction.setState(nextState);
        nonInviteClientTransaction.transactionUser.transactionTimeout(
                nonInviteClientTransaction);
    }
    
    @Override
    public void received1xx() {
        NonInviteClientTransactionState nextState = nonInviteClientTransaction.PROCEEDING;
        nonInviteClientTransaction.setState(nextState);
    }
    
    @Override
    public void received200To699() {
        NonInviteClientTransactionState nextState = nonInviteClientTransaction.COMPLETED;
        nonInviteClientTransaction.setState(nextState);
        SipResponse response = nonInviteClientTransaction.getLastResponse();
        int code = response.getStatusCode();
        if (code < RFC3261.CODE_MIN_REDIR) {
            nonInviteClientTransaction.transactionUser.successResponseReceived(
                    response, nonInviteClientTransaction);
        } else {
            nonInviteClientTransaction.transactionUser.errResponseReceived(
                    response);
        }
    }
    
}
