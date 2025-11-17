// Peers SIP Softphone - GPL v3 License

package sip.transactionuser;

import sip.Logger;
import sip.AbstractState;

public abstract class DialogState extends AbstractState {

    protected Dialog dialog;

    public DialogState(String id, Dialog dialog, Logger logger) {
        super(id, logger);
        this.dialog = dialog;
    }
    
    public void receivedOrSent101To199() {}
    public void receivedOrSent2xx() {}
    public void receivedOrSent300To699() {}
    //sent or received a BYE for RFC3261
    public void receivedOrSentBye() {}
    
}
