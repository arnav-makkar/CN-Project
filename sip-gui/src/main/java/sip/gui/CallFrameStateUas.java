// Peers SIP Softphone - GPL v3 License

package sip.gui;

import java.awt.*;
import javax.swing.*;

import sip.Logger;

public class CallFrameStateUas extends CallFrameState {

    public CallFrameStateUas(String id, CallFrame callFrame, Logger logger) {
        super(id, callFrame, logger);
        callPanel = DarkTheme.createModernPanel();
        callPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
        
        JLabel incomingLabel = DarkTheme.createTitleLabel("📞 Incoming Call");
        incomingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        callPanel.add(incomingLabel);
        
        JPanel buttonPanel = DarkTheme.createModernPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton pickupButton = DarkTheme.createCallButton("Pickup");
        pickupButton.setActionCommand(CallFrame.PICKUP_ACTION_COMMAND);
        pickupButton.addActionListener(callFrame);
        buttonPanel.add(pickupButton);
        
        JButton busyButton = DarkTheme.createHangupButton("Busy");
        busyButton.setActionCommand(CallFrame.BUSY_HERE_ACTION_COMMAND);
        busyButton.addActionListener(callFrame);
        buttonPanel.add(busyButton);
        
        callPanel.add(buttonPanel);
    }

    @Override
    public void pickupClicked() {
        callFrame.setState(callFrame.SUCCESS);
        callFrame.pickup();
        callFrame.setCallPanel(callFrame.SUCCESS.callPanel);
        
        if (callFrame.getCallFrameListener() instanceof EventManager) {
            EventManager eventManager = (EventManager) callFrame.getCallFrameListener();
            eventManager.setStatisticsForCallFrame(callFrame);
        }
    }

    @Override
    public void busyHereClicked() {
        callFrame.setState(callFrame.TERMINATED);
        callFrame.busyHere();
    }

    @Override
    public void remoteHangup() {
        callFrame.setState(callFrame.REMOTE_HANGUP);
        callFrame.setCallPanel(callFrame.REMOTE_HANGUP.callPanel);
    }

}
