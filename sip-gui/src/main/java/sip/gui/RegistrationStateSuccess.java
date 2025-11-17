// Peers SIP Softphone - GPL v3 License

package sip.gui;

import sip.Logger;

public class RegistrationStateSuccess extends RegistrationState {

    public RegistrationStateSuccess(String id, Registration registration,
            Logger logger) {
        super(id, registration, logger);
    }

    @Override
    public void registerSent() {
        registration.setState(registration.REGISTERING);
        registration.displayRegistering();
    }

}
