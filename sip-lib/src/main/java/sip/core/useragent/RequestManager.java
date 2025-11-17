// Peers SIP Softphone - GPL v3 License

package sip.core.useragent;

import sip.Logger;
import sip.RFC3261;
import sip.core.useragent.handlers.ByeHandler;
import sip.core.useragent.handlers.CancelHandler;
import sip.core.useragent.handlers.InviteHandler;
import sip.core.useragent.handlers.MessageHandler;
import sip.core.useragent.handlers.OptionsHandler;
import sip.core.useragent.handlers.RegisterHandler;
import sip.syntaxencoding.NameAddress;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaderParamName;
import sip.syntaxencoding.SipHeaders;
import sip.syntaxencoding.SipURI;
import sip.syntaxencoding.SipUriSyntaxException;
import sip.transaction.TransactionManager;
import sip.transactionuser.Dialog;
import sip.transactionuser.DialogManager;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.TransportManager;


public abstract class RequestManager {

    public static SipURI getDestinationUri(SipRequest sipRequest,
            Logger logger) {
        SipHeaders requestHeaders = sipRequest.getSipHeaders();
        SipURI destinationUri = null;
        SipHeaderFieldValue route = requestHeaders.get(
                new SipHeaderFieldName(RFC3261.HDR_ROUTE));
        if (route != null) {
            try {
                destinationUri = new SipURI(
                        NameAddress.nameAddressToUri(route.toString()));
            } catch (SipUriSyntaxException e) {
                logger.error("syntax error", e);
            }
        }
        if (destinationUri == null) {
            destinationUri = sipRequest.getRequestUri();
        }
        return destinationUri;
    }

    public static SipResponse generateResponse(SipRequest sipRequest,
            Dialog dialog, int statusCode, String reasonPhrase) {
        //8.2.6.2
        SipResponse sipResponse = new SipResponse(statusCode, reasonPhrase);
        SipHeaders requestHeaders = sipRequest.getSipHeaders();
        SipHeaders responseHeaders = sipResponse.getSipHeaders();
        SipHeaderFieldName fromName = new SipHeaderFieldName(RFC3261.HDR_FROM);
        responseHeaders.add(fromName, requestHeaders.get(fromName));
        SipHeaderFieldName callIdName = new SipHeaderFieldName(RFC3261.HDR_CALLID);
        responseHeaders.add(callIdName, requestHeaders.get(callIdName));
        SipHeaderFieldName cseqName = new SipHeaderFieldName(RFC3261.HDR_CSEQ);
        responseHeaders.add(cseqName, requestHeaders.get(cseqName));
        SipHeaderFieldName viaName = new SipHeaderFieldName(RFC3261.HDR_VIA);
        responseHeaders.add(viaName, requestHeaders.get(viaName));
        SipHeaderFieldName toName = new SipHeaderFieldName(RFC3261.HDR_TO);
        SipHeaderFieldValue toValue = requestHeaders.get(toName);
        SipHeaderParamName toTagParamName = new SipHeaderParamName(RFC3261.PARAM_TAG);
        String toTag = toValue.getParam(toTagParamName);
        if (toTag == null) {
            if (dialog != null) {
                toTag = dialog.getLocalTag();
                toValue.addParam(toTagParamName, toTag);
            }
        }
        responseHeaders.add(toName, toValue);
        return sipResponse;
    }

    protected InviteHandler inviteHandler;
    protected CancelHandler cancelHandler;
    protected ByeHandler byeHandler;
    protected OptionsHandler optionsHandler;
    protected RegisterHandler registerHandler;
    protected MessageHandler messageHandler;
    
    protected UserAgent userAgent;
    protected TransactionManager transactionManager;
    protected TransportManager transportManager;
    protected Logger logger;
    
    public RequestManager(UserAgent userAgent,
            InviteHandler inviteHandler,
            CancelHandler cancelHandler,
            ByeHandler byeHandler,
            OptionsHandler optionsHandler,
            RegisterHandler registerHandler,
            MessageHandler messageHandler,
            DialogManager dialogManager,
            TransactionManager transactionManager,
            TransportManager transportManager,
            Logger logger) {
        this.userAgent = userAgent;
        this.inviteHandler = inviteHandler;
        this.cancelHandler = cancelHandler;
        this.byeHandler = byeHandler;
        this.optionsHandler = optionsHandler;
        this.registerHandler = registerHandler;
        this.messageHandler = messageHandler;
        this.transactionManager = transactionManager;
        this.transportManager = transportManager;
        this.logger = logger;
    }

    public InviteHandler getInviteHandler() {
        return inviteHandler;
    }

    public ByeHandler getByeHandler() {
        return byeHandler;
    }

    public RegisterHandler getRegisterHandler() {
        return registerHandler;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

}
