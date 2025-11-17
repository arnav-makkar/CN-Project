// Peers SIP Softphone - GPL v3 License

package sip.gui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;


public class Keypad extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    public static final String CHARS = "123456789*0#";

    private CallFrame callFrame;
    private KeypadListener keypadListener;

    public Keypad(CallFrame callFrame) {
        this.callFrame = callFrame;
        initComponents();
    }
    
    public Keypad(KeypadListener keypadListener) {
        this.keypadListener = keypadListener;
        initComponents();
    }

    private void initComponents() {
        setBackground(DarkTheme.BACKGROUND_PRIMARY);
        setLayout(new GridLayout(4, 3, 10, 5));
        for (int i = 0; i < CHARS.length(); ++i) {
            char[] c = { CHARS.charAt(i) };
            String digit = new String(c);
            JButton button = new JButton(digit) {
                @Override
                protected void paintComponent(java.awt.Graphics g) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                                       java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    java.awt.Color bgColor = DarkTheme.BACKGROUND_SECONDARY;
                    if (!isEnabled()) {
                        bgColor = DarkTheme.BUTTON_DISABLED;
                    } else if (getModel().isPressed()) {
                        bgColor = DarkTheme.BACKGROUND_TERTIARY;
                    } else if (getModel().isRollover()) {
                        bgColor = DarkTheme.BACKGROUND_TERTIARY;
                    }
                    
                    g2.setColor(bgColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            button.setFont(DarkTheme.FONT_BOLD);
            button.setForeground(DarkTheme.FOREGROUND_PRIMARY);
            button.setBackground(DarkTheme.BACKGROUND_SECONDARY);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setOpaque(false);
            button.setActionCommand(digit);
            button.addActionListener(this);
            add(button);
        }
        setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        Dimension dimension = new Dimension(180, 115);
        setMinimumSize(dimension);
        setMaximumSize(dimension);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String command = actionEvent.getActionCommand();
        char digit = command.charAt(0);
        if (callFrame != null) {
            callFrame.keypadEvent(digit);
        } else if (keypadListener != null) {
            keypadListener.keypadEvent(digit);
        }
    }

}
