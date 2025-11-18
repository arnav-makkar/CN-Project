// Peers SIP Softphone - GPL v3 License

package sip.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import sip.Config;
import sip.Logger;
import sip.media.AbstractSoundManager;
import sip.media.MediaManager;
import sip.RFC3261;
import sip.Utils;
import sip.core.useragent.SipListener;
import sip.core.useragent.UserAgent;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaders;
import sip.syntaxencoding.SipUriSyntaxException;
import sip.transactionuser.Dialog;
import sip.transactionuser.DialogManager;
import sip.transport.SipMessage;
import sip.transport.SipRequest;
import sip.transport.SipResponse;

public class EventManager implements SipListener, MainFrame.MainFrameListener,
        CallFrameListener, ActionListener {

    public static final String PEERS_URL = "http://peers.sourceforge.net/";
    public static final String PEERS_USER_MANUAL = PEERS_URL + "user_manual";

    public static final String ACTION_EXIT          = "Exit";
    public static final String ACTION_ACCOUNT       = "Account";
    public static final String ACTION_PREFERENCES   = "Preferences";
    public static final String ACTION_ABOUT         = "About";
    public static final String ACTION_DOCUMENTATION = "Documentation";

    private UserAgent userAgent;
    private MainFrame mainFrame;
    private AccountFrame accountFrame;
    private Map<String, CallFrame> callFrames;
    private boolean closed;
    private Logger logger;

    public EventManager(MainFrame mainFrame, String peersHome,
            Logger logger, AbstractSoundManager soundManager) {
        this.mainFrame = mainFrame;
        this.logger = logger;
        callFrames = Collections.synchronizedMap(
                new HashMap<String, CallFrame>());
        closed = false;
        // create sip stack
        try {
            userAgent = new UserAgent(this, peersHome, logger, soundManager);
        } catch (SocketException e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, "Peers sip port " +
                    		"unavailable, about to leave", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            });
        }
    }

    // sip events

    // never update gui from a non-swing thread, thus using
    // SwingUtilties.invokeLater for each event coming from sip stack.
    @Override
    public void registering(final SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                if (accountFrame != null) {
                    accountFrame.registering(sipRequest);
                }
                mainFrame.registering(sipRequest);
            }
        });

    }

    @Override
    public void registerFailed(final SipResponse sipResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                //mainFrame.setLabelText("Registration failed");
                if (accountFrame != null) {
                    accountFrame.registerFailed(sipResponse);
                }
                mainFrame.registerFailed(sipResponse);
            }
        });

    }

    @Override
    public void registerSuccessful(final SipResponse sipResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                if (closed) {
                    userAgent.close();
                    System.exit(0);
                    return;
                }
                if (accountFrame != null) {
                    accountFrame.registerSuccess(sipResponse);
                }
                mainFrame.registerSuccessful(sipResponse);
            }
        });

    }

    @Override
    public void calleePickup(final SipResponse sipResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                mainFrame.callConnected();
            }
        });

    }

    @Override
    public void error(final SipResponse sipResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                mainFrame.setLabelText("Call failed: " + sipResponse.getReasonPhrase());
                mainFrame.callEnded();
            }
        });

    }

    @Override
    public void incomingCall(final SipRequest sipRequest,
            SipResponse provResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                SipHeaders sipHeaders = sipRequest.getSipHeaders();
                SipHeaderFieldName sipHeaderFieldName =
                    new SipHeaderFieldName(RFC3261.HDR_FROM);
                SipHeaderFieldValue from = sipHeaders.get(sipHeaderFieldName);
                final String fromValue = from.getValue();
                String callId = Utils.getMessageCallId(sipRequest);
                mainFrame.incomingCall(fromValue, callId, sipRequest);
            }
        });

    }

    @Override
    public void remoteHangup(final SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                mainFrame.callEnded();
            }
        });

    }

    @Override
    public void ringing(final SipResponse sipResponse) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                mainFrame.callRinging();
            }
        });

    }

    // main frame events

    @Override
    public void register() {
        if (userAgent == null) {
            // if several peers instances are launched concurrently,
            // display error message and exit
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                Config config = userAgent.getConfig();
                if (config.getPassword() != null) {
                    try {
                        userAgent.register();
                    } catch (SipUriSyntaxException e) {
                        mainFrame.setLabelText(e.getMessage());
                    }
                }
            }
        });

    }

    @Override
    public void callClicked(final String uri) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                String callId = Utils.generateCallID(
                        userAgent.getConfig().getLocalInetAddress());
                SipRequest sipRequest;
                try {
                    sipRequest = userAgent.invite(uri, callId);
                } catch (SipUriSyntaxException e) {
                    logger.error(e.getMessage(), e);
                    mainFrame.setLabelText(e.getMessage());
                    return;
                }
                mainFrame.startCall(uri, callId, sipRequest);
            }
        });

    }

    @Override
    public void windowClosed() {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                try {
                    userAgent.unregister();
                } catch (Exception e) {
                    logger.error("error while unregistering", e);
                }
                closed = true;
                try {
                    Thread.sleep(3 * RFC3261.TIMER_T1);
                } catch (InterruptedException e) {
                }
                System.exit(0);
            }
        });
    }

    // call frame events
    
    @Override
    public void hangupClicked(final SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                userAgent.terminate(sipRequest);
            }
        });
    }

    @Override
    public void pickupClicked(final SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String callId = Utils.getMessageCallId(sipRequest);
                DialogManager dialogManager = userAgent.getDialogManager();
                Dialog dialog = dialogManager.getDialog(callId);
                userAgent.acceptCall(sipRequest, dialog);
            }
        });
    }
    
    @Override
    public void busyHereClicked(final SipRequest sipRequest) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                userAgent.rejectCall(sipRequest);
            }
        });

    }
    
    @Override
    public void dtmf(final char digit) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MediaManager mediaManager = userAgent.getMediaManager();
                mediaManager.sendDtmf(digit);
            }
        });
    }

    @Override
    public void receivedDtmf(final char digit) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Find the active call frame and display received DTMF
                // Since DTMF doesn't have a direct SipMessage, we'll show it on all active calls
                // or find the most recent active call
                for (CallFrame callFrame : callFrames.values()) {
                    if (callFrame != null && callFrame.isActive()) {
                        callFrame.receivedDtmf(digit);
                        logger.debug("Received DTMF digit: " + digit);
                        break; // Only show on first active call
                    }
                }
            }
        });
    }

    private CallFrame getCallFrame(SipMessage sipMessage) {
        String callId = Utils.getMessageCallId(sipMessage);
        return callFrames.get(callId);
    }

    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        logger.debug("gui actionPerformed() " + action);
        Runnable runnable = null;
        if (ACTION_EXIT.equals(action)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    windowClosed();
                }
            };
        } else if (ACTION_ACCOUNT.equals(action)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (accountFrame == null ||
                            !accountFrame.isDisplayable()) {
                        accountFrame = new AccountFrame(userAgent, logger);
                        accountFrame.setVisible(true);
                    } else {
                        accountFrame.requestFocus();
                    }
                }
            };
        } else if (ACTION_PREFERENCES.equals(action)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, "Not implemented yet");
                }
            };
        } else if (ACTION_ABOUT.equals(action)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    AboutFrame aboutFrame = new AboutFrame(
                            userAgent.getPeersHome(), logger);
                    aboutFrame.setVisible(true);
                }
            };
        } else if (ACTION_DOCUMENTATION.equals(action)) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        URI uri = new URI(PEERS_USER_MANUAL);
                        java.awt.Desktop.getDesktop().browse(uri);
                    } catch (URISyntaxException e) {
                        logger.error(e.getMessage(), e);
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            };
        }
        if (runnable != null) {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public void setMuted(boolean muted) {
        if (userAgent != null) {
            sip.media.MediaManager mediaManager = userAgent.getMediaManager();
            if (mediaManager != null) {
                mediaManager.setMuted(muted);
            }
        }
    }

    public void setStatisticsForCallFrame(final CallFrame callFrame) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (userAgent != null) {
                    sip.media.MediaManager mediaManager = userAgent.getMediaManager();
                    if (mediaManager != null) {
                        sip.rtp.RtpStatistics stats = mediaManager.getStatistics();
                        if (stats != null) {
                            callFrame.setStatistics(stats);
                        }
                    }
                }
            }
        });
    }

    public sip.media.MediaManager getMediaManager() {
        if (userAgent != null) {
            return userAgent.getMediaManager();
        }
        return null;
    }
    
    public void sendChatMessage(final String toUri, final String text, final java.io.File attachment) {
        final String destinationUri = sanitizeDestinationUri(toUri);
        if (destinationUri == null || destinationUri.isEmpty()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, 
                            "Error: No recipient URI available. Please ensure a call is active.", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            return;
        }
        
        if (userAgent == null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, 
                            "Error: UserAgent not initialized.", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            return;
        }
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] attachmentData = null;
                    String attachmentName = null;
                    String contentType = null;
                    
                    if (attachment != null && attachment.exists() && attachment.isFile()) {
                        System.out.println("EventManager.sendChatMessage: Reading attachment file: " + attachment.getName());
                        try {
                            attachmentData = java.nio.file.Files.readAllBytes(attachment.toPath());
                            attachmentName = attachment.getName();
                            contentType = java.nio.file.Files.probeContentType(attachment.toPath());
                            if (contentType == null) {
                                contentType = "application/octet-stream";
                            }
                            System.out.println("EventManager.sendChatMessage: File read successfully - " + attachmentData.length + " bytes, type=" + contentType);
                        } catch (Exception e) {
                            logger.error("Error reading attachment file", e);
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    JOptionPane.showMessageDialog(null, 
                                            "Error reading file: " + e.getMessage(), 
                                            "Error", 
                                            JOptionPane.ERROR_MESSAGE);
                                }
                            });
                            return;
                        }
                    }
                    
                    // Ensure we have either text or attachment
                    if ((text == null || text.trim().isEmpty()) && (attachmentData == null || attachmentData.length == 0)) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(null, 
                                        "Error: Message cannot be empty.", 
                                        "Error", 
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        return;
                    }
                    
                    System.out.println("EventManager.sendChatMessage: Calling userAgent.sendMessage()");
                    System.out.println("  toUri: " + destinationUri);
                    System.out.println("  text: " + (text != null ? text : "null"));
                    System.out.println("  attachmentData: " + (attachmentData != null ? attachmentData.length + " bytes" : "null"));
                    System.out.println("  attachmentName: " + attachmentName);
                    
                    userAgent.sendMessage(destinationUri, text != null ? text : "", attachmentData, attachmentName, contentType);
                    
                    System.out.println("EventManager.sendChatMessage: Message sent successfully");
                    
                    // Add sent message to chat UI
                    mainFrame.addChatMessage("You", text != null ? text : "", attachmentData, attachmentName, true);
                    
                } catch (Exception e) {
                    logger.error("Error sending chat message", e);
                    final String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(null, 
                                    "Error sending message: " + errorMsg, 
                                    "Error", 
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            }
        }).start();
    }
    
    @Override
    public void messageReceived(final SipRequest sipRequest, final String messageText, 
            final byte[] attachment, final String attachmentName, final String contentType) {
        System.out.println("===== EventManager.messageReceived() CALLED =====");
        System.out.println("  messageText: " + (messageText != null ? messageText.substring(0, Math.min(50, messageText.length())) + "..." : "null"));
        System.out.println("  attachment: " + (attachment != null ? attachment.length + " bytes" : "null"));
        System.out.println("  attachmentName: " + attachmentName);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("  Inside SwingUtilities.invokeLater in messageReceived");
                // Extract sender from From header
                SipHeaders headers = sipRequest.getSipHeaders();
                SipHeaderFieldValue from = headers.get(new SipHeaderFieldName(RFC3261.HDR_FROM));
                String sender = "Remote";
                if (from != null) {
                    String fromValue = from.getValue();
                    // Extract display name or URI
                    if (fromValue.contains("<")) {
                        int startIndex = fromValue.indexOf("<") + 1;
                        int endIndex = fromValue.indexOf(">");
                        if (endIndex > startIndex) {
                            sender = fromValue.substring(startIndex, endIndex);
                        }
                    } else {
                        sender = fromValue;
                    }
                    // Simplify sender display
                    if (sender.contains("@")) {
                        sender = sender.substring(0, sender.indexOf("@"));
                    }
                    if (sender.startsWith("sip:")) {
                        sender = sender.substring(4);
                    }
                }
                
                System.out.println("  Extracted sender: " + sender);
                System.out.println("  Calling mainFrame.addChatMessage()");
                
                // Add received message to chat UI
                mainFrame.addChatMessage(sender, messageText, attachment, attachmentName, false);
                
                System.out.println("  mainFrame.addChatMessage() completed");
            }
        });
    }

    private String sanitizeDestinationUri(String rawUri) {
        if (rawUri == null) {
            return null;
        }
        String uri = rawUri.trim();
        if (uri.isEmpty()) {
            return null;
        }
        int lt = uri.indexOf('<');
        int gt = uri.indexOf('>');
        if (lt >= 0 && gt > lt) {
            uri = uri.substring(lt + 1, gt).trim();
        }
        if (uri.startsWith("\"") && uri.endsWith("\"") && uri.length() > 1) {
            uri = uri.substring(1, uri.length() - 1);
        }
        uri = uri.trim();
        if (uri.isEmpty()) {
            return null;
        }
        if (!uri.regionMatches(true, 0, "sip:", 0, 4)) {
            uri = "sip:" + uri;
        }
        return uri;
    }

}
