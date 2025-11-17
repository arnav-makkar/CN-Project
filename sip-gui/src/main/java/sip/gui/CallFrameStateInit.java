// Peers SIP Softphone - GPL v3 License

package sip.gui;

import javax.swing.JFrame;

import sip.Logger;

public class CallFrameStateInit extends CallFrameState {

    public CallFrameStateInit(String id, CallFrame callFrame, Logger logger) {
        super(id, callFrame, logger);
    }

    @Override
    public void callClicked() {
        callFrame.setState(callFrame.UAC);
        JFrame frame = callFrame.getFrame();
        callFrame.setCallPanel(callFrame.UAC.callPanel);
        frame.setVisible(true);
    }

    @Override
    public void incomingCall() {
        callFrame.setState(callFrame.UAS);
        JFrame frame = callFrame.getFrame();
        callFrame.setCallPanel(callFrame.UAS.callPanel);
        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();
        frame.setAlwaysOnTop(true);
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

}
