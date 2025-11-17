// Peers SIP Softphone - GPL v3 License

package sip.gui;

import sip.Logger;
import sip.AbstractState;

public abstract class RegistrationState extends AbstractState {

    protected Registration registration;

    public RegistrationState(String id, Registration registration,
            Logger logger) {
        super(id, logger);
        this.registration = registration;
    }

    public void registerSent() {}
    public void registerSuccessful() {}
    public void registerFailed() {}

}
