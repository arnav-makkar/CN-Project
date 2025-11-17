// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import sip.transport.SipResponse;

public interface ClientTransaction {

    public void receivedResponse(SipResponse sipResponse);
    public void start();
    public String getContact();
}
