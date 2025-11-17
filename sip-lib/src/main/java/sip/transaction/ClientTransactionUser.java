// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.transport.SipResponse;

public interface ClientTransactionUser {
    public void transactionTimeout(ClientTransaction clientTransaction);
    public void provResponseReceived(SipResponse sipResponse, Transaction transaction);
    
    public void errResponseReceived(SipResponse sipResponse);//3XX is considered as an error response
    public void successResponseReceived(SipResponse sipResponse, Transaction transaction);
    public void transactionTransportError();
}
