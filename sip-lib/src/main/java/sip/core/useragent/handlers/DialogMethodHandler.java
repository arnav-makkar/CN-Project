// Peers SIP Softphone - GPL v3 License

package sip.core.useragent.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;

import sip.Logger;
import sip.RFC3261;
import sip.core.useragent.UserAgent;
import sip.syntaxencoding.NameAddress;
import sip.syntaxencoding.SipHeaderFieldMultiValue;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaderParamName;
import sip.syntaxencoding.SipHeaders;
import sip.transaction.Transaction;
import sip.transaction.TransactionManager;
import sip.transactionuser.Dialog;
import sip.transactionuser.DialogManager;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.TransportManager;


public abstract class DialogMethodHandler extends MethodHandler {

    protected DialogManager dialogManager;
    
    public DialogMethodHandler(UserAgent userAgent,
            DialogManager dialogManager,
            TransactionManager transactionManager,
            TransportManager transportManager, Logger logger) {
        super(userAgent, transactionManager, transportManager, logger);
        this.dialogManager = dialogManager;
    }
    
    protected Dialog buildDialogForUas(SipResponse sipResponse,
            SipRequest sipRequest) {
        //12.1.1
        
        //prepare response
        
        SipHeaders reqHeaders = sipRequest.getSipHeaders();
        SipHeaders respHeaders = sipResponse.getSipHeaders();
        
          //copy record-route
        SipHeaderFieldName recordRouteName =
            new SipHeaderFieldName(RFC3261.HDR_RECORD_ROUTE);
        SipHeaderFieldValue reqRecRoute = reqHeaders.get(recordRouteName);
        if (reqRecRoute != null) {
        	respHeaders.add(recordRouteName, reqRecRoute);
        }

        

        SipHeaderFieldName contactName = new SipHeaderFieldName(RFC3261.HDR_CONTACT);
        
        Dialog dialog = dialogManager.createDialog(sipResponse);
        
        //build dialog state
        
          //route set
        SipHeaderFieldValue recordRoute =
            respHeaders.get(new SipHeaderFieldName(RFC3261.HDR_RECORD_ROUTE));
        ArrayList<String> routeSet = new ArrayList<String>();
        if (recordRoute != null) {
            if (recordRoute instanceof SipHeaderFieldMultiValue) {
                SipHeaderFieldMultiValue multiRecordRoute =
                    (SipHeaderFieldMultiValue) recordRoute;
                for (SipHeaderFieldValue routeValue : multiRecordRoute.getValues()) {
                    routeSet.add(routeValue.getValue());
                }
            } else {
                routeSet.add(recordRoute.getValue());
            }
        }
        dialog.setRouteSet(routeSet);
        
          //remote target
        SipHeaderFieldValue reqContact = reqHeaders.get(contactName);
        String remoteTarget = reqContact.getValue();
        if (remoteTarget.indexOf(RFC3261.LEFT_ANGLE_BRACKET) > -1) {
            remoteTarget = NameAddress.nameAddressToUri(remoteTarget);
        }
        dialog.setRemoteTarget(remoteTarget);
        
          //remote cseq
        SipHeaderFieldName cseqName = new SipHeaderFieldName(RFC3261.HDR_CSEQ);
        SipHeaderFieldValue cseq = reqHeaders.get(cseqName);
        String remoteCseq = cseq.getValue().substring(0, cseq.getValue().indexOf(' '));
        dialog.setRemoteCSeq(Integer.parseInt(remoteCseq));
        
          //callid
        SipHeaderFieldName callidName = new SipHeaderFieldName(RFC3261.HDR_CALLID);
        SipHeaderFieldValue callid = reqHeaders.get(callidName);
        dialog.setCallId(callid.getValue());
        
          //local tag
        SipHeaderFieldName toName = new SipHeaderFieldName(RFC3261.HDR_TO);
        SipHeaderFieldValue to = respHeaders.get(toName);
        SipHeaderParamName tagName = new SipHeaderParamName(RFC3261.PARAM_TAG);
        String toTag = to.getParam(tagName);
        dialog.setLocalTag(toTag);
        
          //remote tag
        SipHeaderFieldName fromName = new SipHeaderFieldName(RFC3261.HDR_FROM);
        SipHeaderFieldValue from = reqHeaders.get(fromName);
        String fromTag = from.getParam(tagName);
        dialog.setRemoteTag(fromTag);
        
          //remote uri
        
        String remoteUri = from.getValue();
        if (remoteUri.indexOf(RFC3261.LEFT_ANGLE_BRACKET) > -1) {
            remoteUri = NameAddress.nameAddressToUri(remoteUri);
        }
        dialog.setRemoteUri(remoteUri);
        
          //local uri
        
        String localUri = to.getValue();
        if (localUri.indexOf(RFC3261.LEFT_ANGLE_BRACKET) > -1) {
            localUri = NameAddress.nameAddressToUri(localUri);
        }
        dialog.setLocalUri(localUri);
        
        return dialog;
    }
    
    
    protected Dialog buildOrUpdateDialogForUac(SipResponse sipResponse,
            Transaction transaction) {
        SipHeaders headers = sipResponse.getSipHeaders();
        
        Dialog dialog = dialogManager.getDialog(sipResponse);
        if (dialog == null) {
            dialog = dialogManager.createDialog(sipResponse);
        }
        
        //12.1.2
        
        
        
        //route set
        
        ArrayList<String> routeSet = computeRouteSet(headers);
        if (routeSet != null) {
            dialog.setRouteSet(routeSet);
        }
        
        //remote target
        
        SipHeaderFieldValue contact = headers.get(new SipHeaderFieldName(RFC3261.HDR_CONTACT));
        logger.debug("Contact: " + contact);
        if (contact != null) {
            String remoteTarget = NameAddress.nameAddressToUri(contact.toString());
            dialog.setRemoteTarget(remoteTarget);
        }
        
        SipHeaders requestSipHeaders = transaction.getRequest().getSipHeaders();
        
        //local cseq
        
        String requestCSeq = requestSipHeaders.get(
                new SipHeaderFieldName(RFC3261.HDR_CSEQ)).toString();
        requestCSeq = requestCSeq.substring(0, requestCSeq.indexOf(' '));
        dialog.setLocalCSeq(Integer.parseInt(requestCSeq));
        
        //callID
        
        //already done in createDialog()
//        String requestCallID = requestSipHeaders.get(
//                new SipHeaderFieldName(RFC3261.HDR_CALLID)).toString();
//        dialog.setCallId(requestCallID);
        
        //local tag
        
        //already done in createDialog()
//        SipHeaderFieldValue requestFrom = requestSipHeaders.get(
//                new SipHeaderFieldName(RFC3261.HDR_FROM));
//        String requestFromTag =
//            requestFrom.getParam(new SipHeaderParamName(RFC3261.PARAM_TAG));
//        dialog.setLocalTag(requestFromTag);
        
        //remote tag
        
        //already done in createDialog()
//        dialog.setRemoteTag(toTag);
        
          //remote uri
        
        SipHeaderFieldValue to = headers.get(new SipHeaderFieldName(RFC3261.HDR_TO));
        if (to != null) {
            String remoteUri = to.getValue();
            if (remoteUri.indexOf(RFC3261.LEFT_ANGLE_BRACKET) > -1) {
                remoteUri = NameAddress.nameAddressToUri(remoteUri);
            }
            dialog.setRemoteUri(remoteUri);
        }
        
          //local uri
        SipHeaderFieldValue requestFrom = requestSipHeaders.get(
              new SipHeaderFieldName(RFC3261.HDR_FROM));
        String localUri = requestFrom.getValue();
        if (localUri.indexOf(RFC3261.LEFT_ANGLE_BRACKET) > -1) {
            localUri = NameAddress.nameAddressToUri(localUri);
        }
        dialog.setLocalUri(localUri);
        
        return dialog;
    }

