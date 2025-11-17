// Peers SIP Softphone - GPL v3 License

package sip.transactionuser;

import sip.Logger;

public class DialogStateInit extends DialogState {
    
    public DialogStateInit(String id, Dialog dialog, Logger logger) {
        super(id, dialog, logger);
    }

    @Override
    public void receivedOrSent101To199() {
        DialogState nextState = dialog.EARLY;
        dialog.setState(nextState);
    }
    
    @Override
    public void receivedOrSent2xx() {
        DialogState nextState = dialog.CONFIRMED;
        dialog.setState(nextState);
    }
    
    @Override
    public void receivedOrSent300To699() {
        DialogState nextState = dialog.TERMINATED;
        dialog.setState(nextState);
    }
    
    @Override
    public void receivedOrSentBye() {
        logger.error(id + " invalid transition");
        throw new IllegalStateException();
    }
}
