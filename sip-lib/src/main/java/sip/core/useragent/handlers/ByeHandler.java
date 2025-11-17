// Peers SIP Softphone - GPL v3 License

package sip.core.useragent.handlers;

import sip.Logger;
import sip.RFC3261;
import sip.core.useragent.RequestManager;
import sip.core.useragent.SipListener;
import sip.core.useragent.UserAgent;
import sip.transaction.ClientTransaction;
import sip.transaction.ClientTransactionUser;
import sip.transaction.NonInviteClientTransaction;
import sip.transaction.ServerTransaction;
import sip.transaction.ServerTransactionUser;
import sip.transaction.Transaction;
import sip.transaction.TransactionManager;
import sip.transactionuser.Dialog;
import sip.transactionuser.DialogManager;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.TransportManager;

public class ByeHandler extends DialogMethodHandler
        implements ServerTransactionUser, ClientTransactionUser {

    public ByeHandler(UserAgent userAgent, DialogManager dialogManager,
            TransactionManager transactionManager,
            TransportManager transportManager, Logger logger) {
        super(userAgent, dialogManager, transactionManager, transportManager,
                logger);
    }

    ////////////////////////////////////////////////
    // methods for UAC
    ////////////////////////////////////////////////
    
    public void preprocessBye(SipRequest sipRequest, Dialog dialog) {

        // 15.1.1
        
        String addrSpec = sipRequest.getRequestUri().toString();
        userAgent.getPeers().remove(addrSpec);
        challengeManager.postProcess(sipRequest);
    }
    
    

    
    
    
    ////////////////////////////////////////////////
    // methods for UAS
    ////////////////////////////////////////////////
    
    public void handleBye(SipRequest sipRequest, Dialog dialog) {
        dialog.receivedOrSentBye();
        //String remoteUri = dialog.getRemoteUri();

        String addrSpec = sipRequest.getRequestUri().toString();
        userAgent.getPeers().remove(addrSpec);
        dialogManager.removeDialog(dialog.getId());
        logger.debug("removed dialog " + dialog.getId());
        userAgent.getMediaManager().stopSession();
        
        SipResponse sipResponse =
            RequestManager.generateResponse(
                    sipRequest,
                    dialog,
                    RFC3261.CODE_200_OK,
                    RFC3261.REASON_200_OK);
        
        // TODO determine port and transport for server transaction>transport
        // from initial invite
        // FIXME determine port and transport for server transaction>transport
        ServerTransaction serverTransaction = transactionManager
            .createServerTransaction(
                    sipResponse,
                    userAgent.getSipPort(),
                    RFC3261.TRANSPORT_UDP,
                    this,
                    sipRequest);
        
        serverTransaction.start();
        
        serverTransaction.receivedRequest(sipRequest);
        
        serverTransaction.sendReponse(sipResponse);
        
        dialogManager.removeDialog(dialog.getId());

        SipListener sipListener = userAgent.getSipListener();
        if (sipListener != null) {
            sipListener.remoteHangup(sipRequest);
        }

//        setChanged();
//        notifyObservers(sipRequest);
    }

    ///////////////////////////////////////
    //ServerTransactionUser methods
    ///////////////////////////////////////
    public void transactionFailure() {
        // TODO Auto-generated method stub
        
    }

    ///////////////////////////////////////
    //ClientTransactionUser methods
    ///////////////////////////////////////
	@Override
	public void transactionTimeout(ClientTransaction clientTransaction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void provResponseReceived(SipResponse sipResponse,
			Transaction transaction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void errResponseReceived(SipResponse sipResponse) {
		int statusCode = sipResponse.getStatusCode();
        if (statusCode == RFC3261.CODE_401_UNAUTHORIZED
                || statusCode == RFC3261.CODE_407_PROXY_AUTHENTICATION_REQUIRED
                && !challenged) {
        	NonInviteClientTransaction nonInviteClientTransaction =
                (NonInviteClientTransaction)
                transactionManager.getClientTransaction(sipResponse);
            SipRequest sipRequest = nonInviteClientTransaction.getRequest();
            String password = userAgent.getConfig().getPassword();
            if (password != null && !"".equals(password.trim())) {
                challengeManager.handleChallenge(sipRequest,
                        sipResponse);
            }
        	challenged = true;
        } else {
        	challenged = false;
        }
	}

	@Override
	public void successResponseReceived(SipResponse sipResponse,
			Transaction transaction) {
		Dialog dialog = dialogManager.getDialog(sipResponse);
		if (dialog == null) {
		    return;
		}
		dialog.receivedOrSentBye();
		dialogManager.removeDialog(dialog.getId());
        logger.debug("removed dialog " + dialog.getId());
	}

	@Override
	public void transactionTransportError() {
		// TODO Auto-generated method stub
		
	}


    
    
}
