// Peers SIP Softphone - GPL v3 License

package sip.gui;

import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import sip.Logger;

public class RegistrationStateRegistering extends RegistrationState {

    public RegistrationStateRegistering(String id, Registration registration,
            Logger logger) {
        super(id, registration, logger);
    }

    @Override
    public void registerSuccessful() {
        registration.setState(registration.SUCCESS);
        JLabel label = registration.label;
        URL url = getClass().getResource("green.png");
//        String folder = MainFrame.class.getPackage().getName().replace(".",
//                File.separator);
//        String filename = folder + File.separator + "green.png";
//        logger.debug("filename: " + filename);
//        URL url = MainFrame.class.getClassLoader().getResource(filename);
        ImageIcon imageIcon = new ImageIcon(url);
        label.setIcon(imageIcon);
        label.setText("Registered");
    }

    @Override
    public void registerFailed() {
        registration.setState(registration.FAILED);
        JLabel label = registration.label;
        URL url = getClass().getResource("red.png");
//        String folder = MainFrame.class.getPackage().getName().replace(".",
//                File.separator);
//        URL url = MainFrame.class.getClassLoader().getResource(
//                folder + File.separator + "red.png");
        logger.debug("image url: " + url);
        ImageIcon imageIcon = new ImageIcon(url);
        label.setIcon(imageIcon);
        label.setText("Registration failed");
    }

}
