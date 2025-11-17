// Peers SIP Softphone - GPL v3 License

package sip.gui;

import sip.Logger;

public class RegistrationStateUnregsitered extends RegistrationState {

    public RegistrationStateUnregsitered(String id, Registration registration,
            Logger logger) {
        super(id, registration, logger);
    }

    @Override
    public void registerSent() {
        registration.setState(registration.REGISTERING);
        registration.displayRegistering();
    }

}
