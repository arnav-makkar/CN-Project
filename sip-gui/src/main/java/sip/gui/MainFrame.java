// SIP Softphone - GPL v3 License

package sip.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;

import sip.FileLogger;
import sip.Logger;
import sip.javaxsound.JavaxSoundManager;
import sip.media.AbstractSoundManager;
import sip.RFC3261;
import sip.Utils;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaders;
import sip.syntaxencoding.SipURI;
import sip.syntaxencoding.SipUriSyntaxException;
import sip.transport.SipRequest;
import sip.transport.SipResponse;

public class MainFrame implements WindowListener, ActionListener {

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(args);
            }
        });
    }

    private static void createAndShowGUI(String[] args) {
        JFrame.setDefaultLookAndFeelDecorated(true);
        new MainFrame(args);
    }

    private enum MainFrameState { DIALER, CALLING, RINGING, IN_CALL, INCOMING_CALL }

    private JFrame mainFrame;
    private JPanel mainPanel;
    private JPanel dialerPanel;
    private JTextField uri;
    private JButton actionButton;
    private JLabel statusLabel;
    
    private JPanel callPanel;
    private JLabel remotePartyLabel;
    private CallStatisticsPanel statisticsPanel;
    private JPanel callControlsPanel;
    private JButton muteButton;
    private JButton hangupButton;
    private JButton pickupButton;
    private JButton busyButton;
    private ChatPanel chatPanel;
    private String remotePartyUri;
    
    private MainFrameState currentState = MainFrameState.DIALER;
    private String currentCallId;
    private SipRequest currentSipRequest;
    private boolean isMuted = false;
    private javax.swing.Timer statisticsRefreshTimer;

    private EventManager eventManager;
    private Registration registration;
    private Logger logger;

    public MainFrame(final String[] args) {
        String peersHome = Utils.DEFAULT_SIP_HOME;
        if (args.length > 0) {
            peersHome = args[0];
        } else {
            String systemProperty = System.getProperty(Utils.SIPHOME_SYSTEM_PROPERTY);
            if (systemProperty != null && !systemProperty.isEmpty()) {
                peersHome = systemProperty;
            }
        }
        logger = new FileLogger(peersHome);
        
        DarkTheme.applyTheme();
        
        final AbstractSoundManager soundManager = new JavaxSoundManager(
                false, logger, peersHome);
        String title = "SIP Softphone";
        mainFrame = new JFrame(title);
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.addWindowListener(this);
        mainFrame.setMinimumSize(new Dimension(500, 250));

        mainPanel = DarkTheme.createModernPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = DarkTheme.createTitleLabel("SIP Softphone");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        dialerPanel = DarkTheme.createModernPanel();
        dialerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        uri = DarkTheme.createModernTextField("sip:");
        uri.setText("sip:");
        uri.addActionListener(this);
        uri.setPreferredSize(new Dimension(250, 40));

        actionButton = DarkTheme.createCallButton("Call");
        actionButton.addActionListener(this);

        dialerPanel.add(uri);
        dialerPanel.add(actionButton);
        dialerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = DarkTheme.createStatusLabel("Ready", DarkTheme.FOREGROUND_SECONDARY);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Border border = BorderFactory.createEmptyBorder(10, 2, 2, 2);
        statusLabel.setBorder(border);

        mainPanel.add(dialerPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(statusLabel);

        Container contentPane = mainFrame.getContentPane();
        contentPane.setBackground(DarkTheme.BACKGROUND_PRIMARY);
        contentPane.add(mainPanel);

        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(DarkTheme.BACKGROUND_SECONDARY);
        
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        JMenuItem menuItem = new JMenuItem("Exit");
        menuItem.setMnemonic('x');
        menuItem.setActionCommand(EventManager.ACTION_EXIT);

        registration = new Registration(statusLabel, logger);

        Thread thread = new Thread(new Runnable() {
            public void run() {
                String peersHome = Utils.DEFAULT_SIP_HOME;
                if (args.length > 0) {
                    peersHome = args[0];
                } else {
                    String systemProperty = System.getProperty(Utils.SIPHOME_SYSTEM_PROPERTY);
                    if (systemProperty != null && !systemProperty.isEmpty()) {
                        peersHome = systemProperty;
                    }
                }
                eventManager = new EventManager(MainFrame.this,
                        peersHome, logger, soundManager);
                    eventManager.register();
            }
        }, "gui-event-manager");
        thread.start();

        try {
            while (eventManager == null) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            return;
        }
        menuItem.addActionListener(eventManager);
        menu.add(menuItem);
        menuBar.add(menu);

        menu = new JMenu("Edit");
        menu.setMnemonic('E');
        menuItem = new JMenuItem("Account");
        menuItem.setMnemonic('A');
        menuItem.setActionCommand(EventManager.ACTION_ACCOUNT);
        menuItem.addActionListener(eventManager);
        menu.add(menuItem);
        menuItem = new JMenuItem("Preferences");
        menuItem.setMnemonic('P');
        menuItem.setActionCommand(EventManager.ACTION_PREFERENCES);
        menuItem.addActionListener(eventManager);
        menu.add(menuItem);
        menuBar.add(menu);

        menu = new JMenu("Help");
        menu.setMnemonic('H');
        menuItem = new JMenuItem("User manual");
        menuItem.setMnemonic('D');
        menuItem.setActionCommand(EventManager.ACTION_DOCUMENTATION);
        menuItem.addActionListener(eventManager);
        menu.add(menuItem);
        menuItem = new JMenuItem("About");
        menuItem.setMnemonic('A');
        menuItem.setActionCommand(EventManager.ACTION_ABOUT);
        menuItem.addActionListener(eventManager);
        menu.add(menuItem);
        menuBar.add(menu);

        mainFrame.setJMenuBar(menuBar);

        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
        eventManager.windowClosed();
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

    @Override
    public void actionPerformed(ActionEvent e) {
        eventManager.callClicked(uri.getText());
    }

    public void setLabelText(String text) {
        statusLabel.setText(text);
        statusLabel.setForeground(DarkTheme.ACCENT_PRIMARY);
        mainFrame.pack();
    }

    public void registerFailed(SipResponse sipResponse) {
        registration.registerFailed();
    }

    public void registerSuccessful(SipResponse sipResponse) {
        registration.registerSuccessful();
    }

    public void registering(SipRequest sipRequest) {
        registration.registerSent();
    }

    public void socketExceptionOnStartup() {
        JOptionPane.showMessageDialog(mainFrame, "SIP port unavailable, exiting");
        System.exit(1);
    }
    
    private void initializeCallUI() {
        callPanel = DarkTheme.createModernPanel();
        callPanel.setLayout(new BorderLayout(10, 10));
        
        // Left panel: Call controls
        JPanel leftPanel = DarkTheme.createModernPanel();
        leftPanel.setLayout(new BorderLayout(10, 10));
        
        JPanel topPanel = DarkTheme.createModernPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        
        remotePartyLabel = DarkTheme.createTitleLabel("");
        remotePartyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Border remotePartyBorder = BorderFactory.createEmptyBorder(5, 5, 10, 5);
        remotePartyLabel.setBorder(remotePartyBorder);
        topPanel.add(remotePartyLabel);
        
        statisticsPanel = new CallStatisticsPanel();
        statisticsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(statisticsPanel);
        
        leftPanel.add(topPanel, BorderLayout.NORTH);
        
        callControlsPanel = DarkTheme.createModernPanel();
        callControlsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        muteButton = DarkTheme.createModernButton("Mute");
        muteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleMute();
            }
        });
        
        hangupButton = DarkTheme.createHangupButton("Hangup");
        hangupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hangupCall();
            }
        });
        
        pickupButton = DarkTheme.createCallButton("Pickup");
        pickupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pickupCall();
            }
        });
        
        busyButton = DarkTheme.createHangupButton("Busy");
        busyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                busyHereCall();
            }
        });
        
        leftPanel.add(callControlsPanel, BorderLayout.SOUTH);
        
        // Right panel: Chat
        chatPanel = new ChatPanel(new ChatPanel.ChatPanelListener() {
            @Override
            public void sendMessage(String text, java.io.File attachment) {
                if (eventManager != null && remotePartyUri != null) {
                    eventManager.sendChatMessage(remotePartyUri, text, attachment);
                }
            }
        });
        chatPanel.setEnabled(false);
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, chatPanel);
        splitPane.setResizeWeight(0.6);
        splitPane.setDividerSize(5);
        splitPane.setBackground(DarkTheme.BACKGROUND_PRIMARY);
        splitPane.setBorder(null);
        
        callPanel.add(splitPane, BorderLayout.CENTER);
    }
    
    public void startCall(String remoteUri, String callId, SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (callPanel == null) {
                    initializeCallUI();
                }
                
                currentCallId = callId;
                currentSipRequest = sipRequest;
                currentState = MainFrameState.CALLING;
                remotePartyUri = resolveRemotePartyUri(remoteUri, sipRequest);
                
                remotePartyLabel.setText(remotePartyUri != null ? remotePartyUri : remoteUri);
                
                callControlsPanel.removeAll();
                callControlsPanel.add(hangupButton);
                callControlsPanel.revalidate();
                callControlsPanel.repaint();
                
                mainPanel.removeAll();
                mainPanel.add(callPanel);
                mainPanel.revalidate();
                mainPanel.repaint();
                mainFrame.pack();
                
                startStatisticsRefresh();
            }
        });
    }
    
    public void incomingCall(String remoteUri, String callId, SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (callPanel == null) {
                    initializeCallUI();
                }
                
                currentCallId = callId;
                currentSipRequest = sipRequest;
                currentState = MainFrameState.INCOMING_CALL;
                remotePartyUri = resolveRemotePartyUri(remoteUri, sipRequest);
                
                remotePartyLabel.setText("📞 Incoming: " + (remotePartyUri != null ? remotePartyUri : remoteUri));
                
                callControlsPanel.removeAll();
                callControlsPanel.add(pickupButton);
                callControlsPanel.add(busyButton);
                callControlsPanel.revalidate();
                callControlsPanel.repaint();
                
                mainPanel.removeAll();
                mainPanel.add(callPanel);
                mainPanel.revalidate();
                mainPanel.repaint();
                mainFrame.pack();
                
                mainFrame.toFront();
                mainFrame.requestFocus();
                mainFrame.setAlwaysOnTop(true);
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        });
    }
    
    public void callRinging() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentState = MainFrameState.RINGING;
                remotePartyLabel.setText("📞 Ringing...");
            }
        });
    }
    
    public void callConnected() {
        System.out.println("===== MainFrame.callConnected() CALLED =====");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("  Inside SwingUtilities.invokeLater in callConnected");
                currentState = MainFrameState.IN_CALL;
                
                // Ensure remotePartyUri is set - extract from label if not already set
                if (remotePartyUri == null || remotePartyUri.isEmpty()) {
                    remotePartyUri = resolveRemotePartyUri(remotePartyLabel.getText(), currentSipRequest);
                }
                
                remotePartyLabel.setText("In Call: " + (remotePartyUri != null ? remotePartyUri : ""));
                
                callControlsPanel.removeAll();
                callControlsPanel.add(muteButton);
                callControlsPanel.add(hangupButton);
                callControlsPanel.revalidate();
                callControlsPanel.repaint();
                
                mainFrame.setAlwaysOnTop(false);
                
                // Enable chat panel
                System.out.println("MainFrame.callConnected - about to enable chat panel");
                if (chatPanel != null) {
                    System.out.println("  chatPanel is not null, calling clearMessages and setChatActive(true)");
                    chatPanel.clearMessages();
                    chatPanel.setChatActive(true);
                } else {
                    System.out.println("  ERROR: chatPanel is NULL!");
                }
                
                startStatisticsRefresh();
            }
        });
    }
    
    public void callEnded() {
        System.out.println("===== MainFrame.callEnded() CALLED =====");
        new Exception("Call stack for callEnded").printStackTrace();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("  Inside SwingUtilities.invokeLater in callEnded");
                stopStatisticsRefresh();
                
                // Disable and clear chat panel
                if (chatPanel != null) {
                    chatPanel.clearMessages();
                    chatPanel.setChatActive(false);
                }
                remotePartyUri = null;
                
                currentState = MainFrameState.DIALER;
                currentCallId = null;
                currentSipRequest = null;
                isMuted = false;
                
                if (statisticsPanel != null) {
                    statisticsPanel.setStatistics(null);
                }
                
                mainPanel.removeAll();
                mainPanel.add(Box.createVerticalStrut(20));
                JLabel titleLabel = DarkTheme.createTitleLabel("SIP Softphone");
                titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                mainPanel.add(titleLabel);
                mainPanel.add(Box.createVerticalStrut(20));
                mainPanel.add(dialerPanel);
                mainPanel.add(Box.createVerticalStrut(10));
                mainPanel.add(statusLabel);
                mainPanel.revalidate();
                mainPanel.repaint();
                mainFrame.pack();
            }
        });
    }
    
    private void toggleMute() {
        isMuted = !isMuted;
        if (eventManager != null) {
            eventManager.setMuted(isMuted);
        }
        muteButton.setText(isMuted ? "Unmute" : "Mute");
    }
    
    private void hangupCall() {
        if (eventManager != null && currentSipRequest != null) {
            eventManager.hangupClicked(currentSipRequest);
        }
        callEnded();
    }
    
    private void pickupCall() {
        if (eventManager != null && currentSipRequest != null) {
            eventManager.pickupClicked(currentSipRequest);
            callConnected();
        }
    }
    
    private void busyHereCall() {
        if (eventManager != null && currentSipRequest != null) {
            eventManager.busyHereClicked(currentSipRequest);
        }
        callEnded();
    }
    
    private void startStatisticsRefresh() {
        if (statisticsRefreshTimer == null) {
            statisticsRefreshTimer = new javax.swing.Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (eventManager != null && currentState == MainFrameState.IN_CALL) {
                        sip.media.MediaManager mediaManager = eventManager.getMediaManager();
                        if (mediaManager != null) {
                            sip.rtp.RtpStatistics stats = mediaManager.getStatistics();
                            if (stats != null && statisticsPanel != null) {
                                statisticsPanel.setStatistics(stats);
                            }
                        }
                    }
                }
            });
        }
        statisticsRefreshTimer.start();
    }
    
    private void stopStatisticsRefresh() {
        if (statisticsRefreshTimer != null) {
            statisticsRefreshTimer.stop();
        }
    }
    
    public String getCurrentCallId() {
        return currentCallId;
    }
    
    public SipRequest getCurrentSipRequest() {
        return currentSipRequest;
    }
    
    public void addChatMessage(String sender, String text, byte[] attachment, 
            String attachmentName, boolean isSent) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (chatPanel != null && currentState == MainFrameState.IN_CALL) {
                    ChatMessage message = new ChatMessage(sender, text, attachment, 
                            attachmentName, null, isSent);
                    chatPanel.addMessage(message);
                }
            }
        });
    }

    private String resolveRemotePartyUri(String displayedUri, SipRequest sipRequest) {
        String candidate = normalizeUri(displayedUri);
        String contactUri = normalizeUri(getHeaderUriValue(sipRequest, RFC3261.HDR_CONTACT));
        String fromUri = normalizeUri(getHeaderUriValue(sipRequest, RFC3261.HDR_FROM));
        candidate = ensureSipScheme(candidate);
        contactUri = ensureSipScheme(contactUri);
        fromUri = ensureSipScheme(fromUri);
        String fallback = contactUri != null ? contactUri : fromUri;
        return ensureDialableHost(candidate, fallback);
    }
    
    private String getHeaderUriValue(SipRequest sipRequest, String headerName) {
        if (sipRequest == null || headerName == null) {
            return null;
        }
        SipHeaders headers = sipRequest.getSipHeaders();
        if (headers == null) {
            return null;
        }
        SipHeaderFieldValue value = headers.get(new SipHeaderFieldName(headerName));
        return value != null ? value.getValue() : null;
    }
    
    private String normalizeUri(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        int lt = value.indexOf('<');
        int gt = value.indexOf('>');
        if (lt >= 0 && gt > lt) {
            value = value.substring(lt + 1, gt).trim();
        }
        if (value.startsWith("\"")) {
            value = value.substring(1);
        }
        if (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }
    
    private String ensureSipScheme(String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }
        if (!uri.regionMatches(true, 0, "sip:", 0, 4)) {
            uri = "sip:" + uri;
        }
        return uri;
    }
    
    private String ensureDialableHost(String primaryUri, String fallbackUri) {
        if (primaryUri == null || primaryUri.isEmpty()) {
            return fallbackUri;
        }
        try {
            SipURI sipUri = new SipURI(primaryUri);
            String host = sipUri.getHost();
            if (host != null && !host.isEmpty() && !"local".equalsIgnoreCase(host)) {
                return primaryUri;
            }
            return mergeWithFallback(sipUri.getUserinfo(), fallbackUri);
        } catch (SipUriSyntaxException e) {
            return fallbackUri;
        }
    }
    
    private String mergeWithFallback(String preferredUser, String fallbackUri) {
        if (fallbackUri == null || fallbackUri.isEmpty()) {
            return null;
        }
        try {
            SipURI fallback = new SipURI(fallbackUri);
            String host = fallback.getHost();
            if (host == null || host.isEmpty()) {
                return fallbackUri;
            }
            int port = fallback.getPort();
            String user = preferredUser;
            if ((user == null || user.isEmpty()) && fallback.getUserinfo() != null) {
                user = fallback.getUserinfo();
            }
            StringBuilder builder = new StringBuilder("sip:");
            if (user != null && !user.isEmpty()) {
                builder.append(user).append("@");
            }
            builder.append(host);
            if (port != SipURI.DEFAULT_PORT && port > 0) {
                builder.append(":").append(port);
            }
            return builder.toString();
        } catch (SipUriSyntaxException e) {
            return fallbackUri;
        }
    }

}
