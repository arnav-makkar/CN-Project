// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.transport.SipRequest;
import sip.transport.SipResponse;

public interface ServerTransaction {

    public void start();
    
    public void receivedRequest(SipRequest sipRequest);
    
    public void sendReponse(SipResponse sipResponse);
}
