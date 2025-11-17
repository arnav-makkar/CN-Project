// Peers SIP Softphone - GPL v3 License

package sip.gui;

import javax.swing.*;
import java.awt.*;
import sip.rtp.RtpStatistics;

public class CallStatisticsPanel extends JPanel {
    
    private JLabel packetLossLabel;
    private JLabel latencyLabel;
    private JLabel jitterLabel;
    private JLabel durationLabel;
    private JLabel packetsLabel;
    
    private RtpStatistics statistics;
    private Timer updateTimer;
    
    public CallStatisticsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(DarkTheme.BACKGROUND_SECONDARY);
        setBorder(DarkTheme.createModernBorder());
        
        JLabel titleLabel = DarkTheme.createTitleLabel("Call Quality");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(titleLabel);
        add(Box.createVerticalStrut(15));
        
        JPanel statsContainer = new JPanel();
        statsContainer.setLayout(new GridLayout(5, 1, 5, 10));
        statsContainer.setBackground(DarkTheme.BACKGROUND_SECONDARY);
        
        packetLossLabel = createStatLabel("Packet Loss: --");
        latencyLabel = createStatLabel("Latency: --");
        jitterLabel = createStatLabel("Jitter: --");
        durationLabel = createStatLabel("Duration: 00:00");
        packetsLabel = createStatLabel("Packets: 0/0");
        
        statsContainer.add(createStatRow("📊", packetLossLabel));
        statsContainer.add(createStatRow("⏱", latencyLabel));
        statsContainer.add(createStatRow("📶", jitterLabel));
        statsContainer.add(createStatRow("⏰", durationLabel));
        statsContainer.add(createStatRow("📦", packetsLabel));
        
        add(statsContainer);
    }
    
    private JLabel createStatLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DarkTheme.FONT_REGULAR);
        label.setForeground(DarkTheme.FOREGROUND_PRIMARY);
        return label;
    }
    
    private JPanel createStatRow(String icon, JLabel label) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setBackground(DarkTheme.BACKGROUND_SECONDARY);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        row.add(iconLabel);
        row.add(label);
        
        return row;
    }
    
    public void setStatistics(RtpStatistics stats) {
        this.statistics = stats;
        if (stats != null && updateTimer == null) {
            startUpdating();
        } else if (stats == null && updateTimer != null) {
            stopUpdating();
        }
    }
    
    private void startUpdating() {
        updateTimer = new Timer(500, e -> updateDisplay());
        updateTimer.start();
        updateDisplay();
    }
    
    private void stopUpdating() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
        resetDisplay();
    }
    
    private void updateDisplay() {
        if (statistics == null) {
            resetDisplay();
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            double packetLoss = statistics.getPacketLossPercentage();
            Color lossColor = getQualityColor(packetLoss, 1.0, 5.0);
            packetLossLabel.setText(String.format("Packet Loss: %.2f%%", packetLoss));
            packetLossLabel.setForeground(lossColor);
            
            long latency = statistics.getAverageLatencyMs();
            Color latencyColor = getQualityColor(latency, 100, 200);
            latencyLabel.setText(String.format("Latency: %d ms", latency));
            latencyLabel.setForeground(latencyColor);
            
            long jitter = statistics.getAverageJitterMs();
            Color jitterColor = getQualityColor(jitter, 20, 50);
            jitterLabel.setText(String.format("Jitter: %d ms", jitter));
            jitterLabel.setForeground(jitterColor);
            
            durationLabel.setText("Duration: " + statistics.getFormattedCallDuration());
            durationLabel.setForeground(DarkTheme.FOREGROUND_PRIMARY);
            
            int received = statistics.getPacketsReceived();
            int expected = statistics.getExpectedPackets();
            packetsLabel.setText(String.format("Packets: %d/%d", received, expected));
            packetsLabel.setForeground(DarkTheme.FOREGROUND_PRIMARY);
        });
    }
    
    private Color getQualityColor(double value, double warningThreshold, double errorThreshold) {
        if (value < warningThreshold) {
            return DarkTheme.ACCENT_SUCCESS;
        } else if (value < errorThreshold) {
            return DarkTheme.ACCENT_WARNING;
        } else {
            return DarkTheme.ACCENT_ERROR;
        }
    }
    
    private void resetDisplay() {
        SwingUtilities.invokeLater(() -> {
            packetLossLabel.setText("Packet Loss: --");
            packetLossLabel.setForeground(DarkTheme.FOREGROUND_SECONDARY);
            
            latencyLabel.setText("Latency: --");
            latencyLabel.setForeground(DarkTheme.FOREGROUND_SECONDARY);
            
            jitterLabel.setText("Jitter: --");
            jitterLabel.setForeground(DarkTheme.FOREGROUND_SECONDARY);
            
            durationLabel.setText("Duration: 00:00");
            durationLabel.setForeground(DarkTheme.FOREGROUND_SECONDARY);
            
            packetsLabel.setText("Packets: 0/0");
            packetsLabel.setForeground(DarkTheme.FOREGROUND_SECONDARY);
        });
    }
    
    public void cleanup() {
        stopUpdating();
        statistics = null;
    }
}

