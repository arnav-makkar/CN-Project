// Peers SIP Softphone - GPL v3 License

package sip.gui;

import javax.swing.JPanel;

import sip.Logger;
import sip.AbstractState;
import sip.transport.SipResponse;

public abstract class CallFrameState extends AbstractState {

    protected CallFrame callFrame;
    protected JPanel callPanel;

    public CallFrameState(String id, CallFrame callFrame, Logger logger) {
        super(id, logger);
        this.callFrame = callFrame;
    }

    public void callClicked() {}
    public void incomingCall() {}
    public void calleePickup() {}
    public void error(SipResponse sipResponse) {}
    public void pickupClicked() {}
    public void busyHereClicked() {}
    public void hangupClicked() {}
    public void remoteHangup() {}
    public void closeClicked() {}
    public void ringing() {}
    
}
