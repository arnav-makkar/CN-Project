// Peers SIP Softphone - GPL v3 License

package sip.transactionuser;

import sip.Logger;

public class DialogStateConfirmed extends DialogState {

    public DialogStateConfirmed(String id, Dialog dialog, Logger logger) {
        super(id, dialog, logger);
    }

    @Override
    public void receivedOrSent101To199() {
        logger.error(id + " invalid transition");
        throw new IllegalStateException();
    }

    @Override
    public void receivedOrSent2xx() {
        logger.error(id + " invalid transition");
        throw new IllegalStateException();
    }

    @Override
    public void receivedOrSent300To699() {
        logger.error(id + " invalid transition");
        throw new IllegalStateException();
    }

    @Override
    public void receivedOrSentBye() {
        DialogState nextState = dialog.TERMINATED;
        dialog.setState(nextState);
    }

}
