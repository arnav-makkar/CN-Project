// Peers SIP Softphone - GPL v3 License

package sip.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;

import sip.Logger;
import sip.transport.SipRequest;
import sip.transport.SipResponse;

public class CallFrame implements ActionListener, WindowListener {

    public static final String HANGUP_ACTION_COMMAND    = "hangup";
    public static final String PICKUP_ACTION_COMMAND    = "pickup";
    public static final String BUSY_HERE_ACTION_COMMAND = "busyhere";
    public static final String CLOSE_ACTION_COMMAND     = "close";

    private CallFrameState state;

    public final CallFrameState INIT;
    public final CallFrameState UAC;
    public final CallFrameState UAS;
    public final CallFrameState RINGING;
    public final CallFrameState SUCCESS;
    public final CallFrameState FAILED;
    public final CallFrameState REMOTE_HANGUP;
    public final CallFrameState TERMINATED;

    private JFrame frame;
    private JPanel callPanel;
    private JPanel callPanelContainer;
    private CallFrameListener callFrameListener;
    private SipRequest sipRequest;
    private CallStatisticsPanel statisticsPanel;
    private javax.swing.Timer statisticsRefreshTimer;

    CallFrame(String remoteParty, String id,
            CallFrameListener callFrameListener, Logger logger) {
        INIT = new StateInit(id, this, logger);
        UAC = new StateUac(id, this, logger);
        UAS = new StateUas(id, this, logger);
        RINGING = new StateRinging(id, this, logger);
        SUCCESS = new StateSuccess(id, this, logger);
        FAILED = new StateFailed(id, this, logger);
        REMOTE_HANGUP = new StateRemoteHangup(id, this, logger);
        TERMINATED = new StateTerminated(id, this, logger);
        state = INIT;
        this.callFrameListener = callFrameListener;
        
        frame = new JFrame(remoteParty);
        frame.setMinimumSize(new Dimension(400, 500));
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.setBackground(DarkTheme.BACKGROUND_PRIMARY);
        
        ((JPanel)contentPane).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel topPanel = DarkTheme.createModernPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        
        JLabel remotePartyLabel = DarkTheme.createTitleLabel(remoteParty);
        Border remotePartyBorder = BorderFactory.createEmptyBorder(5, 5, 10, 5);
        remotePartyLabel.setBorder(remotePartyBorder);
        remotePartyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(remotePartyLabel);
        
        statisticsPanel = new CallStatisticsPanel();
        statisticsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(statisticsPanel);
        
        contentPane.add(topPanel, BorderLayout.NORTH);
        
        Keypad keypad = new Keypad(this);
        contentPane.add(keypad, BorderLayout.CENTER);
        
        callPanelContainer = DarkTheme.createModernPanel();
        contentPane.add(callPanelContainer, BorderLayout.SOUTH);
        
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(this);
        
        statisticsRefreshTimer = new javax.swing.Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (callFrameListener instanceof EventManager && state == SUCCESS) {
                    EventManager eventManager = (EventManager) callFrameListener;
                    eventManager.setStatisticsForCallFrame(CallFrame.this);
                }
            }
        });
        statisticsRefreshTimer.start();
    }

    public void callClicked() {
        state.callClicked();
    }

    public void incomingCall() {
        state.incomingCall();
    }

    public void remoteHangup() {
        state.remoteHangup();
        if (statisticsPanel != null) {
            statisticsPanel.setStatistics(null);
        }
    }

    public void error(SipResponse sipResponse) {
        state.error(sipResponse);
        if (statisticsPanel != null) {
            statisticsPanel.setStatistics(null);
        }
    }

    public void calleePickup() {
        state.calleePickup();
    }

    public void ringing() {
        state.ringing();
    }

    void hangup() {
        if (callFrameListener != null) {
            callFrameListener.hangupClicked(sipRequest);
        }
    }

    void pickup() {
        if (callFrameListener != null && sipRequest != null) {
            callFrameListener.pickupClicked(sipRequest);
        }
    }

    void busyHere() {
        if (callFrameListener != null && sipRequest != null) {
            frame.dispose();
            callFrameListener.busyHereClicked(sipRequest);
            sipRequest = null;
        }
    }

    void close() {
        if (statisticsRefreshTimer != null) {
            statisticsRefreshTimer.stop();
        }
        if (statisticsPanel != null) {
            statisticsPanel.cleanup();
        }
        frame.dispose();
    }
    
    public void setState(CallFrameState state) {
        this.state.log(state);
        this.state = state;
    }

    public JFrame getFrame() {
        return frame;
    }

    public void setCallPanel(JPanel callPanel) {
        if (this.callPanel != null) {
            callPanelContainer.remove(this.callPanel);
            frame.pack();
        }
        callPanelContainer.add(callPanel);
        frame.pack();
        this.callPanel = callPanel;
    }

    public void addPageEndLabel(String text) {
        Container container = frame.getContentPane();
        JLabel label = DarkTheme.createStatusLabel(text, DarkTheme.FOREGROUND_PRIMARY);
        Border labelBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        label.setBorder(labelBorder);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(label);
        frame.pack();
    }

    public void setSipRequest(SipRequest sipRequest) {
        this.sipRequest = sipRequest;
    }
    
    public void setStatistics(sip.rtp.RtpStatistics statistics) {
        if (statisticsPanel != null) {
            statisticsPanel.setStatistics(statistics);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        Runnable runnable = null;
        if (HANGUP_ACTION_COMMAND.equals(actionCommand)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    state.hangupClicked();
                }
            };
        } else if (CLOSE_ACTION_COMMAND.equals(actionCommand)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    state.closeClicked();
                }
            };
        } else if (PICKUP_ACTION_COMMAND.equals(actionCommand)) {
            runnable = new Runnable() {
                public void run() {
                    state.pickupClicked();
                }
            };
        } else if (BUSY_HERE_ACTION_COMMAND.equals(actionCommand)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    state.busyHereClicked();
                }
            };
        }
        if (runnable != null) {
            SwingUtilities.invokeLater(runnable);
        }
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
        state.hangupClicked();
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    public void keypadEvent(char c) {
        callFrameListener.dtmf(c);
    }

    public void receivedDtmf(char digit) {
        if (state == SUCCESS) {
            addPageEndLabel("Received DTMF: " + digit);
        }
    }

    public boolean isActive() {
        return state == SUCCESS;
    }

    public CallFrameListener getCallFrameListener() {
        return callFrameListener;
    }

    // ========== Inner State Classes ==========
    
    abstract class CallFrameState extends sip.AbstractState {
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
    
    class StateInit extends CallFrameState {
        public StateInit(String id, CallFrame callFrame, Logger logger) {
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
    
    class StateUac extends CallFrameState {
        public StateUac(String id, CallFrame callFrame, Logger logger) {
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
            callFrame.addPageEndLabel("Reason: " + sipResponse.getReasonPhrase());
        }

        @Override
        public void ringing() {
            callFrame.setState(callFrame.RINGING);
            callFrame.setCallPanel(callFrame.RINGING.callPanel);
        }
    }
    
    class StateUas extends CallFrameState {
        public StateUas(String id, CallFrame callFrame, Logger logger) {
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
    
    class StateRinging extends CallFrameState {
        public StateRinging(String id, CallFrame callFrame, Logger logger) {
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
            callFrame.addPageEndLabel("Reason: " + sipResponse.getReasonPhrase());
        }
    }
    
    class StateSuccess extends CallFrameState {
        private JButton muteButton;
        private boolean isMuted = false;

        public StateSuccess(String id, CallFrame callFrame, Logger logger) {
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
    
    class StateFailed extends CallFrameState {
        public StateFailed(String id, CallFrame callFrame, Logger logger) {
            super(id, callFrame, logger);
            callPanel = DarkTheme.createModernPanel();
            callPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
            callPanel.add(DarkTheme.createTitleLabel("Failed"));
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
    
    class StateRemoteHangup extends CallFrameState {
        public StateRemoteHangup(String id, CallFrame callFrame, Logger logger) {
            super(id, callFrame, logger);
            callPanel = DarkTheme.createModernPanel();
            callPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
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
    
    class StateTerminated extends CallFrameState {
        public StateTerminated(String id, CallFrame callFrame, Logger logger) {
            super(id, callFrame, logger);
        }

        @Override
        public void calleePickup() {
            callFrame.hangup();
        }
    }

}
