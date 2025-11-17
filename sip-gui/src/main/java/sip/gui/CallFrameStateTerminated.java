// Peers SIP Softphone - GPL v3 License

package sip.gui;

import sip.Logger;

public class CallFrameStateTerminated extends CallFrameState {

    public CallFrameStateTerminated(String id, CallFrame callFrame,
            Logger logger) {
        super(id, callFrame, logger);
    }

    @Override
    public void calleePickup() {
        callFrame.hangup();
    }

}