    protected ArrayList<String> computeRouteSet(SipHeaders headers) {
        SipHeaderFieldValue recordRoute =
            headers.get(new SipHeaderFieldName(RFC3261.HDR_RECORD_ROUTE));
        ArrayList<String> routeSet = new ArrayList<String>();
        if (recordRoute != null) {
            if (recordRoute instanceof SipHeaderFieldMultiValue) {
                List<SipHeaderFieldValue> values =
                    ((SipHeaderFieldMultiValue)recordRoute).getValues();
                for (SipHeaderFieldValue value : values) {
                    //reverse order
                    routeSet.add(0, value.toString());
                }
            } else {
                routeSet.add(recordRoute.toString());
            }
        }
        return routeSet;
    }
    
    
    class AckTimerTask extends TimerTask {

        private String toUri;
        
        public AckTimerTask(String toUri) {
            super();
            this.toUri = toUri;
        }

        @Override
        public void run() {
            ArrayList<Dialog> purgedDialogs = new ArrayList<Dialog>();
            Collection<Dialog> dialogs = dialogManager.getDialogCollection();
            for (Dialog dialog : dialogs) {
                String remoteUri = dialog.getRemoteUri();
                if (remoteUri.equals(toUri) &&
                        !dialog.CONFIRMED.equals(dialog.getState())) {
                    dialog.receivedOrSentBye();
                    purgedDialogs.add(dialog);
                }
            }
            for (Dialog dialog : purgedDialogs) {
                dialogs.remove(dialog);
            }
        }
        
    }


}
