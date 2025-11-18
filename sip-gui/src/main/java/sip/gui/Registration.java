// Peers SIP Softphone - GPL v3 License

package sip.gui;

import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import sip.Logger;

public class Registration {

    public final RegistrationState UNREGISTERED;
    public final RegistrationState REGISTERING;
    public final RegistrationState SUCCESS;
    public final RegistrationState FAILED;

    protected JLabel label;
    private RegistrationState state;

    public Registration(JLabel label, Logger logger) {
        this.label = label;

        String id = String.valueOf(hashCode());
        UNREGISTERED = new StateUnregistered(id, this, logger);
        state = UNREGISTERED;
        REGISTERING = new StateRegistering(id, this, logger);
        SUCCESS = new StateSuccess(id, this, logger);
        FAILED = new StateFailed(id, this, logger);

    }

    public void setState(RegistrationState state) {
        this.state = state;
    }

    public synchronized void registerSent() {
        state.registerSent();
    }

    public synchronized void registerFailed() {
        state.registerFailed();
    }

    public synchronized void registerSuccessful() {
        state.registerSuccessful();
    }

    protected void displayRegistering() {
        URL url = getClass().getResource("working.gif");
//        String folder = MainFrame.class.getPackage().getName().replace(".",
//                File.separator);
//        String filename = folder + File.separator + "working.gif";
//        Logger.debug("filename: " + filename);
//        URL url = MainFrame.class.getClassLoader().getResource(filename);
        ImageIcon imageIcon = new ImageIcon(url);
        label.setIcon(imageIcon);
        label.setText("Registering");
    }

    // ========== Inner State Classes ==========
    
    abstract class RegistrationState extends sip.AbstractState {
        protected Registration registration;

        public RegistrationState(String id, Registration registration, Logger logger) {
            super(id, logger);
            this.registration = registration;
        }

        public void registerSent() {}
        public void registerSuccessful() {}
        public void registerFailed() {}
    }
    
    class StateUnregistered extends RegistrationState {
        public StateUnregistered(String id, Registration registration, Logger logger) {
            super(id, registration, logger);
        }

        @Override
        public void registerSent() {
            registration.setState(registration.REGISTERING);
            registration.displayRegistering();
        }
    }
    
    class StateRegistering extends RegistrationState {
        public StateRegistering(String id, Registration registration, Logger logger) {
            super(id, registration, logger);
        }

        @Override
        public void registerSuccessful() {
            registration.setState(registration.SUCCESS);
            JLabel label = registration.label;
            URL url = getClass().getResource("green.png");
            ImageIcon imageIcon = new ImageIcon(url);
            label.setIcon(imageIcon);
            label.setText("Registered");
        }

        @Override
        public void registerFailed() {
            registration.setState(registration.FAILED);
            JLabel label = registration.label;
            URL url = getClass().getResource("red.png");
            logger.debug("image url: " + url);
            ImageIcon imageIcon = new ImageIcon(url);
            label.setIcon(imageIcon);
            label.setText("Registration failed");
        }
    }
    
    class StateSuccess extends RegistrationState {
        public StateSuccess(String id, Registration registration, Logger logger) {
            super(id, registration, logger);
        }

        @Override
        public void registerSent() {
            registration.setState(registration.REGISTERING);
            registration.displayRegistering();
        }
    }
    
    class StateFailed extends RegistrationState {
        public StateFailed(String id, Registration registration, Logger logger) {
            super(id, registration, logger);
        }

        @Override
        public void registerSent() {
            registration.setState(registration.REGISTERING);
            registration.displayRegistering();
        }
    }

}
