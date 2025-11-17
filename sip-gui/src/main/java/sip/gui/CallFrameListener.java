// Peers SIP Softphone - GPL v3 License

package sip.gui;

import sip.transport.SipRequest;

public interface CallFrameListener {

    public void hangupClicked(SipRequest sipRequest);
    public void pickupClicked(SipRequest sipRequest);
    public void busyHereClicked(SipRequest sipRequest);
    public void dtmf(char digit);

}
