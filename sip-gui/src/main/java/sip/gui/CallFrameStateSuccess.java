// Peers SIP Softphone - GPL v3 License

package sip.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import sip.Logger;

public class CallFrameStateSuccess extends CallFrameState {

    private JButton muteButton;
    private boolean isMuted = false;

    public CallFrameStateSuccess(String id, CallFrame callFrame, Logger logger) {
        super(id, callFrame, logger);
        callPanel = DarkTheme.createModernPanel();
        callPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
        
        JLabel talkingLabel = DarkTheme.createTitleLabel("In Call");
        callPanel.add(talkingLabel);
        
        JPanel buttonPanel = DarkTheme.createModernPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        muteButton = DarkTheme.createModernButton("Mute");
        muteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleMute();
            }
        });
        buttonPanel.add(muteButton);
        
        JButton hangupButton = DarkTheme.createHangupButton("Hangup");
        hangupButton.setActionCommand(CallFrame.HANGUP_ACTION_COMMAND);
        hangupButton.addActionListener(callFrame);
        buttonPanel.add(hangupButton);
        
        callPanel.add(buttonPanel);
    }
    
    private void toggleMute() {
        isMuted = !isMuted;
        if (callFrame.getCallFrameListener() instanceof EventManager) {
            EventManager eventManager = (EventManager) callFrame.getCallFrameListener();
            eventManager.setMuted(isMuted);
        }
        muteButton.setText(isMuted ? "Unmute" : "Mute");
    }

    @Override
    public void remoteHangup() {
        callFrame.setState(callFrame.REMOTE_HANGUP);
        callFrame.setCallPanel(callFrame.REMOTE_HANGUP.callPanel);
    }

    @Override
    public void hangupClicked() {
        callFrame.setState(callFrame.TERMINATED);
        callFrame.hangup();
        callFrame.close();
    }

}
