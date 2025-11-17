// Peers SIP Softphone - GPL v3 License

package sip.core.useragent;

import java.net.SocketException;
import java.util.ArrayList;

import sip.RFC3261;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaderParamName;
import sip.syntaxencoding.SipHeaders;
import sip.transaction.TransactionManager;
import sip.transactionuser.Dialog;
import sip.transactionuser.DialogManager;
import sip.transport.SipMessage;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.SipServerTransportUser;
import sip.transport.TransportManager;

public class UAS implements SipServerTransportUser {

    public final static ArrayList<String> SUPPORTED_METHODS;
    
    static {
        SUPPORTED_METHODS = new ArrayList<String>();
        SUPPORTED_METHODS.add(RFC3261.METHOD_INVITE);
        SUPPORTED_METHODS.add(RFC3261.METHOD_ACK);
        SUPPORTED_METHODS.add(RFC3261.METHOD_CANCEL);
        SUPPORTED_METHODS.add(RFC3261.METHOD_OPTIONS);
        SUPPORTED_METHODS.add(RFC3261.METHOD_BYE);
        SUPPORTED_METHODS.add(RFC3261.METHOD_MESSAGE);
    };
    
    private InitialRequestManager initialRequestManager;
    private MidDialogRequestManager midDialogRequestManager;
    
    private DialogManager dialogManager;
    
    /**
     * should be instanciated only once, it was a singleton.
     */
    public UAS(UserAgent userAgent,
            InitialRequestManager initialRequestManager,
            MidDialogRequestManager midDialogRequestManager,
            DialogManager dialogManager,
            TransactionManager transactionManager,
            TransportManager transportManager) throws SocketException {
        this.initialRequestManager = initialRequestManager;
        this.midDialogRequestManager = midDialogRequestManager;
        this.dialogManager = dialogManager;
        transportManager.setSipServerTransportUser(this);
        transportManager.createServerTransport(
                RFC3261.TRANSPORT_UDP, userAgent.getConfig().getSipPort());
    }
    
    public void messageReceived(SipMessage sipMessage) {
        if (sipMessage instanceof SipRequest) {
            requestReceived((SipRequest) sipMessage);
        } else if (sipMessage instanceof SipResponse) {
            responseReceived((SipResponse) sipMessage);
        } else {
            throw new RuntimeException("unknown message type");
        }
    }

    private void responseReceived(SipResponse sipResponse) {
        
    }
    
    private void requestReceived(SipRequest sipRequest) {
        
        
        
        
        SipHeaders headers = sipRequest.getSipHeaders();
        
        
        SipHeaderFieldValue to =
            headers.get(new SipHeaderFieldName(RFC3261.HDR_TO));
        String toTag = to.getParam(new SipHeaderParamName(RFC3261.PARAM_TAG));
        if (toTag != null) {
            Dialog dialog = dialogManager.getDialog(sipRequest);
            if (dialog != null) {
                //this is a mid-dialog request
                midDialogRequestManager.manageMidDialogRequest(sipRequest, dialog);
                
            } else {
                
                
            }
        } else {
            
            initialRequestManager.manageInitialRequest(sipRequest);
            
        }
    }

    void acceptCall(SipRequest sipRequest, Dialog dialog) {
        initialRequestManager.getInviteHandler().acceptCall(sipRequest,
                dialog);
    }

    void rejectCall(SipRequest sipRequest) {
        initialRequestManager.getInviteHandler().rejectCall(sipRequest);
    }

    public InitialRequestManager getInitialRequestManager() {
        return initialRequestManager;
    }

    public MidDialogRequestManager getMidDialogRequestManager() {
        return midDialogRequestManager;
    }
    
}
