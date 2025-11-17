// Peers SIP Softphone - GPL v3 License

package sip.gui;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import sip.Logger;
import sip.transport.SipResponse;

public class CallFrameStateUac extends CallFrameState {

    public CallFrameStateUac(String id, CallFrame callFrame, Logger logger) {
        super(id, callFrame, logger);
        callPanel = DarkTheme.createModernPanel();
        callPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
        
        JLabel callingLabel = DarkTheme.createTitleLabel("Calling...");
        callPanel.add(callingLabel);
        
        JButton hangupButton = DarkTheme.createHangupButton("Hangup");
        hangupButton.setActionCommand(CallFrame.HANGUP_ACTION_COMMAND);
        hangupButton.addActionListener(callFrame);
        callPanel.add(hangupButton);
    }

    @Override
    public void hangupClicked() {
        callFrame.setState(callFrame.TERMINATED);
        callFrame.hangup();
        callFrame.close();
    }

    @Override
    public void calleePickup() {
        callFrame.setState(callFrame.SUCCESS);
        callFrame.setCallPanel(callFrame.SUCCESS.callPanel);
        
        if (callFrame.getCallFrameListener() instanceof EventManager) {
            EventManager eventManager = (EventManager) callFrame.getCallFrameListener();
            eventManager.setStatisticsForCallFrame(callFrame);
        }
    }

    @Override
    public void error(SipResponse sipResponse) {
        callFrame.setState(callFrame.FAILED);
        callFrame.setCallPanel(callFrame.FAILED.callPanel);
        callFrame.addPageEndLabel("Reason: "
                + sipResponse.getReasonPhrase());
    }

    @Override
    public void ringing() {
        callFrame.setState(callFrame.RINGING);
        callFrame.setCallPanel(callFrame.RINGING.callPanel);
    }

}
