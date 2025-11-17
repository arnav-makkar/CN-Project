// Peers SIP Softphone - GPL v3 License

package sip.gui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import sip.Logger;

public class CallFrameStateRemoteHangup extends CallFrameState {

    public CallFrameStateRemoteHangup(String id, CallFrame callFrame,
            Logger logger) {
        super(id, callFrame, logger);
        callPanel = DarkTheme.createModernPanel();
        callPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 15, 15));
        callPanel.add(DarkTheme.createTitleLabel("Remote hangup"));
        JButton closeButton = DarkTheme.createModernButton("Close");
        closeButton.setActionCommand(CallFrame.CLOSE_ACTION_COMMAND);
        closeButton.addActionListener(callFrame);
        callPanel.add(closeButton);
    }

    @Override
    public void closeClicked() {
        callFrame.setState(callFrame.TERMINATED);
        callFrame.close();
    }

}
