// Peers SIP Softphone - GPL v3 License

package sip.core.useragent.handlers;

import sip.Logger;
import sip.sdp.SDPManager;
import sip.RFC3261;
import sip.Utils;
import sip.core.useragent.ChallengeManager;
import sip.core.useragent.UserAgent;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaderParamName;
import sip.syntaxencoding.SipHeaders;
import sip.transaction.TransactionManager;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.TransportManager;

public abstract class MethodHandler {

    protected UserAgent userAgent;
    protected TransactionManager transactionManager;
    protected TransportManager transportManager;
    protected ChallengeManager challengeManager;
    protected SDPManager sdpManager;
    protected boolean challenged;
    protected Logger logger;
    
    public MethodHandler(UserAgent userAgent,
            TransactionManager transactionManager,
            TransportManager transportManager, Logger logger) {
        this.userAgent = userAgent;
        this.transactionManager = transactionManager;
        this.transportManager = transportManager;
        this.logger = logger;
        challenged = false;
    }
    
    protected SipResponse buildGenericResponse(SipRequest sipRequest,
            int statusCode, String reasonPhrase) {
        //8.2.6
        SipResponse sipResponse = new SipResponse(statusCode, reasonPhrase);
        SipHeaders respHeaders = sipResponse.getSipHeaders();
        SipHeaders reqHeaders = sipRequest.getSipHeaders();
        SipHeaderFieldName fromName = new SipHeaderFieldName(RFC3261.HDR_FROM);
        respHeaders.add(fromName, reqHeaders.get(fromName));
        SipHeaderFieldName callIdName = new SipHeaderFieldName(RFC3261.HDR_CALLID);
        respHeaders.add(callIdName, reqHeaders.get(callIdName));
        SipHeaderFieldName cseqName = new SipHeaderFieldName(RFC3261.HDR_CSEQ);
        respHeaders.add(cseqName, reqHeaders.get(cseqName));
        SipHeaderFieldName viaName = new SipHeaderFieldName(RFC3261.HDR_VIA);
        respHeaders.add(viaName, reqHeaders.get(viaName));
        SipHeaderFieldName toName = new SipHeaderFieldName(RFC3261.HDR_TO);
        String to = reqHeaders.get(toName).getValue();
        SipHeaderFieldValue toValue = new SipHeaderFieldValue(to);
        toValue.addParam(new SipHeaderParamName(RFC3261.PARAM_TAG),
                Utils.randomString(10));// TODO 19.3
        respHeaders.add(toName, toValue);
        return sipResponse;
    }

    public void setChallengeManager(ChallengeManager challengeManager) {
        this.challengeManager = challengeManager;
    }

    public void setSdpManager(SDPManager sdpManager) {
        this.sdpManager = sdpManager;
    }

}
