// Peers SIP Softphone - GPL v3 License

package sip.gui;

import java.awt.*;
import javax.swing.*;

import sip.Logger;
import sip.transport.SipResponse;

public class CallFrameStateRinging extends CallFrameState {

    public CallFrameStateRinging(String id, CallFrame callFrame, Logger logger) {
        super(id, callFrame, logger);
        callPanel = DarkTheme.createModernPanel();
        callPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
        
        JLabel ringingLabel = DarkTheme.createTitleLabel("📞 Ringing...");
        ringingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        callPanel.add(ringingLabel);
        
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
    }

    @Override
    public void error(SipResponse sipResponse) {
        callFrame.setState(callFrame.FAILED);
        callFrame.setCallPanel(callFrame.FAILED.callPanel);
        callFrame.addPageEndLabel("Reason: "
                + sipResponse.getReasonPhrase());
    }

}
