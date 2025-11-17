// Peers SIP Softphone - GPL v3 License


package sip.core.useragent;

import sip.transport.SipRequest;
import sip.transport.SipResponse;

public interface SipListener {

    public void registering(SipRequest sipRequest);

    public void registerSuccessful(SipResponse sipResponse);

    public void registerFailed(SipResponse sipResponse);

    public void incomingCall(SipRequest sipRequest, SipResponse provResponse);

    public void remoteHangup(SipRequest sipRequest);

    public void ringing(SipResponse sipResponse);

    public void calleePickup(SipResponse sipResponse);

    public void error(SipResponse sipResponse);

    public void receivedDtmf(char digit);

    public void messageReceived(SipRequest sipRequest, String messageText, 
            byte[] attachment, String attachmentName, String contentType);

}
