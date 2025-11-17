// Peers SIP Softphone - GPL v3 License

package sip.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.ColorUIResource;

public class DarkTheme {
    
    public static final Color BACKGROUND_PRIMARY = new Color(30, 30, 30);
    public static final Color BACKGROUND_SECONDARY = new Color(43, 43, 43);
    public static final Color BACKGROUND_TERTIARY = new Color(55, 55, 55);
    
    public static final Color FOREGROUND_PRIMARY = new Color(224, 224, 224);
    public static final Color FOREGROUND_SECONDARY = new Color(189, 189, 189);
    public static final Color FOREGROUND_DISABLED = new Color(100, 100, 100);
    
    public static final Color ACCENT_PRIMARY = new Color(74, 158, 255);
    public static final Color ACCENT_SECONDARY = new Color(0, 212, 255);
    public static final Color ACCENT_SUCCESS = new Color(76, 175, 80);
    public static final Color ACCENT_WARNING = new Color(255, 152, 0);
    public static final Color ACCENT_ERROR = new Color(244, 67, 54);
    public static final Color ACCENT_BLUE = new Color(74, 158, 255);
    public static final Color ACCENT_GREEN = new Color(76, 175, 80);
    
    public static final Color BUTTON_NORMAL = new Color(74, 158, 255);
    public static final Color BUTTON_HOVER = new Color(100, 180, 255);
    public static final Color BUTTON_PRESSED = new Color(50, 136, 233);
    public static final Color BUTTON_DISABLED = new Color(60, 60, 60);
    
    public static final Color CALL_BUTTON = new Color(76, 175, 80);
    public static final Color CALL_BUTTON_HOVER = new Color(102, 187, 106);
    public static final Color HANGUP_BUTTON = new Color(244, 67, 54);
    public static final Color HANGUP_BUTTON_HOVER = new Color(229, 115, 115);
    
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    
    public static void applyTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        
        UIManager.put("Panel.background", BACKGROUND_PRIMARY);
        UIManager.put("Panel.foreground", FOREGROUND_PRIMARY);
        
        UIManager.put("Button.background", BUTTON_NORMAL);
        UIManager.put("Button.foreground", Color.BLACK);
        UIManager.put("Button.font", FONT_BOLD);
        UIManager.put("Button.focus", BACKGROUND_SECONDARY);
        
        UIManager.put("TextField.background", BACKGROUND_SECONDARY);
        UIManager.put("TextField.foreground", FOREGROUND_PRIMARY);
        UIManager.put("TextField.caretForeground", ACCENT_PRIMARY);
        UIManager.put("TextField.inactiveForeground", FOREGROUND_DISABLED);
        UIManager.put("TextField.font", FONT_REGULAR);
        
        UIManager.put("Label.foreground", FOREGROUND_PRIMARY);
        UIManager.put("Label.font", FONT_REGULAR);
        
        UIManager.put("Menu.background", BACKGROUND_SECONDARY);
        UIManager.put("Menu.foreground", FOREGROUND_PRIMARY);
        UIManager.put("MenuBar.background", BACKGROUND_SECONDARY);
        UIManager.put("MenuItem.background", BACKGROUND_SECONDARY);
        UIManager.put("MenuItem.foreground", FOREGROUND_PRIMARY);
        UIManager.put("MenuItem.selectionBackground", BACKGROUND_TERTIARY);
        
        UIManager.put("OptionPane.background", BACKGROUND_PRIMARY);
        UIManager.put("OptionPane.messageForeground", FOREGROUND_PRIMARY);
    }
    
    public static JButton createModernButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color bgColor = BUTTON_NORMAL;
                if (!isEnabled()) {
                    bgColor = BUTTON_DISABLED;
                } else if (getModel().isPressed()) {
                    bgColor = BUTTON_PRESSED;
                } else if (getModel().isRollover()) {
                    bgColor = BUTTON_HOVER;
                }
                
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                
                setForeground(Color.BLACK);
                super.paintComponent(g);
            }
        };
        button.setFont(FONT_BOLD);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 40));
        return button;
    }
    
    public static JButton createCallButton(String text) {
        JButton button = createModernButton(text);
        button.putClientProperty("buttonType", "call");
        return button;
    }
    
    public static JButton createHangupButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color bgColor = HANGUP_BUTTON;
                if (!isEnabled()) {
                    bgColor = BUTTON_DISABLED;
                } else if (getModel().isPressed()) {
                    bgColor = HANGUP_BUTTON.darker();
                } else if (getModel().isRollover()) {
                    bgColor = HANGUP_BUTTON_HOVER;
                }
                
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                
                setForeground(Color.BLACK);
                super.paintComponent(g);
            }
        };
        button.setFont(FONT_BOLD);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 40));
        return button;
    }
    
    public static JTextField createModernTextField(String placeholder) {
        JTextField textField = new JTextField(15);
        textField.setFont(FONT_REGULAR);
        textField.setBackground(BACKGROUND_SECONDARY);
        textField.setForeground(FOREGROUND_PRIMARY);
        textField.setCaretColor(ACCENT_PRIMARY);
        textField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BACKGROUND_TERTIARY, 2, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return textField;
    }
    
    public static JPanel createModernPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND_PRIMARY);
        return panel;
    }
    
    public static JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_TITLE);
        label.setForeground(FOREGROUND_PRIMARY);
        return label;
    }
    
    public static JLabel createStatusLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SMALL);
        label.setForeground(color);
        return label;
    }
    
    public static Border createModernBorder() {
        return BorderFactory.createCompoundBorder(
            new LineBorder(BACKGROUND_TERTIARY, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );
    }
}

