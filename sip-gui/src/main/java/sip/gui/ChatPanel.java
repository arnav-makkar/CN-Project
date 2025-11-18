// Peers SIP Softphone - GPL v3 License

package sip.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatPanel extends JPanel {
    private static final int MAX_FILE_SIZE = 1024 * 1024; // 1MB
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
    
    private JTextPane messageHistoryPane;
    private StyledDocument messageDocument;
    private JTextArea messageInputArea;
    private JButton sendButton;
    private JButton attachButton;
    private JLabel attachmentLabel;
    private JPanel headerPanel;
    private JButton toggleButton;
    private JPanel contentPanel;
    private boolean isExpanded;
    private File selectedFile;
    private List<ChatMessage> messages;
    private ChatPanelListener listener;
    
    public interface ChatPanelListener {
        void sendMessage(String text, File attachment);
    }
    
    public ChatPanel(ChatPanelListener listener) {
        this.listener = listener;
        this.messages = new ArrayList<>();
        this.isExpanded = false;
        initComponents();
        
        // Make the panel itself clickable to give focus to the input field
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (messageInputArea != null && messageInputArea.isEnabled()) {
                    focusInputField();
                }
            }
        });
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(DarkTheme.BACKGROUND_PRIMARY);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        
        // Header with toggle button
        headerPanel = DarkTheme.createModernPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("💬 Chat");
        titleLabel.setFont(DarkTheme.FONT_BOLD);
        titleLabel.setForeground(DarkTheme.FOREGROUND_PRIMARY);
        
        toggleButton = new JButton(isExpanded ? "▼" : "▶");
        toggleButton.setFont(new Font("Arial", Font.PLAIN, 10));
        toggleButton.setForeground(DarkTheme.FOREGROUND_PRIMARY);
        toggleButton.setBackground(DarkTheme.BACKGROUND_SECONDARY);
        toggleButton.setFocusPainted(false);
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleButton.addActionListener(e -> toggleExpand());
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(toggleButton, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Content panel (collapsible)
        contentPanel = DarkTheme.createModernPanel();
        contentPanel.setLayout(new BorderLayout(5, 5));
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPanel.setVisible(isExpanded);
        
        // Message history area
        messageHistoryPane = new JTextPane();
        messageHistoryPane.setEditable(false);
        messageHistoryPane.setFocusable(false);
        messageHistoryPane.setRequestFocusEnabled(false);
        messageHistoryPane.setFocusTraversalKeysEnabled(false);
        messageHistoryPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                focusInputField();
            }
        });
        messageHistoryPane.setBackground(DarkTheme.BACKGROUND_SECONDARY);
        messageHistoryPane.setForeground(DarkTheme.FOREGROUND_PRIMARY);
        messageHistoryPane.setFont(DarkTheme.FONT_REGULAR);
        messageDocument = messageHistoryPane.getStyledDocument();
        
        JScrollPane scrollPane = new JScrollPane(messageHistoryPane);
        scrollPane.setFocusable(false);
        scrollPane.setRequestFocusEnabled(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(DarkTheme.BACKGROUND_TERTIARY, 1));
        scrollPane.setPreferredSize(new Dimension(300, 400));
        
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Input area
        JPanel inputPanel = DarkTheme.createModernPanel();
        inputPanel.setLayout(new BorderLayout(5, 8));
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        // Attachment label
        attachmentLabel = new JLabel();
        attachmentLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        attachmentLabel.setForeground(DarkTheme.ACCENT_BLUE);
        attachmentLabel.setVisible(false);
        inputPanel.add(attachmentLabel, BorderLayout.NORTH);
        
        // Message composer
        messageInputArea = new JTextArea(3, 20);
        messageInputArea.setLineWrap(true);
        messageInputArea.setWrapStyleWord(true);
        messageInputArea.setBackground(DarkTheme.BACKGROUND_TERTIARY);
        messageInputArea.setForeground(DarkTheme.FOREGROUND_PRIMARY);
        messageInputArea.setCaretColor(DarkTheme.FOREGROUND_PRIMARY);
        messageInputArea.setFont(DarkTheme.FONT_REGULAR);
        messageInputArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        messageInputArea.setEnabled(false);
        messageInputArea.setEditable(false);
        messageInputArea.setFocusable(false);
        messageInputArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSendButton(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSendButton(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSendButton(); }
        });
        
        JScrollPane composerScroll = new JScrollPane(messageInputArea);
        composerScroll.setBorder(BorderFactory.createLineBorder(DarkTheme.BACKGROUND_TERTIARY, 1));
        composerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        composerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        composerScroll.setFocusable(false);
        composerScroll.setRequestFocusEnabled(false);
        inputPanel.add(composerScroll, BorderLayout.CENTER);
        
        // Action buttons row
        attachButton = createIconButton("📎");
        attachButton.setToolTipText("Attach file");
        attachButton.addActionListener(e -> selectFile());
        
        sendButton = DarkTheme.createModernButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        sendButton.setEnabled(false);
        
        JPanel controlsPanel = new JPanel(new BorderLayout(5, 0));
        controlsPanel.setOpaque(false);
        controlsPanel.add(attachButton, BorderLayout.WEST);
        controlsPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(controlsPanel, BorderLayout.SOUTH);
        
        contentPanel.add(inputPanel, BorderLayout.SOUTH);
        
        add(contentPanel, BorderLayout.CENTER);
    }
    
    private JButton createIconButton(String icon) {
        JButton button = new JButton(icon);
        button.setFont(new Font("Arial", Font.PLAIN, 16));
        button.setForeground(DarkTheme.FOREGROUND_PRIMARY);
        button.setBackground(DarkTheme.BACKGROUND_SECONDARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(40, 30));
        return button;
    }
    
    private void toggleExpand() {
        isExpanded = !isExpanded;
        contentPanel.setVisible(isExpanded);
        toggleButton.setText(isExpanded ? "▼" : "▶");
        revalidate();
        repaint();
    }
    
    public void expand() {
        if (!isExpanded) {
            isExpanded = true;
            contentPanel.setVisible(true);
            toggleButton.setText("▼");
            revalidate();
            repaint();
        }
        SwingUtilities.invokeLater(() -> focusInputField());
    }
    
    public void focusInputField() {
        if (messageInputArea == null || !messageInputArea.isEnabled()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            messageInputArea.requestFocusInWindow();
            messageInputArea.grabFocus();
            int length = messageInputArea.getText() != null ? messageInputArea.getText().length() : 0;
            messageInputArea.setCaretPosition(length);
        });
    }
    
    private void updateSendButton() {
        if (sendButton == null || messageInputArea == null) {
            return;
        }
        boolean panelEnabled = super.isEnabled() && messageInputArea.isEnabled();
        boolean hasText = messageInputArea.getText() != null && !messageInputArea.getText().trim().isEmpty();
        boolean hasAttachment = selectedFile != null;
        sendButton.setEnabled(panelEnabled && (hasText || hasAttachment));
    }
    
    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            if (file.length() > MAX_FILE_SIZE) {
                JOptionPane.showMessageDialog(this,
                        "File size exceeds 1MB limit. Please select a smaller file.",
                        "File Too Large",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            selectedFile = file;
            attachmentLabel.setText("📎 " + file.getName() + " (" + formatFileSize(file.length()) + ")");
            attachmentLabel.setVisible(true);
            updateSendButton();
        }
    }
    
    private void sendMessage() {
        if (!super.isEnabled() || messageInputArea == null || !messageInputArea.isEnabled()) {
            return;
        }
        
        String text = messageInputArea.getText() != null ? messageInputArea.getText().trim() : "";
        
        if (text.isEmpty() && selectedFile == null) {
            return;
        }
        
        if (listener == null) {
            JOptionPane.showMessageDialog(this,
                    "Error: Chat listener not initialized.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        sendButton.setEnabled(false);
        sendButton.setText("Sending...");
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                listener.sendMessage(text, selectedFile);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    messageInputArea.setText("");
                    selectedFile = null;
                    attachmentLabel.setVisible(false);
                } catch (Exception e) {
                    // EventManager shows error dialog
                } finally {
                    sendButton.setText("Send");
                    updateSendButton();
                    focusInputField();
                }
            }
        };
        worker.execute();
    }
    
    public void addMessage(ChatMessage message) {
        messages.add(message);
        displayMessage(message);
        scrollToBottom();
    }
    
    private void displayMessage(ChatMessage message) {
        try {
            // Create styles
            Style defaultStyle = messageDocument.addStyle("default", null);
            StyleConstants.setFontFamily(defaultStyle, DarkTheme.FONT_REGULAR.getFamily());
            StyleConstants.setFontSize(defaultStyle, 12);
            
            Style timestampStyle = messageDocument.addStyle("timestamp", defaultStyle);
            StyleConstants.setForeground(timestampStyle, DarkTheme.FOREGROUND_SECONDARY);
            StyleConstants.setFontSize(timestampStyle, 10);
            
            Style senderStyle = messageDocument.addStyle("sender", defaultStyle);
            StyleConstants.setForeground(senderStyle, message.isSent() ? 
                    DarkTheme.ACCENT_BLUE : DarkTheme.ACCENT_GREEN);
            StyleConstants.setBold(senderStyle, true);
            
            Style messageStyle = messageDocument.addStyle("message", defaultStyle);
            StyleConstants.setForeground(messageStyle, DarkTheme.FOREGROUND_PRIMARY);
            
            Style attachmentStyle = messageDocument.addStyle("attachment", defaultStyle);
            StyleConstants.setForeground(attachmentStyle, DarkTheme.ACCENT_BLUE);
            StyleConstants.setUnderline(attachmentStyle, true);
            
            // Add timestamp
            String timeStr = TIME_FORMAT.format(new Date(message.getTimestamp()));
            messageDocument.insertString(messageDocument.getLength(), 
                    timeStr + " ", timestampStyle);
            
            // Add sender
            messageDocument.insertString(messageDocument.getLength(), 
                    message.getSender() + ": ", senderStyle);
            
            // Add message text
            if (message.getText() != null && !message.getText().isEmpty()) {
                messageDocument.insertString(messageDocument.getLength(), 
                        message.getText(), messageStyle);
            }
            
            // Add attachment indicator
            if (message.hasAttachment()) {
                String attachmentText = "\n📄 " + message.getAttachmentName();
                messageDocument.insertString(messageDocument.getLength(), 
                        attachmentText, attachmentStyle);
                
                // Make attachment clickable
                int start = messageDocument.getLength() - attachmentText.length();
                int end = messageDocument.getLength();
                
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, DarkTheme.ACCENT_BLUE);
                StyleConstants.setUnderline(attrs, true);
                messageDocument.setCharacterAttributes(start, end - start, attrs, false);
                
                // Add mouse listener for download
                messageHistoryPane.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        int pos = messageHistoryPane.viewToModel(e.getPoint());
                        if (pos >= start && pos < end) {
                            downloadAttachment(message);
                        }
                    }
                });
            }
            
            // Add newline
            messageDocument.insertString(messageDocument.getLength(), "\n\n", defaultStyle);
            
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private void downloadAttachment(ChatMessage message) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(message.getAttachmentName()));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(message.getAttachment());
                JOptionPane.showMessageDialog(this,
                        "File saved successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error saving file: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = ((JScrollPane) contentPanel.getComponent(0))
                    .getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }
    
    public void clearMessages() {
        messages.clear();
        messageHistoryPane.setText("");
        if (messageInputArea != null) {
            messageInputArea.setText("");
        }
        selectedFile = null;
        attachmentLabel.setVisible(false);
    }
    
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        applyInputState(enabled);
        updateSendButton();
    }

    public void setChatActive(boolean active) {
        super.setEnabled(active);
        applyInputState(active);
        if (active) {
            expand();
            focusInputField();
        }
        updateSendButton();
    }

    private void applyInputState(boolean enabled) {
        if (messageInputArea != null) {
            messageInputArea.setEnabled(enabled);
            messageInputArea.setEditable(enabled);
            messageInputArea.setFocusable(enabled);
        }
        if (attachButton != null) {
            attachButton.setEnabled(enabled);
        }
        if (!enabled && sendButton != null) {
            sendButton.setEnabled(false);
        }
    }
    
    // ========== Inner Class ==========
    
    public static class ChatMessage {
        private String sender;
        private String text;
        private byte[] attachment;
        private String attachmentName;
        private String contentType;
        private long timestamp;
        private boolean isSent;

        public ChatMessage(String sender, String text, byte[] attachment, 
                String attachmentName, String contentType, boolean isSent) {
            this.sender = sender;
            this.text = text;
            this.attachment = attachment;
            this.attachmentName = attachmentName;
            this.contentType = contentType;
            this.timestamp = System.currentTimeMillis();
            this.isSent = isSent;
        }

        public String getSender() {
            return sender;
        }

        public String getText() {
            return text;
        }

        public byte[] getAttachment() {
            return attachment;
        }

        public String getAttachmentName() {
            return attachmentName;
        }

        public String getContentType() {
            return contentType;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isSent() {
            return isSent;
        }

        public boolean hasAttachment() {
            return attachment != null && attachment.length > 0;
        }
    }
}

